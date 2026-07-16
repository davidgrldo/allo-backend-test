# Split Bill API

A Spring Boot 3.x REST API for group expense splitting and settlement. Built for the Allo Bank Backend Developer Take-Home Test.

## Build & Run

### Prerequisites
- Java 21+
- Maven (or use the included `./mvnw` wrapper)
- Docker (for PostgreSQL + containerized build)

### Build
```bash
./mvnw clean package
```

### Run with Docker
```bash
docker build -t splitbill .
docker run -p 4110:4110 splitbill
```

### Run locally (requires PostgreSQL)
```bash
docker run --name splitbill-db -e POSTGRES_DB=splitbill -e POSTGRES_USER=splitbill -e POSTGRES_PASSWORD=splitbill -p 5432:5432 -d postgres:16-alpine
./mvnw spring-boot:run
```

The API runs on **port 4110**.

## API Endpoints

### Participants
```bash
# Create participant
curl -X POST http://localhost:4110/api/v1/participants \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice"}'

# List participants
curl http://localhost:4110/api/v1/participants
```

### Groups
```bash
# Create group with participants
curl -X POST http://localhost:4110/api/v1/groups \
  -H "Content-Type: application/json" \
  -d '{"name": "Trip to Bali", "participantIds": [1, 2, 3]}'

# List groups
curl http://localhost:4110/api/v1/groups

# Get group by ID
curl http://localhost:4110/api/v1/groups/1
```

### Expenses
```bash
# Add expense (equal split)
curl -X POST http://localhost:4110/api/v1/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{"description": "Dinner", "payerId": 1, "amount": 90.00, "category": "FOOD", "splitStrategy": "EQUAL", "splits": [{"participantId": 1}, {"participantId": 2}, {"participantId": 3}]}'

# Add expense (exact split)
curl -X POST http://localhost:4110/api/v1/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{"description": "Hotel", "payerId": 2, "amount": 300.00, "category": "ACCOMMODATION", "splitStrategy": "EXACT", "splits": [{"participantId": 1, "amount": 100.00}, {"participantId": 2, "amount": 100.00}, {"participantId": 3, "amount": 100.00}]}'

# List expenses for a group
curl http://localhost:4110/api/v1/groups/1/expenses
```

### Payments
```bash
# Record a payment
curl -X POST http://localhost:4110/api/v1/groups/1/payments \
  -H "Content-Type: application/json" \
  -d '{"payerId": 3, "payeeId": 1, "amount": 30.00}'
```

### Settlement
```bash
# Get settlement summary
curl http://localhost:4110/api/v1/groups/1/settlement
```

Response includes:
- Net balances per participant
- Minimized settlement transactions (who pays whom)
- Service charge (personalized)
- Per-category expense summaries

## Personalization

- **GitHub username**: `davidgrldo`
- **Unicode sum**: 100+97+118+105+100+103+114+108+100+111 = 1056
- **service_charge_pct**: 1056 % 10 = **6%**
- **service_charge_amount**: 6% of total group expenses

## Design Answer

I chose a layered architecture with Spring Data JPA entities mapped to PostgreSQL, service classes holding the business logic, and thin REST controllers. The settlement engine uses a greedy creditor/debtor netting algorithm with sorted PriorityQueue-style pairing to minimize the number of settle-up transactions — the core differentiator from a naive "everyone pays everyone" approach. All monetary values use `BigDecimal` with `NUMERIC(19,4)` database columns to guarantee precision and prevent floating-point drift. `@Transactional` annotations on service methods ensure Hibernate lazy collections are safely hydrated within the transaction boundary, while `open-in-view: false` keeps the persistence layer properly encapsulated. The Dockerfile uses a multi-stage build (`eclipse-temurin:21-jdk-alpine` → `eclipse-temurin:21-jre-alpine`) to produce a minimal runtime image.
