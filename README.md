# Tiny Ledger Service by Tiago Castro — Documentation
Welcome to my implementation of the Tiny Ledger take-home assignment!<br><br>
I chose to build this service using Clean Architecture-inspired layering to keep the codebase maintainable, testable, and decoupled while preserving the simplicity expected for this assignment.
For a real production environment, it could also be worth considering an event-driven architecture.<br><br>
Most decisions were made from the standpoint that there is a bounded context around the Transaction domain. It does not include an Account domain, but it retains a reference to one through `accountId`.<br>
This Ledger can therefore store Transactions associated with any `accountId` and return all Transactions stored in the Ledger.<br>
I also included a separate Balance endpoint for retrieving the current ledger balance. Because Balance is derived from Transactions but is not itself a Transaction, I separated its controller and service from the Transaction flow.<br><br>
One other important decision I made was around duplicate-transaction safety. The `transactionId` is generated server-side by this service (`UUID.randomUUID()`) and the client must send an `Idempotency-Key` HTTP header on every create request. The `TransactionService` checks that key, replays the original response on a matching retry (returning `200 OK`), and rejects (with `422`) any retry that reuses the key with a different payload. This is the standard idempotency-key pattern used by payment APIs and keeps retries safe from duplicate writes.

# 1. Architectural Strategy & Directory Structure
To keep a clear separation of concerns, I structured the package layout using Clean Architecture-inspired layers.
The core domain is entirely decoupled from external frameworks, persistence mechanisms, and transport protocols.

## Package Structure
Below is the exact package layout I used for this project:
```text
src/
└── main/
    ├── java/
    │   └── com.teya.ledger/
    │       ├── config/
    │       │   └── LedgerConfiguration.java
    │       ├── controllers/
    │       │   ├── BalancesController.java
    │       │   └── TransactionsController.java
    │       ├── domain/
    │       │   ├── Money.java
    │       │   ├── Transaction.java
    │       │   └── TransactionType.java
    │       ├── dtos/
    │       │   ├── BalanceResponse.java
    │       │   ├── CreateTransactionCommand.java
    │       │   ├── CreateTransactionResult.java
    │       │   ├── ErrorDto.java
    │       │   ├── TransactionRequest.java
    │       │   └── TransactionResponse.java
    │       ├── exceptions/
    │       │   ├── DuplicateTransactionException.java
    │       │   ├── GlobalExceptionHandler.java
    │       │   ├── IdempotencyKeyMismatchException.java
    │       │   ├── InsufficientFundsException.java
    │       │   └── MissingIdempotencyKeyException.java
    │       ├── persistence/
    │       │   ├── datamodels/
    │       │   │   └── TransactionDataModel.java
    │       │   ├── CacheRepository.java
    │       │   ├── IdempotencyRepository.java
    │       │   ├── IdempotentRequest.java
    │       │   ├── InMemoryCacheRepository.java
    │       │   ├── InMemoryIdempotencyRepository.java
    │       │   ├── InMemoryTransactionsRepository.java
    │       │   └── TransactionsRepository.java
    │       ├── services/
    │       │   ├── BalanceService.java
    │       │   └── TransactionService.java
    │       └── LedgerApplication.java
    └── resources/
        └── application.properties
```

# 2. Running the Application
I have included the Maven Wrapper (mvnw) in the repository to ensure you can build, test, and run this Spring Boot application locally with absolute ease and without requiring a pre-installed Maven instance.

## Prerequisites
Java Development Kit (JDK): Version 26.

## Running the Tests
To execute the unit and integration test suite, run the following command in your terminal:
```bash
# On macOS / Linux:
./mvnw clean test

# On Windows (Command Prompt):
mvnw.cmd clean test

# On Windows (PowerShell):
.\mvnw.cmd clean test
```

## Launching the Application
To run the Spring Boot application locally:
```bash
# On macOS / Linux:
./mvnw spring-boot:run

# On Windows (Command Prompt):
mvnw.cmd spring-boot:run

# On Windows (PowerShell):
.\mvnw.cmd spring-boot:run
```

Once started, the server will boot up and listen for incoming HTTP API requests on http://localhost:8080

# 3. Key Decisions Matrix

| Decision                                                 | Impact                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Clean Architecture-inspired layering                     | The core domain remains independent of Spring, persistence, and transport concerns. Application services are wired through Spring but can still be tested in isolation without bootstrapping the application context.                                                                                                                                                                                                                                                                                 |
| In-memory storage                                        | Keeps the solution simple and aligned with the assignment constraints, requiring no external dependencies or local setup. All data is lost on restart and a production implementation would require persistent storage.                                                                                                                                                                                                                                                                               |
| Single ledger, no Account domain or authentication       | I assumed a single shared ledger for this exercise. Authentication and authorization are explicitly out of scope, while `accountId` is retained only as a reference to an Account domain outside this bounded context.                                                                                                                                                                                                                                                                                |
| No pagination                                            | Transaction history is returned as a complete list to keep the API simple. This is acceptable for the small in-memory dataset used in this exercise; a production implementation should introduce pagination as history grows.                                                                                                                                                                                                                                                                        |
| Synchronous requests                                     | Keeping in mind simplicity, so no pending states.                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Separate `/balances` and `/transactions` endpoints       | Applies Separation of Concerns and Interface Segregation principles. Consumers can query the current balance without transferring transaction history, and the read model can later be optimized independently from the write path (CQRS-ready).                                                                                                                                                                                                                                                      |
| Non-negative balance invariant                           | The ledger does not allow negative balances, so this domain constraint is enforced in the `Transaction` constructor.                                                                                                                                                                                                                                                                                                                                                                                  |
| `BigDecimal` for precision                               | Ensures accurate representation of monetary values, preventing floating-point arithmetic issues that could lead to rounding errors in financial calculations.                                                                                                                                                                                                                                                                                                                                         |
| `balanceAfter` stored on each transaction                | Each transaction records the resulting ledger balance at the time it is processed. This is a processing-order snapshot rather than a chronologically recalculated historical balance, because clients may submit backdated timestamps. It provides useful context at the cost of duplicating derived state.                                                                                                                                                                                           |
| Cached running balance                                   | The ledger balance is initialized at startup by replaying stored transactions and then updated incrementally in O(1) after every successful write. This improves balance-read performance at the cost of maintaining derived state.                                                                                                                                                                                                                                                                   |
| Explicit cache initialization                            | A dedicated `CommandLineRunner` initializes the balance cache, keeping startup concerns outside `BalanceService` and making the initialization lifecycle explicit since I decided to depend on that cache to read the balance.                                                                                                                                                                                                                                                                        |
| Eventual consistency between history and balance         | Even though I'm using an `AtomicReference<BigDecimal>` to keep track of the balance, saving a transaction and updating the cached balance are separate operations (not wrapped in the same database transaction) for simplicity. A concurrent reader may briefly observe the new transaction with the previous balance. This temporary inconsistency is an accepted simplification for this assignment. A production implementation would update both within one transaction or consistency boundary. |
| `ConcurrentHashMap` repository and Concurrency Control   | Provides simple thread safety and basic concurrency control with minimal complexity, making the service more robust in multi-threaded scenarios even though concurrency requirements were not explicitly requested. `ConcurrentHashMap.putIfAbsent` provides atomic duplicate protection at the repository level (defense-in-depth, since the service now generates IDs and dedups via the idempotency key), while transaction creation is synchronized to serialize idempotency lookups, balance calculations, and writes. Reads remain concurrent and may observe the documented temporary inconsistency.                                    |
| Transactions sorted by timestamp descending              | Since `ConcurrentHashMap` does not guarantee ordering, transaction history is explicitly sorted so that the newest transactions appear first, matching common banking application behavior.                                                                                                                                                                                                                                                                                                           |
| Idempotency-Key header for deduplication                 | The `transactionId` is generated server-side (`UUID.randomUUID()`) and the client supplies a separate `Idempotency-Key` HTTP header. The service stores the original request and response under that key, so a retry with the same key and matching payload replays the original response with `200 OK` instead of creating a duplicate. A retry that reuses the key with a *different* payload is rejected with `422`, preventing key reuse for different intents. Payload match is checked with a plain `equals()` on the stored `CreateTransactionCommand` (a record, so equality is structural). Keys are retained for the lifetime of the in-memory store. |
| Repository implementation selected through configuration | `TransactionsRepository` is wired through configuration, allowing the application to select among repository implementations already packaged with it. This adds flexibility at the cost of a small amount of configuration complexity.                                                                                                                                                                                                                                                               |
| No currency or exchange rates introduced                 | The `Money` value object encapsulates the amount and ensures type safety, while deliberately omitting currency and exchange-rate concerns. Transaction amounts must be greater than zero.                                                                                                                                                                                                                                                                                                             |

# 4. REST API Documentation
## Endpoints Specifications

| Endpoint                           | Method | Description                                          | Response status codes                                                                                  |
|------------------------------------|--------|------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `/api/v1/transactions`             | `POST` | Create a new transaction (Deposit or Withdrawal).    | 200 OK (idempotent replay)<br>201 Created<br>400 Bad Request<br>422 Unprocessable Entity                             |
| `/api/v1/transactions`             | `GET`  | Retrieve all transactions in the ledger.             | 200 OK                                                                                                 |
| `/api/v1/balances`                 | `GET`  | Retrieve the total balance across the entire ledger. | 200 OK                                                                                                 |

## Request examples:
```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 3f1b2c4d-5e6f-4a0b-8d7e-07e33dc7816f" \
  -d '{
    "accountId": "a50db8df-2872-4d2c-88e9-df7726a4220b",
    "description": "Salary",
    "transactionType": "DEPOSIT",
    "amount": 100000.53,
    "timestamp": "2026-07-16T14:30:00Z"
  }'

## Success response status code: 201 Created
{
  "transactionId": "d027f300-3be2-4a0b-8d7e-07e33dc7816f",
  "accountId": "a50db8df-2872-4d2c-88e9-df7726a4220b",
  "description": "Salary",
  "transactionType": "DEPOSIT",
  "amount": 100000.53,
  "timestamp": "2026-07-16T14:30:00Z",
  "balanceAfter": 100000.53
}

## Idempotent replay status code: 200 OK
## Returned when the same Idempotency-Key is reused with the same payload.
## The original response body is replayed verbatim (same transactionId).

## Error response status code: 400 Bad Request
{
  "message": "Idempotency-Key header is required",
  "timestamp": "2026-07-16T23:28:23.332275600Z"
}

## Error response status code: 422 Unprocessable Entity (Idempotency-Key reused with a different payload)
{
  "message": "Idempotency key was already used for a different request payload",
  "timestamp": "2026-07-16T23:28:23.332275600Z"
}

## Error response status code: 422 Unprocessable Entity
{
  "message": "Insufficient funds for withdrawal",
  "timestamp": "2026-07-16T23:28:23.332275600Z"
}
```

```bash
curl http://localhost:8080/api/v1/transactions

## Success response status code: 200 OK
[
  {
    "transactionId": "d027f300-3be2-4a0b-8d7e-07e33dc7816f",
    "accountId": "a50db8df-2872-4d2c-88e9-df7726a4220b",
    "description": "Salary",
    "transactionType": "DEPOSIT",
    "amount": 100000.53,
    "timestamp": "2026-07-16T14:30:00Z",
    "balanceAfter": 100000.53
  }
]
```

```bash
curl http://localhost:8080/api/v1/balances

## Success response status code: 200 OK
{
  "balance": 100000.53
}
```

# 5. Core Domain Modeling
The domain boundary contains pure Java constructs with no dependencies on web or framework-specific layers. 
It handles essential invariants such as validating balance sufficiency, so the Transaction object in this case can't be created in inconsistent state, going with similar principles of Domain Driven Design.
I would ask a couple of questions to clarify with product, for example, how should be the balance within the ledger and if the balance is not a thing from Account instead.
In this case I decided that the ledger has a balance which can't be negative and I used the `Transaction` domain object to carry a `balanceAfter` variable so when looking at the history of the transactions we get something similar to an audit trail of the balance changes.

## Domain Entities & Value Objects
```plantuml
Transaction {
    UUID transactionId;
    UUID accountId;
    String description;
    Money money;
    TransactionType transactionType;
    Instant timestamp;
    BigDecimal balanceAfter;
}

enum TransactionType {
    DEPOSIT,
    WITHDRAWAL
}

Money {
    BigDecimal amount;
    // currency and other fields can be added here in the future
}
```

## 5.1 Domain Design Rationale
 - The Money Value Object: I purposefully created a dedicated Money value object. While keeping it plain and simple with just a BigDecimal amount right now, wrapping money into its own encapsulation guarantees that if we introduce currency support (e.g., EUR, GBP) or currency conversion logic down the line, we don’t break the external Transaction class.
 - Bounded Context Reference via `accountId`: Instead of linking a direct Account Java object inside the transaction, I chose to maintain only a reference to the `accountId`. This reflects a real-world microservice structure where the Account domain entity likely lives outside the scope of our ledger service.
 - `Idempotency-Key` for deduplication: The `transactionId` is generated server-side (`UUID.randomUUID()`) and a separate `Idempotency-Key` HTTP header is used for deduplication. The first request under a key creates the transaction and stores the original `CreateTransactionCommand` together with the original `TransactionResponse`; a retry with the same key and matching payload replays that response with `200 OK`, while a retry that reuses the key with a *different* payload is rejected with `422 Unprocessable Entity`. Payload equality is a plain `equals()` comparison on the stored command (records give structural equality for free), keeping the implementation simple. Keys are retained for the lifetime of the in-memory store. For a persistent production deployment, the same shape would be backed by a database table with a unique constraint on the key and an optional TTL.

## 5.2 Performance Improvements & Nice-to-Have Additions
 - Calculating the total balance of a ledger by reading and folding over all historical logs is an expensive *O(N)* lookup operation. To improve read execution speeds, I implemented a simple optimization using a `CacheRepository` in-memory. Even though I'm using an `AtomicReference<BigDecimal>` to keep track of the balance, saving a transaction and updating the cached balance are separate operations (not wrapped in the same database transaction) for simplicity. A concurrent reader may briefly observe the new transaction with the previous balance. This temporary inconsistency is an accepted simplification for this assignment. A production implementation would update both within one transaction or consistency boundary.
On application startup, my service scans the existing repository, computes the current running balance, and *caches* this value in memory. Any successful transaction write thereafter updates this cached state incrementally in *O(1)* time. This completely alleviates the performance cost of retrieving the general ledger balance on demand but if the find query takes too long on a real database, then the application startup is slower too.
 - I also decided to go with `ConcurrentMap` data structure since it allows for simple concurrency control in the application even though it wasn't requested.
 - Because `ConcurrentHashMap` does not preserve ordering, `findAllSortedByTimestampDesc()` returns a list sorted by timestamp in descending order so the API presents the latest transactions first, as commonly seen in banking applications.
