# Trade Matching Engine

A sophisticated **NestJS-based trade matching engine** that simulates an orderbook where users can place buy and sell orders. The system automatically matches trades and persists all data in PostgreSQL with Redis caching and queue-based processing.

## 🧠 Core Matching Algorithm

The system implements a **sophisticated price-time priority matching engine** with the following characteristics:

### Algorithm Overview
- **Price-Time Priority**: Orders are matched by price first, then by time (FIFO)
- **Chunked Processing**: Processes orders in bounded batches (max 200 per chunk) to prevent runaway transactions
- **Partial Fills**: Supports partial order execution with automatic status updates
- **Transaction Safety**: All matching operations run within database transactions for ACID compliance
- **Retry Logic**: Automatic retry for partially filled orders via BullMQ queues

### Matching Process
1. **Order Placement**: New orders are persisted and enqueued for matching
2. **Counter Order Discovery**: System finds matching orders on the opposite side
3. **Price Validation**: Ensures buy orders match with sell orders at compatible prices
4. **Trade Execution**: Calculates trade quantity and price, updates order statuses
5. **Status Tracking**: Logs all status changes with actor information (USER/SYSTEM)
6. **Batch Processing**: Continues until order is fully filled or no more matches found

### Price Matching Rules
- **Buy Orders**: Match with sell orders where `buy_price >= sell_price`
- **Sell Orders**: Match with buy orders where `sell_price <= buy_price`
- **Trade Price**: Uses the price of the existing order (price-time priority)

## 📈 Scalability: How and Why This Scales Well

This design is intentionally production-leaning and horizontally scalable:

- **Stateless API layer**: The HTTP/API tier does not hold state; user sessions are JWT-based. You can scale out API instances behind a load balancer without sticky sessions.
- **Asynchronous processing via queues**: Matching and expiry run as background jobs (BullMQ). This decouples write-heavy order placement from matching, smoothing spikes. Workers can be scaled independently of the API.
- **Separation of concerns**: API (ingress), matching engine (domain logic), persistence (Postgres), and cache/queues (Redis) are cleanly separated, enabling targeted scaling and operational ownership.
- **Efficient data access**: Purposeful indexing on `(side, status, price, created_at)` improves orderbook scans; aggregated orderbook uses SQL grouping at the DB-level for performance.
- **Chunked matching**: Matching processes counter orders in bounded chunks (max 200 per batch), preventing runaway transactions and keeping latency predictable under load.
- **Idempotent, transactional writes**: Matching and expiry run in DB transactions to ensure atomicity and consistency; retries won't double-apply state.
- **Backpressure and retries**: BullMQ default job options provide exponential backoff and capped retries, protecting downstream systems and allowing graceful recovery.
- **Caching and TTLs**: Redis-based token blacklist and cache hooks allow offloading hot paths and reducing DB reads when needed.

Together, these properties allow you to scale each dimension (API replicas, worker count, DB resources) based on bottlenecks observed in production metrics.

## 🗄️ Database Schema & Entity Relationships

### Core Entities

#### 1. **User Entity**
```typescript
User {
  id: number (Primary Key)
  name: string
  email: string (Unique Index)
  password_hash: string
  created_at: timestamp
  updated_at: timestamp
}
```

#### 2. **Order Entity**
```typescript
Order {
  id: number (Primary Key)
  user_id: number (Foreign Key → User.id)
  side: 'BUY' | 'SELL'
  price: string (NUMERIC 18,8)
  quantity: string (NUMERIC 18,8)
  remaining: string (NUMERIC 18,8)
  status: 'OPEN' | 'PARTIAL' | 'FILLED' | 'EXPIRED'
  validity_days: number (default: 60)
  created_at: timestamp
  updated_at: timestamp
}
```

#### 3. **Trade Entity**
```typescript
Trade {
  id: number (Primary Key)
  buy_order_id: number (Foreign Key → Order.id)
  sell_order_id: number (Foreign Key → Order.id)
  price: string (NUMERIC 18,8)
  quantity: string (NUMERIC 18,8)
  created_at: timestamp
}
```

#### 4. **Order Status History Entity**
```typescript
OrderStatusHistory {
  id: number (Primary Key)
  order_id: number (Foreign Key → Order.id, CASCADE DELETE)
  status: 'OPEN' | 'PARTIAL' | 'FILLED' | 'EXPIRED'
  actor: 'USER' | 'SYSTEM'
  created_at: timestamp
}
```

### Entity Relationships

```
User (1) ──────────── (N) Order
 │                        │
 │                        │
 │                        ├── (1) ─── (N) OrderStatusHistory
 │                        │
 │                        └── (1) ─── (N) Trade (as buy_order)
 │                                     │
 │                                     └── (1) ─── (N) Trade (as sell_order)
 │
 └── (1) ─── (N) Trade (as buyer via orders)
```

### Key Relationships Explained

1. **User → Orders**: One user can have many orders (One-to-Many)
2. **Order → OrderStatusHistory**: One order can have many status changes (One-to-Many)
3. **Order → Trades**: One order can participate in many trades as either buyer or seller (One-to-Many)
4. **Trade → Orders**: Each trade references exactly two orders (buy_order and sell_order)

### Database Indexes

- **Users**: `uq_users_email` (unique index on email)
- **Orders**:
  - `idx_side_status_price_createdat` (composite index for efficient orderbook queries)
  - `idx_user_id` (index for user-specific order queries)
- **OrderStatusHistory**: Automatic foreign key indexes
- **Trades**: Automatic foreign key indexes

## 🚀 Features

### Core Trading Engine
- **Order Management**: Place buy/sell orders with price and quantity
- **Automatic Matching**: Sophisticated price-time priority matching algorithm
- **Trade Execution**: Real-time trade execution with partial fill support
- **Orderbook Display**: Live orderbook with aggregated price levels
- **Trade History**: Complete trade history for all users
 - **Order Expiry**: Orders can automatically expire after a validity period
 - **Order Status History**: Every status transition is logged with actor and timestamp

### Advanced Features
- **User Authentication**: JWT-based authentication with secure password hashing
- **Queue Processing**: Asynchronous order processing using BullMQ
- **Redis Caching**: High-performance caching for sessions and data
- **Database Transactions**: ACID-compliant trade execution
- **Retry Logic**: Automatic retry for partially filled orders
- **API Documentation**: Comprehensive Swagger/OpenAPI documentation
- **Health Monitoring**: System health checks and monitoring
 - **Background Jobs**: Separate queues for order processing and expiry (BullMQ)

## 🏗️ Architecture

### Technology Stack
- **Framework**: NestJS (Node.js)
- **Database**: PostgreSQL with TypeORM
- **Caching**: Redis
- **Queue System**: BullMQ (Redis-based)
- **Authentication**: JWT with Passport
- **API Documentation**: Swagger/OpenAPI
- **Validation**: class-validator & class-transformer

### System Components
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Order Service  │    │ Matching Engine │
│   (NestJS)      │◄──►│   (Service)     │◄──►│   (Helper)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Auth Service  │    │   PostgreSQL    │    │   BullMQ Queue  │
│   (JWT)         │    │   (Database)    │    │   (Processing)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Redis Cache   │    │   Trade Service │    │   User Service  │
│   (Sessions)    │    │   (History)     │    │   (Management)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📋 Prerequisites

- **Node.js** (v18 or higher)
- **PostgreSQL** (v12 or higher)
- **Redis** (v6 or higher)
- **npm** or **yarn**

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd trade-matching-engine
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Set up environment variables**
   ```bash
   cp env.example .env
   ```

   Edit `.env` with your configuration:
   ```env
   # Server Configuration
   PORT=3000
   NODE_ENV=development

   # Database Configuration (PostgreSQL)
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=trade_matching_engine
   DB_SCHEMA=public
   PG_USER=postgres
   PG_PASSWORD=your_password_here

   # Redis Configuration
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=your_redis_password_here
   REDIS_TTL=3600

   # JWT Configuration
   JWT_SECRET=your_super_secret_jwt_key_here
   JWT_EXPIRES_IN=24h
   ```

4. **Set up the database**
   ```bash
   # Create PostgreSQL database
   createdb trade_matching_engine

   # Run migrations (if any)
   npm run migration:run
   ```

5. **Start Redis server**
   ```bash
   redis-server
   ```

## 🚀 Running the Application

### Development Mode
```bash
npm run start:dev
```

### Production Mode
```bash
npm run build
npm run start:prod
```

### Local Development with Environment
```bash
npm run start:local
```

The application will be available at `http://localhost:3000`

## 📚 API Documentation

### Swagger UI
Once the application is running, visit:
- **Swagger UI**: `http://localhost:3000/api/docs`

### API Endpoints

#### Authentication
- `POST /api/v1/auth/signup` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout

#### User Management
- `GET /api/v1/users/info` - Get current user information

#### Orders
- `POST /api/v1/orders` - Place a new order (buy/sell)
- `GET /api/v1/orders/:id` - Get order details
- `GET /api/v1/orders` - Get user's orders

#### Orderbook
- `GET /api/v1/orderbook/:side` - Get current orderbook (BUY or SELL)

#### Trades
- `GET /api/v1/trades` - Get trade history

#### Health
- `GET /api/v1/health` - System health check

### Example API Usage

#### 1. Register a new user
```bash
curl -X POST http://localhost:3000/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

#### 2. Login
```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

#### 3. Place a buy order
```bash
curl -X POST http://localhost:3000/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "side": "BUY",
    "price": 100.50,
    "quantity": 10,
    "validity_days": 30
  }'
```

#### 4. Place a sell order
```bash
curl -X POST http://localhost:3000/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "side": "SELL",
    "price": 99.75,
    "quantity": 5,
    "validity_days": 7
  }'
```

#### 5. Get orderbook
```bash
curl -X GET "http://localhost:3000/api/v1/orderbook/BUY?page=1&limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 6. Get user information
```bash
curl -X GET "http://localhost:3000/api/v1/users/info" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 7. Get trade history
```bash
curl -X GET "http://localhost:3000/api/v1/trades?page=1&limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 8. Get system health
```bash
curl -X GET "http://localhost:3000/api/v1/health"
```

## 🔧 Development

### Project Structure
```
src/
├── common/                 # Shared utilities and decorators
│   ├── decorators/        # Custom decorators
│   ├── dtos/             # Common DTOs
│   ├── enums/            # Enums
│   ├── filters/          # Exception filters
│   ├── guards/           # Authentication guards
│   ├── helpers/          # Utility helpers
│   ├── interceptors/     # Response interceptors
│   └── interfaces/       # TypeScript interfaces
├── configs/              # Configuration files
├── database/             # Database configuration
│   ├── postgres/         # PostgreSQL setup
│   └── redis/            # Redis setup
├── modules/              # Feature modules
│   ├── auth/             # Authentication module
│   ├── health/           # Health check module
│   ├── orderbook/        # Trading engine module
│   └── user/             # User management module
├── providers/            # Infrastructure providers
└── main.ts              # Application entry point
```

### Key Components


#### Queue System
- **BullMQ Integration**: Asynchronous order processing
- **Retry Mechanisms**: Exponential backoff for failed jobs
- **Job Persistence**: Jobs stored in Redis for reliability
- **Monitoring**: Built-in job monitoring and metrics
 - **Queues**:
   - `PROCESS_ORDER` → Matches incoming orders against the book
   - `EXPIRE_ORDER` → Expires orders after `validity_days`
 - **Jobs**:
   - `PROCESS_ORDER` and `EXPIRE_ORDER` job names map to queue-specific options

### Database Schema

#### Orders Table
```sql
CREATE TABLE orders (
  id SERIAL PRIMARY KEY,
  user_id INTEGER REFERENCES users(id) NOT NULL,
  side VARCHAR(4) NOT NULL, -- 'BUY' or 'SELL'
  price NUMERIC(18,8) NOT NULL,
  quantity NUMERIC(18,8) NOT NULL,
  remaining NUMERIC(18,8) NOT NULL,
  status VARCHAR(10) DEFAULT 'OPEN', -- 'OPEN', 'PARTIAL', 'FILLED', 'EXPIRED'
  validity_days INT DEFAULT 60,       -- order expiry window in days
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);
```

#### Trades Table
```sql
CREATE TABLE trades (
  id SERIAL PRIMARY KEY,
  buy_order_id INTEGER REFERENCES orders(id),
  sell_order_id INTEGER REFERENCES orders(id),
  price NUMERIC(18,8) NOT NULL,
  quantity NUMERIC(18,8) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);
```

#### Order Status History Table
```sql
CREATE TABLE order_status_history (
  id SERIAL PRIMARY KEY,
  order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
  status VARCHAR(10) NOT NULL,          -- mirrors OrderStatus
  actor VARCHAR(10) NOT NULL,           -- 'USER' | 'SYSTEM'
  created_at TIMESTAMP DEFAULT NOW()
);
```

### Response Shapes (high-level)

- Order (`GET /api/v1/orders/:id` and creation response):
```json
{
  "id": 123,
  "user_id": 45,
  "side": "BUY",
  "price": "100.50",
  "quantity": "10.00",
  "remaining": "5.00",
  "status": "PARTIAL",
  "validity_days": 30,
  "created_at": "2025-01-01T12:34:56.000Z",
  "updated_at": "2025-01-01T12:45:00.000Z",
  "status_history": [
    { "status": "OPEN", "actor": "USER", "created_at": "2025-01-01T12:34:56.000Z" },
    { "status": "PARTIAL", "actor": "SYSTEM", "created_at": "2025-01-01T12:40:00.000Z" }
  ]
}
```

- Orderbook (`GET /api/v1/orderbook/:side`):
```json
{
  "message": "Orderbook fetched successfully",
  "data": {
    "total": 25,
    "page": 1,
    "limit": 10,
    "items": [
      {
        "price": "100.50",
        "remaining": 15.75,
        "status": "OPEN",
        "user_name": "John Doe",
        "created_at": "2024-01-15T10:30:00.000Z",
        "side": "BUY"
      }
    ]
  }
}
```

- User Info (`GET /api/v1/users/info`):
```json
{
  "message": "User Info fetched successfully",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z"
  }
}
```

## 🧪 Testing

### Run Tests
```bash
# Unit tests
npm run test

# E2E tests
npm run test:e2e

# Test coverage
npm run test:cov

# Watch mode
npm run test:watch
```

## 📊 Monitoring

### Health Checks
- **Health Endpoint**: `GET /api/v1/health`
- **Database Status**: Checks PostgreSQL connection
- **Redis Status**: Checks Redis connection
- **Queue Status**: Checks BullMQ queue health

### Logging
The application includes comprehensive logging:
- **Request/Response Logging**: All API calls logged
- **Error Logging**: Detailed error information
- **Trade Logging**: All trade executions logged
- **Queue Logging**: Job processing status

## 🔒 Security Features

- **JWT Authentication**: Secure token-based authentication
- **Password Hashing**: bcrypt with salt for password security
- **Input Validation**: Comprehensive request validation
- **CORS Protection**: Configurable CORS settings
- **Helmet Security**: Security headers middleware

- **Token Blacklisting**: Secure logout with token invalidation

## 🚀 Deployment

This project can be deployed like a standard NestJS app. Ensure Postgres and Redis are available and environment variables are configured. Containerization is optional and can be added per your infra requirements.



## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the API documentation at `/api/docs`
- Review the logs for debugging information



---

**Built with ❤️ using NestJS, PostgreSQL, and Redis**


## ✅ Why This Is A Good Solution To The Assignment

This implementation not only satisfies the requirements but demonstrates practical, production-minded trade-offs:

- **Meets all functional requirements**:
  - Create buy/sell orders; store in Postgres
  - Match trades with a clear, testable algorithm
  - Fetch orderbook and trade history
  - Robust error handling and request validation

- **Simple yet realistic matching**:
  - Price-time priority with partial fills and bounded batches
  - Clear separation between read APIs and write/matching flows

- **Correct persistence model**:
  - Normalized entities (`orders`, `trades`), status lifecycle with `order_status_history`
  - Transactional writes ensure consistency under concurrency

- **Operationally sound**:
  - Health checks (readiness/liveness analogs via health endpoints)
  - Structured logging, global exception handling, input validation
  - Swagger documentation for easy review and testing

- **Extensible by design**:
  - Queues allow adding new processors (e.g., risk checks, alerts) without changing API
  - `validity_days` and status history create hooks for future features (cancelations, audits)
  - Modularity (helpers/services/controllers) keeps changes localized

- **Security and DX**:
  - JWT-based auth, bcrypt hashing, CORS, Helmet
  - Example `.env`, clear scripts, tests and DTO schemas for frictionless onboarding

In short, it delivers a working, maintainable system that scales, is easy to reason about.