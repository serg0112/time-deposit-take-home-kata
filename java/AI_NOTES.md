# AI-assisted development

## Tools and reproducible setup

OpenAI Codex desktop was used as a coding assistant with repository access and shell execution.
The project-specific agent instructions are checked in at [../AGENTS.md](../AGENTS.md); no additional
project system prompts, custom agents or external agent framework are required.

To repeat the workflow, clone this fork, open its root in a coding assistant that reads `AGENTS.md`,
install a JDK 17+ and Docker, and follow [README.md](README.md). Maven and dependency versions are
defined by `pom.xml` and the Maven Wrapper; database and Swagger image tags are pinned in Compose.

The task instructions given to the assistant were: read the requirements and existing code first;
test existing behavior before refactoring; implement the Java solution using DRY and clean code
without overengineering; preserve compatibility; and actually execute the documented build and
run instructions, including API requests against a real database.

## Where AI was used

- Read the original source, tests and build files, including a static review for suspicious code.
- Replaced the original tautological assertion with behavior tests before changing the calculator.
- Used test failures to characterize binary floating-point results rather than silently changing them.
- Extracted interest rules and implemented the application service, JDBC/REST adapters, migration,
  OpenAPI contract and development configuration.
- Added integration tests using actual PostgreSQL and HTTP instead of mocked repository behavior.
- Reviewed dependency direction, duplicate logic and unused code, and wrote and checked run instructions.

AI was useful for implementing and testing the routine adapters and for systematically exploring
boundary cases. Compatibility and financial precision were explicit design decisions, not assumed
from passing tests. The shared `TimeDeposit` class remains unchanged.

## Execution record

Verified on 2026-11-11 using Windows, Amazon Corretto JDK 17.0.11, Maven Wrapper 3.9.16 and Docker
Engine 28.4.0 with Linux containers:

- The characterization suite passed 29 tests against the original implementation before refactoring,
  with all calculator branches covered.
- The final clean build, `./mvnw --batch-mode clean verify`, also ran successfully through Git Bash
  on Windows. The PowerShell wrapper was used for the preceding test runs. All 30 unit tests and
  seven PostgreSQL/HTTP integration tests passed, with no failures, errors or skipped tests.
- JaCoCo reported all domain branches covered; the unexercised production lines are the main-method
  launcher, which was exercised separately by running the built JAR.
- Ran `docker compose up -d --wait`, started the packaged application using the README's Java command,
  and observed Flyway migrate an empty PostgreSQL database successfully.
- Executed the README's PowerShell demo import and GET/POST/GET commands. The first GET returned all
  five demo deposits and the historical withdrawal. POST returned 204 with no body; the next GET
  returned balances 1201, 1203, 1205, 1200 and 1200, preserving days and withdrawal history.
- Checked Swagger UI over HTTP: status 200, its initializer selected `./openapi.yaml`, and the served
  contract matched the checked-in file. A browser-style CORS preflight from `http://localhost:8081`
  was accepted for POST. Validated the complete OpenAPI document using
  `@apidevtools/swagger-parser` 13.1.0, installed outside the repository for this check.
- Stopped the application and ran `docker compose down`, rebuilt, then restarted both. Flyway found
  the schema up to date and all balances/history survived. Rerunning `demo.sql` inserted zero rows
  and left the updated data unchanged.

Early test expectations were corrected to match observed legacy floating-point results, including
1001.6600000000001 after repeated accrual and 1000.9530000000001 for an existing fractional balance.
Initial Compose attempts hit transient Docker startup errors; retrying once the engine was ready
succeeded. Native Linux/macOS and interactive Swagger button clicks were not exercised; the POSIX
wrapper, served contract, CORS and actual API operations were checked as described above.
