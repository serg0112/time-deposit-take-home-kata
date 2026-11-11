# AI-assisted workflow

## Tools and reproducible setup

OpenAI Codex desktop was used with repository access and shell execution. The project brief,
compatibility constraints, architecture boundaries and acceptance checks are maintained in
[../AGENTS.md](../AGENTS.md). This is the repository's agent configuration; it requires no personal
rules, private prompts or additional agent framework.

To repeat the workflow, clone this fork, open its root in a coding assistant that reads `AGENTS.md`,
install a JDK 17+ and Docker, and follow [README.md](README.md). Maven and dependency versions are
defined by `pom.xml` and the Maven Wrapper; database and Swagger image tags are pinned in Compose.

The workflow is requirements analysis, characterization tests, focused implementation changes,
design review and executable verification. Behavior changes require explicit justification;
refactoring alone is not a reason to change financial results.

## Engineering ownership and AI assistance

I used Codex as a coding assistant, with an explicit brief and acceptance criteria. I set the
priorities: understand the requirements and existing behavior first, establish a test safety net,
preserve compatibility, and keep the design small. I also required the documented setup to be
executed against a running application and database, rather than accepted from documentation alone.

I questioned the use of `Double` for money and discussed the compatibility implications of replacing
it with `BigDecimal`. The chosen approach preserves the existing calculator's observable results
while using decimal types for persistence and API read models. The rationale and limitations are
documented in [README.md](README.md#design-and-assumptions).

Codex assisted with implementation, tests and documentation under these constraints. The working
goal was faster routine implementation and broader exploration of boundary cases.
The working
agreement in [../AGENTS.md](../AGENTS.md) makes the scope, dependency boundaries and verification
requirements explicit. Changes were checked through characterization tests, PostgreSQL-backed
integration tests and a live walkthrough of the startup and API commands.

Final review and acceptance remain my responsibility. Before submission, I review the diff, trace
the calculation and transaction boundaries, assess whether the tests catch meaningful defects, and
make sure I can explain the design and its trade-offs. The execution record below covers checks
performed during development; it does not substitute for that review.

## Execution record

Verified on 2026-11-11 using Windows, Amazon Corretto JDK 17.0.11, Maven Wrapper 3.9.16 and Docker
Engine 28.4.0 with Linux containers:

- The characterization suite passed 29 tests against the original implementation before refactoring,
  with all calculator branches covered.
- The final JPA build, `.\mvnw.cmd --batch-mode clean verify`, passed all 30 unit tests and eight
  PostgreSQL/HTTP integration tests, with no failures, errors or skipped tests. The POSIX wrapper
  was also verified through Git Bash earlier in development.
- JaCoCo reported all domain branches covered; the unexercised production lines are the main-method
  launcher, which was exercised separately by running the built JAR.
- Ran `docker compose up -d --wait` and started the packaged JPA application using the README's Java
  command. Flyway migrations and Hibernate schema validation succeeded. Integration tests separately
  exercised migration and startup against a fresh PostgreSQL database.
- Executed the documented demo import and GET/POST/GET against a clean local database. POST returned
  204; balances changed from 1200 each to 1201, 1203, 1205, 1200, 1200. Days and withdrawal history
  remained unchanged. PostgreSQL metadata confirmed `time_deposits`, `plan_type` and `time_deposit_id`;
  JSON retained the contract's camelCase fields.
- The JPA integration tests verified atomic rollback, concurrent accrual and withdrawal fetching
  without N+1 queries: 12 deposits with histories required at most two database statements.
- Checked Swagger UI over HTTP: status 200, its initializer selected `./openapi.yaml`, and the served
  contract matched the checked-in file. A browser-style CORS preflight from `http://localhost:8081`
  was accepted for POST. Validated the complete OpenAPI document using
  `@apidevtools/swagger-parser` 13.1.0, installed outside the repository for this check.
- Checked Swagger independently of the Java process: 24 asset requests succeeded (maximum 262 ms),
  nginx configuration validation passed, and an internal HTTP request matched the mounted contract.
  A normal Compose down/up completed successfully and preserved the five demo deposits.
- The local setup uses one initial migration with snake_case database names. Repository access uses
  one Spring Data interface; services are registered by annotations without an extra configuration class.
- Follow-up on 2026-11-11: reorganized the existing classes into a hexagonal package layout — the
  calculation model, `InterestRule` and `TimeDepositCalculator` in `domain/`, the use-case service
  in `application/`, with the `adapter/rest/` and `adapter/persistence/` packages kept. Only file
  locations, package declarations and imports changed; no class name, constructor, annotation or
  calculation logic was modified. Re-running `.\mvnw.cmd --batch-mode clean verify` (OpenJDK 25,
  `--release 17`) passed all 30 unit tests and the eight PostgreSQL/HTTP integration tests.

Native Linux/macOS and interactive Swagger button clicks were not exercised; the POSIX
wrapper, served contract, CORS and actual API operations were checked as described above.
