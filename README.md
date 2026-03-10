# Trade Matching Engine (Java)

Spring Boot (Java 21) backend for a **price-time priority** order matching engine. Uses an **in-memory order book** for fast matching, **Kafka** for process-order and expire-order flows, and optional **batched DB writes** to control throughput.

---

## Tech stack

| Layer        | Technology                    |
|-------------|-------------------------------|
| Runtime     | Java 21                       |
| Framework   | Spring Boot 3.4               |
| Data        | Spring Data JPA, PostgreSQL   |
| Cache       | Redis                         |
| Messaging   | Apache Kafka                  |
| Security    | Spring Security, JWT (jjwt)   |
| API docs    | Springdoc OpenAPI (Swagger)   |

---

## Features

- **Auth:** Signup, login (JWT), logout (token blacklist in Redis). Request-scoped **AuthContext** for current user (no `@AuthenticationPrincipal` in every layer).
- **Orders:** Place buy/sell orders; matching runs in memory against the order book (no DB pagination in the hot path).
- **Orderbook:** Get active orders by side with pagination (reads from DB).
- **Trades:** Trade history for the authenticated user.
- **Audit:** Optional `created_by` / `updated_by` (signed-in user email) on entities that extend `BaseWithCreatedBy` / `BaseWithUpdatedBy`; filled by Spring Data JPA auditing from AuthContext.
- **Health:** Actuator health endpoint.
- **Kafka:** `process-order` and `expire-order` topics; matching and expiry consumers; hourly scheduler to enqueue expiry events.

---

## Architecture

### In-memory order book

Matching is done against an **in-memory order book**, not the database, to avoid repeated queries and pagination.

- **Structure:** Two sides, each implemented as `TreeMap<BigDecimal, Deque<Order>>`:
  - **BUY:** Descending price so the best bid is the highest price (first key).
  - **SELL:** Ascending price so the best ask is the lowest price (first key).
  - At each price level, orders are FIFO (`Deque`).
- **Startup:** All OPEN/PARTIAL orders with `remaining > 0` are loaded from the DB into the book once at startup (`OrderBookConfig.loadBookOnStartup()`).
- **After matching:** The book is updated in memory (add/remove orders). The DB is updated by the **write buffer** (see below). There is no periodic “sync” from DB back into the book; the book is the source of truth for matching, and the DB is the source of truth for **what we show to users** (APIs read from the DB).

### Matching engine

- **`MatchingEngine.matchOrder(Order incoming)`:** Takes one incoming order and matches it against the **counter side** of the book until it is filled or there is no more matching liquidity. For each match it: pops the best counter order, computes trade size, updates both orders’ `remaining` and `status`, records the trade and status history, and puts partially filled counter orders back at the front of their price level (time priority).
- **`MatchResult`:** Holds the updated incoming order, all modified orders, trades, and status logs. The write buffer persists these.

### Kafka flow

1. **Place order:** API saves the order to the DB, writes initial status history, and sends two Kafka messages: `process-order` (orderId) and `expire-order` (orderId).
2. **Process order:** `OrderMatchingConsumer` receives `process-order`, calls `OrderMatchingService.matchOrder(orderId)`. The service gets the order (from the book if it was re-enqueued, else from DB), runs the matching engine inside `synchronized (orderBook)`, enqueues the result to the write buffer (and flushes immediately if batching is off), adds the incoming order back to the book if still partial, and returns. If the order is still partial, the consumer re-sends `process-order` (with a reprocess count; max 10).
3. **Expire order:** `OrderExpiryConsumer` receives `expire-order` (after a delay), loads the order from DB, sets status to EXPIRED, and saves. Optionally the book should be updated (e.g. `orderBook.remove(orderId)`) so the in-memory book does not keep expired orders; currently that removal is not wired.

### DB write throughput (batching)

To avoid overloading the DB when match rate is high, persistence can be **batched**:

- **Write buffer:** `MatchResultWriteBuffer` holds a queue of `MatchResult`s. Each match enqueues one result.
- **When batching is disabled** (default): After each match, the service calls `writeBuffer.flush()` so every match is persisted immediately (one transaction per match).
- **When batching is enabled** (`app.matching.persistence.batching-enabled=true`): The service only enqueues; a **scheduler** runs every `app.matching.persistence.flush-interval-ms` and calls `flush()`. The flush drains up to `app.matching.persistence.batch-size` results, **merges orders by id** (last update wins), and persists orders, trades, and status history in **one transaction**. So DB write rate is capped by the flush interval and batch size.

Trade-off: with batching on, order/trade visibility in the DB (and thus in APIs) can lag by up to one flush interval.

### Concurrency

- Only **one thread** at a time runs the matching engine and updates the order book (`synchronized (orderBook)` in `OrderMatchingService.matchOrder`). This avoids double-spend and keeps the book consistent.
- Kafka consumer threads can still run; they serialize on the book when calling `matchOrder`. No multithreading *inside* a single order’s match (that remains sequential).

---

## Order matching flow (step by step)

1. **New order:** User places order via `POST /orders` → order saved to DB → `process-order` and `expire-order` sent to Kafka.
2. **Consumer:** `OrderMatchingConsumer` receives `process-order` with `orderId`.
3. **Load order:** `OrderMatchingService.matchOrder(orderId)` tries `orderBook.getAndRemove(orderId)` (for re-process); if null, loads order from DB.
4. **Match:** Inside `synchronized (orderBook)`, `MatchingEngine.matchOrder(order)` runs: poll best counter order from the book, check price compatibility, compute trade size, update both orders, record trade and status logs, put back partial counter order; repeat until incoming is filled or no more liquidity.
5. **Persist:** Match result is enqueued to `MatchResultWriteBuffer`. If batching is off, `flush()` runs now (one transaction). If batching is on, the scheduler flushes later (batched transaction).
6. **Re-queue:** If the incoming order is still partial, it is added back to the book and the consumer sends `process-order` again (with incremented reprocess count).
7. **User visibility:** All “my order” and “my trades” APIs read from the **database**. So the user sees the latest state after the next flush (immediate when batching is off, or within one flush interval when batching is on).

---

## Configuration

### Application properties

Copy `src/main/resources/application-properties.example` to `application-local.properties` (or your profile) and set:

| Property | Description |
|----------|-------------|
| `server.port` | HTTP port (default 3000). |
| `server.servlet.context-path` | API prefix (e.g. `/api/v1`). |
| `spring.datasource.*` | PostgreSQL URL, username, password. |
| `spring.data.redis.*` | Redis host, port, password. |
| `spring.kafka.bootstrap-servers` | Kafka brokers. |
| `jwt.secret` | Secret for signing JWTs. |
| `jwt.expiration-ms` | Token TTL in milliseconds. |
| `app.service-name` | Service name (e.g. for cache keys). |
| `app.env` | Environment (e.g. development). |
| `app.redis.ttl` | Default Redis TTL in seconds. |

### Matching persistence (throughput control)

| Property | Default | Description |
|----------|---------|-------------|
| `app.matching.persistence.batching-enabled` | `false` | If `true`, match results are buffered and flushed on a schedule instead of after every match. |
| `app.matching.persistence.flush-interval-ms` | `200` | Interval (ms) between flushes when batching is enabled. |
| `app.matching.persistence.batch-size` | `100` | Max number of match results to merge and persist in one transaction per flush. |

Example (enable batching, flush every 200 ms, up to 100 results per flush):

```properties
app.matching.persistence.batching-enabled=true
app.matching.persistence.flush-interval-ms=200
app.matching.persistence.batch-size=100
```

---

## Kafka topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `process-order` | After placing an order; after partial fill (re-enqueue) | `OrderMatchingConsumer` | Trigger matching for an order. |
| `expire-order` | After placing an order (delayed send) | `OrderExpiryConsumer` | Mark order as EXPIRED when TTL is reached. |

Payload for both: `{ "orderId": <long>, "reprocessCount": <int> }` (reprocessCount only for process-order).

---

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 12+
- Redis 6+
- Kafka 3+

---

## Quick start

1. **Clone and build**
   ```bash
   cd trade-matching-engine-java
   mvn clean install
   ```

2. **Configure**
   ```bash
   cp src/main/resources/application-properties.example src/main/resources/application-local.properties
   # Edit with your DB, Redis, Kafka, JWT values
   ```

3. **Run**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   API base: `http://localhost:3000/api/v1` (or your configured port).

4. **API docs**
   - Swagger UI: `http://localhost:3000/api/v1/swagger-ui.html`
   - OpenAPI JSON: `http://localhost:3000/api/v1/v3/api-docs`

---

## API overview

| Method | Path                 | Description        |
|--------|----------------------|--------------------|
| POST   | /auth/signup         | Register           |
| POST   | /auth/login          | Login (JWT)        |
| POST   | /auth/logout         | Logout (blacklist) |
| GET    | /users/info          | Current user       |
| POST   | /orders              | Place order        |
| GET    | /orders/:id          | Order by ID        |
| GET    | /orders              | My orders (paged)  |
| GET    | /orderbook/:side     | Orderbook (BUY/SELL) |
| GET    | /trades              | My trades (paged)  |
| GET    | /health              | Health check       |

All order/trade data returned by the API is read from the **database** (single source of truth for user-facing state).

---

## Project layout

Base package: **`com.shahbazsideprojects.tradematching`**

| Package / path | Description |
|----------------|-------------|
| **engine** | In-memory order book (`OrderBook`), matching engine (`MatchingEngine`), match result DTO (`MatchResult`), write buffer (`MatchResultWriteBuffer`), flush scheduler (when batching enabled), and config that loads the book at startup (`OrderBookConfig`). |
| **advice** | Global response wrapper, exception handler. |
| **config** | Security, JPA (incl. auditing), Redis, Kafka, OpenAPI. All config via `@ConfigurationProperties` (e.g. `AppProperties`, `JwtProperties`, `KafkaProperties`, `DataSourceProperties`). |
| **controller** | REST controllers. |
| **dto** | Request/response DTOs, pagination. |
| **entity** | JPA entities and enums. Base hierarchy: `BaseEntity` (id, createdAt, updatedAt) → `BaseWithCreatedBy` (createdBy) → `BaseWithUpdatedBy` (updatedBy). Only entities that need audit extend the latter (e.g. `Order` extends `BaseWithUpdatedBy`). |
| **exception** | Domain exceptions. |
| **kafka** | Producer, payloads, consumers (matching, expiry). |
| **repository** | Spring Data JPA repositories. |
| **scheduler** | Order expiry scheduler (enqueues expire events). |
| **security** | JWT filter, util, `UserPrincipal` (record: id, email, name), request-scoped **AuthContext**. |
| **service** | Auth, User, Order, Trade, **OrderMatching** (uses engine + book + write buffer), Cache. |
| **util** | Cache key builder. |

---

## Sync and visibility

- **Book ↔ DB:** The in-memory book is loaded once at startup. After each match we persist via the write buffer (immediately or batched). We do **not** periodically reload the book from the DB. So the book is authoritative for matching; the DB is authoritative for what the API returns.
- **User “my order” state:** Always from the DB (`GET /orders/:id`, `GET /orders`). After a match is flushed, the next request sees updated remaining/status and new trades.

---

## Scaling and large order counts

- **Memory:** The book holds every OPEN/PARTIAL order. For very large numbers of orders, consider partitioning (e.g. one book per symbol) or lazy/partial loading at startup.
- **Throughput:** Use **batched persistence** (`app.matching.persistence.batching-enabled=true`) to limit DB write rate and smooth load.
- **Horizontal scaling:** With a single shared book, only one writer (one instance or one consumer group) should run matching. For multiple symbols, one book per symbol per instance avoids shared state and allows scaling out.

---

## License

UNLICENSED.
