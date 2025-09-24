# Trade Matching Engine

A sophisticated **NestJS-based trade matching engine** that simulates an orderbook where users can place buy and sell orders. The system automatically matches trades and persists all data in PostgreSQL with Redis caching and queue-based processing.

## 🚀 Features

### Core Trading Engine
- **Order Management**: Place buy/sell orders with price and quantity
- **Automatic Matching**: Sophisticated price-time priority matching algorithm
- **Trade Execution**: Real-time trade execution with partial fill support
- **Orderbook Display**: Live orderbook with aggregated price levels
- **Trade History**: Complete trade history for all users

### Advanced Features
- **User Authentication**: JWT-based authentication with secure password hashing
- **Queue Processing**: Asynchronous order processing using BullMQ
- **Redis Caching**: High-performance caching for sessions and data
- **Database Transactions**: ACID-compliant trade execution
- **Retry Logic**: Automatic retry for partially filled orders
- **API Documentation**: Comprehensive Swagger/OpenAPI documentation
- **Health Monitoring**: System health checks and monitoring

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

#### Orders
- `POST /api/v1/orders` - Place a new order (buy/sell)
- `GET /api/v1/orders/:id` - Get order details
- `GET /api/v1/orders/book` - Get current orderbook
- `GET /api/v1/orders` - Get user's orders

#### Trades
- `GET /api/v1/trades` - Get trade history

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
    "quantity": 10
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
    "quantity": 5
  }'
```

#### 5. Get orderbook
```bash
curl -X GET "http://localhost:3000/api/v1/orders/book?page=1&limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
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

#### Order Matching Algorithm
The system implements a sophisticated matching engine with:
- **Price-Time Priority**: Orders matched by price, then by time
- **Chunked Processing**: Processes orders in batches (max 200 per chunk)
- **Partial Fills**: Supports partial order execution
- **Transaction Safety**: Database transactions ensure data consistency
- **Retry Logic**: Automatic retry for partially filled orders

#### Queue System
- **BullMQ Integration**: Asynchronous order processing
- **Retry Mechanisms**: Exponential backoff for failed jobs
- **Job Persistence**: Jobs stored in Redis for reliability
- **Monitoring**: Built-in job monitoring and metrics

### Database Schema

#### Orders Table
```sql
CREATE TABLE orders (
  id SERIAL PRIMARY KEY,
  side VARCHAR(4) NOT NULL, -- 'BUY' or 'SELL'
  price NUMERIC(18,8) NOT NULL,
  quantity NUMERIC(18,8) NOT NULL,
  remaining NUMERIC(18,8) NOT NULL,
  status VARCHAR(10) DEFAULT 'OPEN', -- 'OPEN', 'PARTIAL', 'FILLED'
  user_id INTEGER REFERENCES users(id),
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
- **Rate Limiting**: Built-in rate limiting (configurable)
- **Token Blacklisting**: Secure logout with token invalidation

## 🚀 Deployment

### Docker Deployment
```bash
# Build the application
npm run build

# Run with Docker Compose
docker-compose up -d
```

### Environment Variables for Production
```env
NODE_ENV=production
PORT=3000
DB_HOST=your_production_db_host
REDIS_HOST=your_production_redis_host
JWT_SECRET=your_production_jwt_secret
```



## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the API documentation at `/api/docs`
- Review the logs for debugging information



---

**Built with ❤️ using NestJS, PostgreSQL, and Redis**