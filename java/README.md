# Java solution

Java 17+, Spring Boot, PostgreSQL, Flyway and JDBC. There are exactly two application API routes:

| Method | Path | Result |
| --- | --- | --- |
| GET | `/time-deposits` | `200` with deposits and withdrawal history, or `[]` |
| POST | `/time-deposits/update-balances` | `204`, after committing one monthly update for all deposits |

## Prerequisites

- A JDK, version 17 or newer; set `JAVA_HOME` to its installation directory.
- Docker with Linux containers and Docker Compose v2, with the engine running.
- Internet access on the first run to download Maven, dependencies and container images.
- Free local ports 5433 (PostgreSQL), 8080 (API) and 8081 (Swagger UI).

Maven is supplied through the official Maven Wrapper; a separate Maven installation is unnecessary.
Run the following commands from this repository's `java` directory.

## Build and test

PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Linux/macOS:

```sh
./mvnw clean verify
```

This runs calculator tests and real HTTP/PostgreSQL integration tests. Testcontainers creates its
own temporary database; it does not use the Compose database or its data. Docker being unavailable
is a test failure, not a reason to silently skip tests. Coverage is in `target/site/jacoco/index.html`.
The rollback test deliberately causes an HTTP 500 and a server error log; the test succeeds only
when neither balance has been persisted.

## Start the database, Swagger UI and application

Start the infrastructure:

```sh
docker compose up -d --wait
```

Then run the built application in the foreground, leaving this terminal open.

PowerShell:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target/time-deposit-kata-1.0-SNAPSHOT.jar
```

Linux/macOS:

```sh
"$JAVA_HOME/bin/java" -jar target/time-deposit-kata-1.0-SNAPSHOT.jar
```

Wait for the `Started TimeDepositApplication` log entry. Flyway creates the tables automatically.
The database initially contains no deposits. In another terminal, still in `java`, optionally load
the local demo data after the migration has completed:

PowerShell:

```powershell
Get-Content demo.sql | docker compose exec -T db psql -v ON_ERROR_STOP=1 -U kata -d kata
```

Linux/macOS:

```sh
docker compose exec -T db psql -v ON_ERROR_STOP=1 -U kata -d kata < demo.sql
```

The demo includes all three plans, two ineligible deposits and one historical withdrawal.
It is not loaded automatically and does not reset existing balances if executed again.

## Call the API

PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/time-deposits | ConvertTo-Json -Depth 5
(Invoke-WebRequest -Method Post http://localhost:8080/time-deposits/update-balances).StatusCode
Invoke-RestMethod http://localhost:8080/time-deposits | ConvertTo-Json -Depth 5
```

Linux/macOS:

```sh
curl -i http://localhost:8080/time-deposits
curl -i -X POST http://localhost:8080/time-deposits/update-balances
curl -i http://localhost:8080/time-deposits
```

With fresh demo data, balances start at 1200 each. After exactly one POST, deposits 1-5 have balances
`1201`, `1203`, `1205`, `1200`, `1200`. Days and withdrawal history remain unchanged.
POST takes no body and returns no body. Every additional POST accrues another month of interest.

### Use the OpenAPI contract through Swagger UI

Open [http://localhost:8081](http://localhost:8081). The Compose Swagger UI service loads
[openapi.yaml](openapi.yaml) directly. Expand GET or POST, select **Try it out**, then **Execute**.
The requests go to the application at `http://localhost:8080`; CORS permits the two local Swagger
origins (`localhost:8081` and `127.0.0.1:8081`).

Swagger UI is a separate container. The application exposes no `/v3/api-docs`, Swagger UI, actuator
or error-controller API routes, keeping its contract to exactly the two operations above.

## Stop

Stop the foreground application with Ctrl+C, then run:

```sh
docker compose down
```

Compose preserves the database volume between runs.

## Design and assumptions

- **Small hexagonal design:** `TimeDeposit`, `TimeDepositCalculator` and `InterestRule` are the domain.
  `application/` contains the service, repository port and read models. `adapter/rest/` handles HTTP;
  `adapter/persistence/` implements the port using JDBC. The Spring Boot entry point wires them together.
  Domain and application classes have no Spring, HTTP or JDBC dependencies.
- **Extensible calculations:** a map of `InterestRule` functions replaces nested plan conditionals.
  The default constructor retains the existing plans. An additional constructor accepts rules for
  future plans or more complex calculations without changing the batch update algorithm.
- **Transaction boundary:** the repository port updates a batch through a callback. The JDBC adapter
  loads and locks rows, invokes the domain calculation and saves balances in one transaction. This
  keeps transaction mechanics out of the domain/application and prevents lost updates. Any failure
  rolls back the whole batch. A GET uses two queries for the entire result, avoiding per-deposit reads.
- **Rates and days:** despite the requirement's “monthly interest” wording, the working calculator
  divides the quoted rates by 12. That existing behavior takes precedence: basic is eligible from
  day 31; student on days 31-365 inclusive; premium from day 46. Days never advance on an update.
  Plan names remain case-sensitive; unknown plans earn no interest.
- **Money and compatibility:** the shared `TimeDeposit` class is unchanged, including its `Double`
  constructor, getter and setter. Interest still uses the original double arithmetic followed by
  `new BigDecimal(double).setScale(2, HALF_UP)`. A fully decimal rewrite would change existing results:
  a basic balance of 18 at day 31 earns 0.01, not 0.02. Repeated updates can also retain floating-point
  artifacts, e.g. 1000 becomes 1001.6600000000001 after two updates. Characterization tests preserve
  these results. Storage and API read models use `NUMERIC`/`BigDecimal`, without an extra rounding step
  or a fixed database scale. This preserves calculator compatibility; it does not make that legacy
  calculator suitable for arbitrary-precision financial arithmetic.
- **Withdrawals:** existing balances are assumed to already reflect past withdrawals. History is
  returned as `{id, timeDepositId, amount, date}` and is never deducted again. No withdrawal/create
  endpoints were added. Dates use ISO `yyyy-MM-dd`; results and histories are ordered by id.
- **Persistence:** Flyway creates the requested `timeDeposits` and `withdrawals` tables with required
  columns and a foreign key. Quoted camel-case names match the assignment. Demo data is separate
  from schema migrations. The local Compose credentials are development defaults; connection details
  can be overridden through `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_ADDRESS` and `PORT`.
- **Scope:** no authentication, input validation, custom exception handling or extra operations were
  introduced, following the exercise's scope. Local services bind to loopback for this workflow.

## Verification

The tests cover plan boundaries, legacy rounding, mixed batches, repeated accrual, extension rules,
empty storage, full JSON shape, withdrawals, database persistence, fractional balances, transaction
rollback, concurrent requests and agreement between OpenAPI paths and actual application routes.

See [AI_NOTES.md](AI_NOTES.md) for the development workflow and the final execution record.
