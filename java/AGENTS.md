# Time Deposit: project instructions

## Scope and sources of truth

This repository contains several versions of a refactoring exercise. Implement the solution in
`java/`; leave the other language variants unchanged. Root documentation may be updated for the
Java solution.

This is a take-home exercise, not a deployed service. Keep the initial schema and local setup simple;
do not introduce production upgrade paths, rollout machinery or speculative infrastructure.

- Read the root `README.md` for the assignment, `java/README.md` for design decisions and execution
  commands, and `java/openapi.yaml` for the HTTP contract before changing the corresponding behavior.
- The assignment explicitly requires preserving the working calculator. When the prose admits
  several interpretations, characterize the existing behavior before choosing an implementation.
- Record material assumptions beside the affected code and explain their consequences in the Java
  README. Distinguish assignment requirements from implementation choices.

## Compatibility and financial behavior

- Preserve existing `TimeDeposit` constructors, accessors and their types, and the public
  `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` signature. Compatible additions are allowed
  when justified. Follow the package organization requested by the repository owner while preserving
  the calculation model's constructor/accessor signatures. Immutable response DTOs are separate records.
- Preserve rate eligibility, operation order and rounding. Do not replace `BigDecimal(double)` with
  `BigDecimal.valueOf`, switch the calculation to decimal arithmetic, or round the final balance
  without treating that as a separate behavior change.
- Use `BigDecimal` for persisted/read-model monetary values. Do not introduce a database scale that
  silently rounds the calculator's output or an existing fractional balance.
- One update applies one monthly accrual. It does not advance days or deduct historical withdrawals.
  Repeated calls accrue again; do not introduce idempotency or scheduling implicitly.

## Design boundaries

- Follow the existing hexagonal packages: the calculation model, `InterestRule` and
  `TimeDepositCalculator` in `domain/`, use-case orchestration in `application/`, HTTP handling in
  `adapter/rest/`, Spring Data interfaces in `adapter/persistence/`, JPA entities in
  `adapter/persistence/data/`, and response records in `adapter/rest/response/`.
  Preserve unrelated package moves made by the repository owner.
- Use one `TimeDepositRepository extends JpaRepository` directly from the service. Spring Data
  supplies its implementation. Do not add pass-through repository ports or handwritten adapters
  without a concrete additional responsibility.
- Services use `@Service`, constructor injection and use-case transaction boundaries through
  one `@Transactional` on the balance-update method. Do not add a transaction annotation to the
  single-query read operation. Register the calculator through `@Service`; keep its calculations independent
  of controllers, repositories and JPA entities. Use configuration classes for actual configuration
  needs, not merely to instantiate a project-owned component.
- Use immutable records for response DTOs and ordinary classes for JPA entities. Use Lombok for
  entity getters, protected no-argument constructors and required injection constructors. Add setters
  only for state the application changes; do not generate relationship-based equality or `toString`.
  Keep the shared `TimeDeposit` API compatible and separate from persistence annotations.
- Keep interest eligibility and calculation in the calculator/rules, locking queries in the
  repository, and entity/DTO conversion in the service. Do not duplicate business rules in queries,
  controllers or DTOs.
- Preserve an atomic read/calculate/write batch. Hold row locks until commit and acquire them in
  a consistent order. A failure must not leave part of the batch persisted.
- Retrieve withdrawal history without per-deposit queries. Return empty histories as arrays.
- Use lowercase `snake_case` for every database table, column, index and constraint. Keep JSON fields
  in the camelCase specified by the API contract. Maintain one initial Flyway migration for this kata.
- Expose only the two application operations in the OpenAPI contract. Keep Swagger UI separate;
  do not add CRUD, withdrawal, actuator or documentation API routes as incidental framework defaults.
- Prefer explicit, small implementations. Add an interface, layer, dependency or pattern only when
  it solves a current requirement or removes concrete duplication. Avoid custom generic repository
  frameworks, duplicate business rules in persistence models and speculative extension points.

## Change workflow

1. Inspect the current diff and affected callers. Preserve unrelated local edits.
2. For refactoring, add characterization tests that pass against the existing implementation first.
   For a defect or new requirement, add a test demonstrating the missing behavior.
3. Make one coherent change and run its relevant tests before expanding the scope.
4. Review the resulting design for dependency direction, repeated business rules, unused code and
   unnecessary dependencies. Update the contract and documentation when their behavior changes.

Tests must assert observable results. Cover eligibility boundaries, rounding, mixed batches and
repeated calls. Test HTTP shape, persistence, rollback and concurrent accrual against real PostgreSQL
through Testcontainers. Do not replace these checks with mocks, tautological assertions, coverage
targets or silently skipped tests.

## Verification and completion

Run commands from `java/` with JDK 17+ and a working Docker Linux engine:

| Check | POSIX | PowerShell |
| --- | --- | --- |
| Calculator tests | `./mvnw -Dtest=TimeDepositCalculatorTest test` | `.\mvnw.cmd -Dtest=TimeDepositCalculatorTest test` |
| Full build and integration tests | `./mvnw clean verify` | `.\mvnw.cmd clean verify` |

For runtime/configuration changes, follow the Java README: start Compose, run the packaged JAR,
load demo data on a fresh database, and execute GET/POST/GET. Verify persisted balances and unchanged
withdrawals, not only HTTP status codes. Keep destructive test fixtures confined to Testcontainers.
Stop only application processes started for this task when a clean build requires releasing the JAR.

For documentation-only changes, validate links, commands and claims against the current code and
recorded execution results. A new full test run is needed only if the changes affect executable behavior.

Completion requires a reviewed diff, relevant passing checks and accurate run instructions. Report
what changed, why, the commands executed and their observed results, and any remaining limitations.
Distinguish verified behavior from assumptions and checks that have not been run.

## Git and delivery

- Keep commits focused and preserve the assignment's original requirements.
- Do not push, force-push, rewrite published history, open an upstream pull request or send the
  submission email. The repository owner handles publication and submission.
- Keep machine-specific configuration, credentials, session logs and build output out of commits.
