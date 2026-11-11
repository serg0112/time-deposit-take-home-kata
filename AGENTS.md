# Working rules for the Java solution

- Read the root README before changing behavior. Work in `java/`; leave other language variants unchanged.
- Preserve the public `TimeDeposit` API and `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` signature and behavior.
- Add characterization tests before refactoring. Cover boundaries and rounding with observable assertions.
- Keep domain and application code independent of Spring, HTTP and JDBC. Use small adapters and only introduce abstractions with a concrete use.
- Keep exactly two business endpoints. Do not add withdrawal operations or change deposit age during balance updates.
- Preserve the legacy floating-point calculation; document assumptions where they matter.
- Use PostgreSQL integration tests through Testcontainers. Never silently skip them when Docker is unavailable.
- Run `./mvnw clean verify`, then follow the run instructions against a real application and database. Report actual outcomes, including failures.
- Review the final diff for duplication, unused code, dependencies and accidental changes. Keep commits focused.
- Never send the submission email or publish changes to upstream.
