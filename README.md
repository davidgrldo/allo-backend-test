# Allo Bank Backend Take-Home Test

Thank you for applying to our team! This take-home test is designed to evaluate your practical skills in building **production-ready** Spring Boot applications within a finance domain, focusing on architectural patterns and complex data handling.

## Objective

Your task is to create a single Spring Boot REST API endpoint capable of aggregating data from multiple, distinct resources provided by the public, keyless **Frankfurter Exchange Rate API**. The primary focus is on handling Indonesian Rupiah (IDR) data.

The focus of this test is not just functional correctness, but demonstrating clean code, advanced Spring concepts, thread-safe design, and architectural clarity.

## I. Core Task: The Polymorphic API

1. External API Integration (Frankfurter API)

    * Base URL (Real & Public): https://api.frankfurter.app/ (This is a well-established, keyless API).
    * You must integrate with three distinct data resources to enforce the architectural pattern:

        1. ```/latest?base=IDR``` (The latest rates relative to IDR)
        2. ```/2024-01-01..2024-01-05?from=IDR&to=USD``` (A small time series of historical data)
        3. ```/currencies``` (The list of all supported currency symbols)

2. Internal API Endpoint

You must expose one single endpoint in your application:

```GET /api/finance/data/{resourceType}```


Where `{resourceType}` can be one of the three strings: `latest_idr_rates`, `historical_idr_usd`, or `supported_currencies`.

3. Required Functionality & Business Logic

    * Resource Handling: Your service must correctly map the three incoming `resourceType` values to the correct data fetching strategies.

    * Data Load: All three resources should be fetched from the external API (historical data requires specifying a date range).

    * Data Transformation (Latest IDR Rates only): For the `latest_idr_rates` resource, you must calculate and include a new field, `"USD_BuySpread_IDR"`. This is the Rupiah selling rate to USD after applying a 0.5% banking spread/margin.

   Formula: `USD_BuySpread_IDR = (1 / Rate_USD) * 1.005` (where `Rate_USD` is the value from the API when `base=IDR`).

    * Other Resources: The `historical_idr_usd` and `supported_currencies` resources can return their data with minimal transformation, but the final output must be a unified JSON array of results.

## Constraints

Meeting the core task is only one part of the solution. The following constraints must be strictly adhered to and will be heavily weighted during evaluation:

### Constraint A: The Strategy Pattern

The logic for handling the three different resources (`latest_idr_rates`, `historical_idr_usd`, `supported_currencies`) must be implemented using the **Strategy Design Pattern**.

1. Define a clear **Strategy Interface** (e.g., `IDRDataFetcher`).

2. Implement **three concrete strategy classes** (one for each resource).

3. The main `Controller` should dynamically select the correct strategy implementation using a map-based lookup injected by Spring, avoiding any manual `if/else` or `switch` logic in the controller layer.

### Constraint B: Client Factory Bean

The instance of your chosen external API client (`WebClient` or `RestTemplate`) **must be defined and created within a custom implementation of Spring's `FactoryBean<T>` interface**.

* This FactoryBean should be responsible for externalizing the API Base URL via @Value or @ConfigurationProperties and applying any initial configuration (e.g., timeouts, shared headers).

* **You may not define the client as a simple `@Bean` in a `@Configuration` class**.

### Constraint C: Startup Data Runner & Immutability

The aggregated data for **ALL three resources** must be fetched **exactly once on application startup** and loaded into an in-memory store.

1. Use a Spring Boot `ApplicationRunner` or `CommandLineRunner` component to initiate the data fetching process.

2. The API endpoint (`GET /api/finance/data/{resourceType}`) must serve the data from this **in-memory store**, not by making a new call to the external API on every request.

3. The in-memory storage mechanism (e.g., a service holding the data) must be designed to be **thread-safe** and ensure the data is **immutable** once the `ApplicationRunner` has finished loading it.

## III. Production Readiness & Deliverables

Your final solution must demonstrate production quality through code, testing, and communication.

### 1. Robustness & Best Practices

* Graceful **Error Handling** for network failures or 4xx/5xx responses from the external API.

* Proper use of **Configuration Properties** (e.g., `application.yml`) for external service URLs.

* Clear separation of concerns (Controller, Service, Model/DTO, etc.).

### 2. Testing

* **Unit Tests** for all three `IDRDataFetcher` strategy implementations, ensuring data calculation and transformation logic is covered (using mock clients for external calls).

* **Integration Tests** to verify the `ApplicationRunner` successfully initializes and loads the data into the in-memory store before the application context is ready.

### 3. Documentation

A clear `README.md` is mandatory. It must include:

* Setup/Run Instructions: Clear steps to clone, build, and run the application and tests.

* Endpoint Usage: Example cURL commands to test the three different resource types.

### Architectural Rationale (PR Description)

This section should contain a brief, but detailed, explanation answering the following questions:

1. Polymorphism Justification: Explain why the Strategy Pattern was used over a simpler conditional block in the service layer for handling the multi-resource endpoint. Discuss the benefits in terms of extensibility and maintainability.

2. Client Factory: Explain the specific role and benefit of using a FactoryBean to construct the external API client. Why is this preferable to defining the client using a standard @Bean method in this scenario?

3. Startup Runner Choice: Justify the choice of using an ApplicationRunner (or CommandLineRunner) for the initial data ingestion over a simpler @PostConstruct method.

## IV. Submission

1. Fork this repository.

2. Implement your solution on a dedicated feature branch (e.g., feat/idr-rate-aggregator).

3. When complete, submit your solution via a **Pull Request (PR)** back to the main repository.

Your PR will be evaluated on the following:

* Commit History: Clean, atomic, and descriptive commit messages (e.g., "feat: Implement IDR latest rates strategy," "fix: Correctly calculate IDR spread in tests").

* PR Description: The description must clearly summarize the solution and **must contain the full answers** to the three "Architectural Rationale" questions from Section III.

* Code Review Readiness: The code should be well-structured and ready for immediate review.

Good luck!

---

## Implementation: IDR Rate Aggregator

### Setup & Run

```bash
# Clone and build
git clone <repo-url> && cd allo-backend-test
./mvnw clean package -DskipTests
java -jar target/allo-backend-test-0.0.1-SNAPSHOT.jar

# Run tests
./mvnw test
```

### Endpoint Usage

```bash
# Latest IDR rates with computed USD_BuySpread_IDR
curl http://localhost:8080/api/finance/data/latest_idr_rates

# Historical IDR-USD time series
curl http://localhost:8080/api/finance/data/historical_idr_usd

# Supported currencies
curl http://localhost:8080/api/finance/data/supported_currencies

# Unknown resource type → HTTP 400
curl -i http://localhost:8080/api/finance/data/unknown_type
```

### Architectural Rationale

**1. Strategy Pattern (Polymorphism Justification)**

The Strategy Pattern with map-based dispatch was chosen over conditional blocks because:

- **Open/Closed Principle**: Adding a new resource type requires only a new strategy class annotated with `@Component("new_resource")`. The controller, runner, and dispatch logic need zero changes.
- **Single Responsibility**: Each strategy encapsulates its own fetch URL, transformation logic, and error handling. A `switch` block would centralize 3+ distinct concerns in one class.
- **Testability**: Each strategy can be unit-tested in isolation with mocked dependencies. Conditional dispatch would require testing the controller with all code paths.
- **Spring Integration**: `Map<String, IDRDataFetcher>` auto-injection provides dispatch by bean name without any explicit wiring — the framework does the lookup.

**2. Client FactoryBean**

A custom `FactoryBean<RestTemplate>` was used over a plain `@Bean` method because:

- **Lifecycle Control**: `FactoryBean` gives explicit control over object creation (`getObject()`), type metadata (`getObjectType()`), and singleton/prototype scope — all in one place.
- **Centralized Configuration**: The factory encapsulates base URL injection, connection timeout (5s), and read timeout (10s) in a single class. A plain `@Bean` would scatter this concern across the config class.
- **Constraint Compliance**: The spec explicitly requires the HTTP client to be created via `FactoryBean<T>`, not a plain `@Bean`.

**3. ApplicationRunner**

`ApplicationRunner` was chosen over `@PostConstruct` because:

- **Initialization Order**: `ApplicationRunner.run()` executes after the full application context is ready (all beans instantiated, all dependencies wired). `@PostConstruct` fires during bean initialization when other beans may not be fully available.
- **ApplicationArguments Access**: `ApplicationRunner` provides access to command-line arguments, enabling future configurability (e.g., skip-load flags).
- **Failure Isolation**: The runner wraps each resource fetch in per-resource try/catch, logging warnings without preventing application startup. A failed `@PostConstruct` can block or roll back the entire context.
- **Spec Compliance**: The spec explicitly requires `ApplicationRunner` or `CommandLineRunner`, not `@PostConstruct`.