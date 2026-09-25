# Refactoring Log

This file documents every significant change made during refactoring: what the problem was, what decision was made, why (with reference to the governing standard), and what the impact is going forward.

---

# Upgrade to V2

* **Started:** 2026-08-25
* **Scope:** `OAuth2`, `AOP`, `Testing`, `Docker`, `Exception Handling`, `Migrations`, `Rate Limiting`, `Caching`, `Frontend`
* **Reference standards used:** `Governed-Refactoring-Standards.md`

---

## [Convert Entity Response and Request from Class to Record]

### **Problem:**

* Every Response and Request class has hard-coded implementation and uses Lombok to generate boilerplate code.
* These objects are primarily used for holding and transferring data, so mutable Java classes are unnecessary in this case.
* Java provides `record` specifically for immutable data-carrying objects.
* Using records reduces boilerplate code and makes the DTOs simpler and more explicit.

### **Decision:**

* Switch all Request and Response DTOs from Java classes to Java records.
* Remove unnecessary Lombok annotations such as `@Getter`, `@Setter`, and `@AllArgsConstructor`.
* Use Lombok `@Builder` where builder-style object creation is required.
* Keep validation annotations directly on record components.
* Keep DTOs immutable and use them only for transferring data between application layers.

### **Why:**

* Java Records were introduced as a concise way to model immutable data-carrying objects while reducing the need for boilerplate code.
  **Source:** [JEP 395 — Records](https://openjdk.org/jeps/395)

* Records automatically provide a canonical constructor, accessors, `equals()`, `hashCode()`, and `toString()`, which makes them suitable for DTOs whose primary purpose is to carry data.
  **Source:** [Java Language Specification — Record Classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.10)

* Spring Boot supports using Java records as request/response types, including JSON serialization and deserialization through Jackson.
  **Source:** [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/)

* Bean Validation annotations can be applied directly to record components, allowing Request DTO validation to remain declarative and concise.
  **Source:** [Jakarta Bean Validation Specification](https://jakarta.ee/specifications/bean-validation/3.0/)

* Using records makes DTO immutability explicit and reduces unnecessary mutable state, while Lombok `@Builder` can still be used when builder-style construction improves readability.

### **Impact:**

* All Request and Response DTOs are now implemented as Java records.
* Existing getter/setter usage must be updated to record accessor methods:
  * `getTitle()` → `title()`
  * `getUserId()` → `userId()`
  * `isActive()` → `isActive()`
* DTO construction using setters is no longer supported.
* Code that requires DTO modification must create a new record instance instead.
* MapStruct mappings are updated to work with record accessors and constructors.
* Validation behavior remains unchanged.
* API request and response JSON structures remain unchanged unless explicitly modified.
* This change affects the DTO layer only and does not change the underlying Entity model or database schema.
* Existing business logic remains unchanged; only the representation and construction of DTOs are refactored.

---

## [Adopt MapStruct for Entity-DTO Mapping]

### **Problem:**

* Entity-to-DTO and DTO-to-Entity conversions were implemented manually.
* Manual mapping introduces repetitive boilerplate code.
* Manual mapping can become difficult to maintain as entities and DTOs grow.
* Mapping logic is duplicated across different DTOs and entities.

### **Decision:**

* Adopt MapStruct as the standard mapping framework for Entity ↔ DTO conversion.
* Use `@Mapper(componentModel = "spring")` so generated mapper implementations can be injected as Spring beans.
* Use explicit `@Mapping` definitions when source and target properties have different names or when properties must be ignored.
* Use custom/default mapper methods only when automatic mapping cannot handle the required conversion.
* Keep repository access and business logic outside the mapper layer.

### **Why:**

* MapStruct generates type-safe mapping code at compile time instead of relying on reflection at runtime.
* Compile-time generation allows mapping errors to be detected during compilation.
* MapStruct reduces repetitive manual mapping code while keeping the generated implementation straightforward and readable.
* MapStruct provides Spring integration through `componentModel = "spring"`, allowing generated mappers to be injected as Spring beans.
* Keeping database access and business logic outside the mapper maintains separation of concerns: the mapper transforms data, while services handle business rules and entity retrieval.

**Sources:**

* [MapStruct Reference Guide — Introduction](https://mapstruct.org/documentation/reference-guide/)
* [MapStruct — Spring Component Model](https://mapstruct.org/documentation/reference-guide/#using-dependency-injection)
* [MapStruct — Mapping Customization with `@Mapping`](https://mapstruct.org/documentation/reference-guide/#mapping-customization-with-mapping)

### **Impact:**

* Manual Entity ↔ DTO mapping implementations are replaced with MapStruct mapper interfaces.
* Mapper implementations are generated during compilation.
* Spring can inject mapper implementations using constructor or field injection.
* DTO mapping becomes more consistent across the application.
* Custom conversion logic is kept only where automatic mapping is insufficient.
* Business logic and repository operations remain in the service/application layer.
* Existing API contracts and database entities are not changed by this decision.
* MapStruct becomes an additional compile-time dependency of the project.

---

## [Adopt Spring Data JPA Auditing for Entity Timestamps]

### **Problem:**

* `createdAt`/`updatedAt` timestamps were implemented manually in each entity using `@PrePersist`/`@PreUpdate` lifecycle callbacks.
* The same timestamp-setting logic was duplicated across every entity that required auditing.
* Manual lifecycle callbacks provide no built-in way to track *who* created or modified an entity, only *when*.
* Duplicated logic increases the risk of inconsistency if one entity's callback is written or updated incorrectly.

### **Decision:**

* Adopt Spring Data JPA Auditing as the standard mechanism for tracking entity creation and modification timestamps.
* Introduce a single `@MappedSuperclass` (`Auditing`) exposing `@CreatedDate` and `@LastModifiedDate` fields.
* Annotate `Auditing` with `@EntityListeners(AuditingEntityListener.class)` so the fields are populated automatically.
* Enable auditing infrastructure once at the application level via `@EnableJpaAuditing`.
* Entities requiring only `createdAt`/`updatedAt` extend `Auditing` instead of declaring their own `@PrePersist`/`@PreUpdate` methods.
* Entities with additional timestamp logic beyond simple creation/modification tracking (e.g. `HabitCompletion`'s `completedAt`, which depends on business state) are explicitly excluded from this migration and retain their manual lifecycle callbacks, since that logic is domain-specific and not a generic auditing concern.

### **Why:**

* Spring Data JPA Auditing is the framework-provided, documented mechanism for this exact use case, rather than a hand-rolled pattern repeated per entity.
* Centralizing the fields in one `@MappedSuperclass` removes duplicated lifecycle callback code across entities.
* The same infrastructure extends to `@CreatedBy`/`@LastModifiedBy` via an `AuditorAware` bean if user-level auditing is ever needed later, without changing the underlying pattern.
* Keeping auditing metadata separate from business-specific timestamp logic preserves separation of concerns: `Auditing` handles generic bookkeeping, while entities like `HabitCompletion` keep control of timestamps that are tied to domain rules.

**Sources:**

* [Spring Data JPA Reference Documentation — Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html)
* [EnableJpaAuditing (Spring Data JPA API)](https://docs.spring.io/spring-data/data-jpa/docs/current/api/org/springframework/data/jpa/repository/config/EnableJpaAuditing.html)

### **Impact:**

* Manual `@PrePersist`/`@PreUpdate` timestamp logic is removed from entities that only need generic creation/modification tracking.
* `createdAt`/`updatedAt` population is now handled by `AuditingEntityListener`, triggered automatically on persist/update.
* Entities extend `Auditing` (`@MappedSuperclass`) instead of declaring their own timestamp fields and columns.
* `@EnableJpaAuditing` is now required at the application configuration level for the fields to populate correctly.
* Entities with domain-specific timestamp logic (`HabitCompletion`) are intentionally not migrated and retain their existing manual callbacks.
* No changes to existing API contracts or database column names/types — `created_at`/`updated_at` columns remain as before.
* Adding `@CreatedBy`/`@LastModifiedBy` later (if ever needed) only requires an `AuditorAware` bean, not a redesign of this pattern.

---

## [Adopt Global Exception Handling with a Custom Exception Hierarchy and RFC 7807 Problem Details]

### **Problem:**

* Exception handling was implemented inconsistently: only 4 of 14 controllers had any `try/catch` logic, and the remaining 10 let exceptions surface as raw, unhandled 500 errors with no consistent response shape.
* Error handling relied on generic Java exceptions (`IllegalArgumentException`, `NoSuchElementException`, `IllegalStateException`) that carry no information about the correct HTTP status code, forcing every catch site to re-derive the right response manually.
* A custom exception class (`com.mts.aadati.exception.AuthenticationServiceException`, note the package typo) was defined but never thrown — `AuthenticationService` instead threw Spring Security's built-in `AuthenticationServiceException` for three semantically different situations (invalid credentials → 401, user not found → 404, internal failure → 500), collapsing distinct error types into one.
* Internal exception messages (e.g. failure details from the auth flow) were at risk of being returned directly to the client, which conflicts with standard REST security guidance on not leaking internal error details.
* There was no standardized response body shape for errors, making it harder for any API consumer to parse errors consistently across endpoints.

### **Decision:**

* Introduce a small, fixed set of custom runtime exceptions, each mapped to exactly one HTTP status code based on who is responsible for the error condition (client vs. server) rather than one exception per entity/resource:
  * `ResourceNotFoundException` → 404
  * `DuplicateResourceException` → 409
  * `InvalidRequestException` → 400
  * `BusinessRuleViolationException` → 422
  * `InvalidCredentialsException` → 401
  * `OperationFailedException` → 500
* Replace all existing `IllegalArgumentException` / `NoSuchElementException` / `IllegalStateException` / misused `AuthenticationServiceException` throw sites in the service layer with the appropriate exception from the list above.
* Build a single `@RestControllerAdvice` (`GlobalExceptionHandler`) handling all six custom exceptions, `MethodArgumentNotValidException` (bean validation failures), and a generic `Exception` fallback.
* Use Spring's built-in `ProblemDetail` (RFC 7807 "Problem Details for HTTP APIs") as the response body format for every handled exception, giving a consistent `type`/`title`/`status`/`detail`/`instance` shape across the entire API.
* For server-side failures (`OperationFailedException`, the generic `Exception` fallback), return a fixed generic message in the response body (e.g. "An internal error occurred. Please try again later.") instead of the raw exception message, while still logging the real message server-side for debugging.
* For client-facing exceptions where the message is authored intentionally as part of the business logic (`ResourceNotFoundException`, `DuplicateResourceException`, `InvalidRequestException`, `BusinessRuleViolationException`, `InvalidCredentialsException`), return the exception's message as-is, since it's meant to be read by the API consumer.
* For validation failures, extend the `ProblemDetail` response with a structured `errors` property mapping field names to validation messages, rather than a single generic detail string.
* Authentication/authorization failures that occur before a request reaches a controller (missing/invalid JWT, insufficient role) continue to be handled by the existing `JwtAuthenticationEntryPoint` (401) and `JwtAccessDeniedHandler` (403) in the Spring Security filter chain — these are explicitly out of scope for `GlobalExceptionHandler`, since they occur at a different layer of the request lifecycle.
* Rate limiting is not implemented in the codebase (the `bucket4j` dependency is present but unused), so no exception type for it is introduced at this time — this will be added alongside the actual rate-limiting implementation if/when that feature is built, not ahead of it.

### **Why:**

* RFC 7807 defines a standard, machine-readable JSON format for HTTP API error responses specifically to avoid every API inventing its own incompatible error shape.
  **Source:** [RFC 7807 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807.html)
* The OWASP REST Security Cheat Sheet's error handling guidance states that error responses must be generic and must not reveal internal failure details, stack traces, or technical implementation hints to the client — the basis for returning fixed generic messages on 500-class errors while preserving the real message in server logs.
  **Source:** [OWASP REST Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html)
* Classifying exceptions by "who is at fault and what HTTP status code follows" (rather than one exception per entity/resource) keeps the exception hierarchy small and directly aligned with the HTTP semantics the API needs to express, avoiding both under-differentiation (collapsing 401/404/500 into one type, as the old `AuthenticationServiceException` misuse did) and over-differentiation (a separate `UserNotFoundException`/`HabitNotFoundException`/etc. that would all resolve to the same 404 handler regardless).
* Authentication (401) and authorization (403) failures at the Spring Security filter level occur before request routing reaches any controller or service method, so they are handled by Security's own `AuthenticationEntryPoint`/`AccessDeniedHandler` mechanism rather than being retrofitted into the same exception hierarchy used for business/service-layer errors.

### **Impact:**

* All controllers now return a consistent `ProblemDetail` (RFC 7807) response body for every handled error case, regardless of which controller or service raised it.
* The 10 controllers that previously had no exception handling, and the 4 that had inconsistent manual `try/catch`, are now unified under a single global handler — controller-level `try/catch` blocks for these cases are removed.
* Server-side failure messages (500-class) are no longer returned verbatim to API consumers; the real message is only available in server-side logs.
* Validation errors now return structured, per-field error details instead of a single generic message, making the API usable by any frontend that needs to highlight specific invalid fields.
* The dead `com.mts.aadati.exception.AuthenticationServiceException` class is removed once all its (non-existent) usages and Spring's built-in equivalent's misused throw sites are migrated to the new exception types.
* `AuthenticationService` throw sites are split correctly across `InvalidCredentialsException` (401), `ResourceNotFoundException` (404), and `OperationFailedException` (500) instead of a single ambiguous exception type.
* Authentication/authorization failures at the security-filter level (401/403) remain unchanged and continue to be handled separately from this exception hierarchy.
* Adding a rate-limiting exception type is deferred until rate limiting is actually implemented, avoiding speculative/unused exception classes.

---

## [Repository Layer Refactoring Pass — Query Correctness, Performance, and Access Control]

### **Problem:**

* Several repository queries used `LIKE '%...%'` with non-standard JPQL syntax (`LIKE %:param%` instead of `LIKE CONCAT('%', :param, '%')`), which is not guaranteed to work reliably across JPA providers.
* Several `Page<Entity>` queries returning entities with `@ManyToOne` relationships had no `JOIN FETCH`, causing N+1 queries whenever the calling code accessed those relationships (e.g. displaying a category or username alongside a list result).
* Several repositories duplicated near-identical queries differing only in a boolean condition (e.g. separate `findCompleted...`/`findUncompleted...` method pairs), doubling the number of methods to maintain for no behavioral benefit over a single boolean parameter.
* Aggregate `COUNT()` queries were typed as `Long` (nullable wrapper) despite `COUNT()` never returning `null`, while some `AVG()` queries were correctly typed as `Double`/left un-typed as primitives inconsistently across repositories.
* One dynamic filter query (`TaskPriorityLevelRepository.searchByPriorityLevelOrColor`) combined optional-filter conditions with `OR` instead of `AND`/correctly-negated `OR`, causing the query to return all rows whenever either parameter was `null` — a functional correctness bug, not just a style issue.
* Several repository methods accepted a full `User` entity as a query parameter purely to filter by identity, requiring an unnecessary `User` lookup before the query could run, when the caller (typically the Security Context) already has the user's ID.
* `UserRepository` exposed several methods that returned full personal data (email, first/last name) via partial-match search (`findByEmailContainingIgnoreCase`, etc.) or unpaginated full-table scans (`findAllWithRoles`), allowing broader data exposure than any actual use case required — particularly relevant since some of these were intended for admin-facing statistics, not per-user data browsing.
* One `HabitCalendarRepository` query attempted to `JOIN FETCH` two `@OneToMany` collections (`habitCompletions` and `taskCompletions`) in the same query for a `COUNT`, which would produce an incorrect (multiplied) count due to the Cartesian product created by fetch-joining two to-many collections simultaneously.

### **Decision:**

* Standardize all partial-text search queries on `LOWER(field) LIKE LOWER(CONCAT('%', :param, '%'))` for case-insensitive, provider-safe partial matching; replace exact-value comparisons (e.g. enum values) that were incorrectly using `LIKE` with plain `=`.
* Add `LEFT JOIN FETCH` for `@ManyToOne`/`@OneToOne` relationships on any query whose result is expected to be read (not just counted) by calling code, while never combining more than one `@OneToMany`/`@ManyToMany` fetch-join in the same query (splitting into separate queries instead where both collections are needed).
* Merge boolean-condition method pairs into a single method taking the boolean as a parameter (e.g. `findByHabitAndUserAndComplete(habit, userId, complete)` replacing separate completed/uncompleted methods).
* Standardize `COUNT()` query return types on primitive `long` (never `null`) and `AVG()` query return types on `Double` (nullable, since `AVG()` over an empty result set returns `NULL`).
* Rewrite the `OR`-based optional-filter query to correctly short-circuit on `null` parameters (`(:param IS NOT NULL AND field = :param) OR (...)`), and separately remove a similar method entirely where the underlying use case (searching by priority level OR color together) was determined not to reflect an actual product need.
* Standardize repository methods on accepting `UUID userId` instead of a full `User` entity wherever the query only needs to filter by identity, since the Security Context already exposes the authenticated user's ID directly — avoiding an unnecessary `User` lookup before every query call. Full `User` entities are still accepted where a query genuinely needs a `User`-specific field.
* Remove or convert to aggregate-only form any `UserRepository` method that exposed raw personal data via partial-match search or an unbounded full-table read; retain only exact-identity lookups (`findByEmail`, `findByUsername`, `findById` inherited from `JpaRepository`) and count/aggregate-based statistics methods (`countUsersCreatedBetween`, `countUsersByRoleName`, etc.) for admin-facing use cases.
* Split the double-`@OneToMany`-fetch `COUNT` query in `HabitCalendarRepository` into two separate single-collection queries (`countDaysWithCompletedHabitByWeek`, `countDaysWithCompletedTaskByWeek`), leaving the decision of how to combine the two counts to the service layer.

### **Why:**

* Hibernate's `MultipleBagFetchException` and the underlying cartesian-product problem occur specifically when more than one `@OneToMany`/`@ManyToMany` collection is fetch-joined in a single query; splitting into separate queries is the documented way to avoid both the exception and silently incorrect aggregate results.
  **Source:** [Hibernate ORM User Guide — Fetching](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching)
* The OWASP API Security Top 10 identifies "Excessive Data Exposure" and "Broken Object Level Authorization" as leading causes of API data breaches — returning more data than a given use case requires (e.g. full personal records via partial search, rather than an exact lookup or an aggregate count) is exactly the pattern this guidance warns against.
  **Source:** [OWASP API Security Project](https://owasp.org/www-project-api-security/)
* `COUNT()` in JPQL/SQL is defined to always return a non-null numeric value (zero for an empty result set), while `AVG()` over an empty result set returns `NULL` by definition — the return type should reflect this distinction rather than defensively wrapping every aggregate in a nullable type.
* Passing an entity's ID instead of the full entity where only identity comparison is needed avoids an unnecessary database round-trip and keeps the repository method's contract explicit about what data it actually requires — the ID is already available from the Security Context at the point these methods are called.

### **Impact:**

* All partial-text search queries across the repository layer now use consistent, case-insensitive, provider-safe `LIKE` syntax.
* Queries returning entities with `@ManyToOne` relationships that calling code reads now fetch those relationships eagerly in the same query, eliminating N+1 query patterns in list/page endpoints.
* The number of near-duplicate boolean-pair methods across `HabitCompletionRepository`, `TaskCompletionRepository`, and others is reduced by roughly half, with a single parameterized method replacing each pair.
* `COUNT()`-based methods consistently return primitive `long`; `AVG()`-based methods consistently return `Double` with the nullability handled explicitly at the call site.
* The `TaskPriorityLevelRepository` dynamic-filter bug (returning all rows when a filter parameter was `null`) is fixed; the unused combined search method is removed entirely.
* Most repository methods across `Habit`, `HabitTask`, `HabitCompletion`, `PercentageDay`, `PercentageWeek`, and `HabitCalendar` now accept `UUID userId` instead of a full `User` entity — this is a breaking change to method signatures requiring corresponding updates in every service-layer call site.
* `UserRepository` no longer exposes partial-match search or unbounded full-table methods over personal data; admin-facing functionality is limited to exact-identity lookups and aggregate statistics (counts), consistent with the decision that administrative access should not include browsing individual users' personal data.
* The `HabitCalendarRepository` weekly-completion count is now computed via two separate queries instead of one query with an incorrect double-collection fetch-join; combining the two values (if needed) is a service-layer responsibility.
* This pass covered `HabitRepository`, `HabitTaskRepository`, `HabitCompletionRepository`, `HabitDayWeekRepository`, `HabitWeekRepository`, `HabitCalendarRepository`, `HabitCategoryRepository`, `PercentageDayRepository`, `PercentageWeekRepository`, `RoleRepository`, `TaskCompletionRepository`, `TaskPriorityLevelRepository`, and `UserRepository`.

---

## [Service Layer Refactoring Pass — Consistency, Correctness, and Layering Discipline]

### **Problem:**

* Services were written with inconsistent transaction annotation style — some methods individually marked `@Transactional`/`@Transactional(readOnly = true)` on every method, rather than a single class-level default with explicit overrides on write methods.
* Several services duplicated resource-lookup logic (`existsById` followed by a separate query) instead of a single centralized `getXOrThrow(id)` helper, resulting in extra round-trips and repeated `orElseThrow` boilerplate across methods.
* Some services returned `NullPointerException`-prone types: an aggregate query capable of returning `NULL` (`AVG()` over an empty result set) was declared as a primitive `double` return type in one service, guaranteeing an NPE via auto-unboxing whenever a user had no matching records.
* Update methods in several services duplicated validation logic that already existed inside the entity itself (e.g. re-checking a rate is between 0 and 100 in the service, when the entity's own domain method already enforces this) — a direct contradiction of the project's earlier decision to keep such invariants in a rich domain model.
* One service caught a `ResourceNotFoundException`-equivalent internally (via a checked lookup failure) and silently returned an empty result instead of letting the error propagate — the same "swallow the error, return empty" antipattern previously identified and rejected in the SAS project's reactive controllers.
* Two `@Modifying` cleanup-job queries were copy-pasted from another entity's repository method without updating the entity name in the JPQL, meaning a scheduled cleanup job for one entity would have deleted rows from a completely different table.
* One service's `@PrePersist`/`@PreUpdate` lifecycle method was misnamed relative to what it actually did, and was also mistakenly triggered on both create and update, causing a `createdAt` timestamp to be overwritten on every update — defeating the purpose of a creation timestamp.
* Several services accepted a full parent entity (e.g. `User`, `PercentageDay`) as a method parameter purely to extract one or two fields from it, which is a different, undocumented convention from services that accepted only the specific field values needed — creating inconsistency in method signatures across the service layer.
* Some duplicate-prevention checks in update methods failed to exclude the entity being updated from the uniqueness check, meaning a user could never update their own record without changing the unique field, since the record would always "conflict with itself."

### **Decision:**

* Standardize every service on class-level `@Transactional(readOnly = true)` as the default, with individual write methods (create/update/delete) explicitly annotated `@Transactional` to override the default.
* Introduce a private `getXOrThrow(id)` helper method in every service that owns an entity, used everywhere that entity needs to be loaded or have its existence confirmed — replacing scattered `existsById` + separate-query pairs.
* Audit every aggregate query's return type against whether the underlying SQL/JPQL aggregate function can return `NULL` (`COUNT()` cannot; `AVG()` over an empty set can) and size the return type accordingly — nullable wrapper types (`Double`) resolved to a safe primitive default (`0.0`) inside the service method itself, so the unsafe `null` value never crosses the service's public API boundary.
* Remove validation logic from service methods wherever the corresponding entity already enforces that invariant via a domain method (e.g. `PercentageDay.updateRate()`); the service calls the domain method and trusts it to enforce its own rules.
* Remove all "catch an internal not-found condition and return an empty result" patterns; let `ResourceNotFoundException` (or an equivalent explicit failure) propagate through `GlobalExceptionHandler` instead of being silently absorbed.
* Fix all copy-pasted `@Modifying` cleanup queries to reference the correct entity, and verify each cleanup query independently by checking the `FROM` clause's entity name against the repository's actual generic type.
* Correct lifecycle callback naming and scope: timestamp-setting logic runs only in `@PrePersist` for creation timestamps, never in `@PreUpdate`, and method names describe what the method actually does.
* Standardize on accepting only the specific parameter values a method needs (IDs, primitive values, or a request-shaped set of fields) rather than a full entity, except where a service explicitly mirrors another already-agreed service's established pattern for consistency within that pair of related entities (e.g. `PercentageWeekService`/`PercentageDayService` deliberately accepting a full entity for create/update, matching each other).
* For update methods with a uniqueness constraint, use the "exclude self" repository query pattern (`existsByFieldAndIdNot`) so a record's own unchanged field never triggers a false-positive duplicate error against itself.

### **Why:**

* Constructor injection (via `@RequiredArgsConstructor` on `final` fields) is the community- and framework-recommended dependency injection style in Spring, since it guarantees a bean cannot be instantiated in an incomplete state and keeps dependencies explicit and testable.
  **Source:** [Spring Framework Reference — Constructor-based or Setter-based DI](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)
* Declaring `@Transactional(readOnly = true)` at the class level and overriding it per write method is the pattern demonstrated in Spring's own reference documentation for exactly this scenario, and avoids repeating the annotation on every single method.
  **Source:** [Spring Framework Reference — Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
* `COUNT()` is defined to always return a non-null numeric value, while `AVG()` over an empty result set returns `NULL` by definition — a service's return type should reflect this rather than risk an unboxing `NullPointerException` at an arbitrary call site.
* Keeping domain invariants (like a valid rate range) enforced exclusively inside the entity, rather than duplicated in every service method that touches it, was already established project-wide as part of the rich domain model direction — duplicating the check in the service reintroduces the exact inconsistency that decision was meant to prevent.
* Swallowing an internal failure and returning an empty/default result silently corrupts the API's meaning from the caller's perspective (this was already identified as a critical defect pattern in the SAS reactive controllers) — the same reasoning applies identically in a blocking service method.

### **Impact:**

* All services now follow a single, predictable transaction-annotation shape, making it obvious at a glance which methods perform writes.
* Resource-lookup logic is centralized per service (`getUserOrThrow`, `getRoleOrThrow`, `getPercentageWeekOrThrow`, `getCompletionOrThrow`, etc.), eliminating duplicate `existsById` + query pairs and the extra database round-trip each one cost.
* The `PercentageWeekService`/`PercentageDayService` average-rate methods no longer risk an NPE for users with zero matching records; they now return a safe default (`0.0`) resolved inside the service.
* `PercentageDay`'s rate-range validation now lives exclusively in `PercentageDay.updateRate()`, with the service delegating to it instead of duplicating the check — consistent with the project's rich domain model decision.
* Two cleanup jobs (originally copy-pasted with the wrong entity name in their `@Modifying` query) now correctly target their own entity's table; this was caught before being run against a live database.
* `HabitWeek`'s creation-timestamp lifecycle callback no longer overwrites `createdAt` on every update, and is renamed to describe its actual behavior.
* Update methods with uniqueness constraints (`User`, `Role`, `TaskPriorityLevel`) now correctly exclude the record being updated from their duplicate-check queries, fixing a class of bug where a user could not save an update without an unrelated field triggering a false "already exists" error.
* This pass covered `UserService`, `RoleService`, `TaskPriorityLevelService`, `PercentageWeekService`, `PercentageDayService`, `TaskCompletionService`, `HabitWeekService`, and `HabitDayWeekService`.

---

## [Introduce Redis for Business-Data Caching and JWT Token Revocation (Blocklist)]

### **Problem:**

* JWTs are stateless by design, which means there was no way to invalidate an access or refresh token before its natural expiration — a logged-out user's token remained fully valid until it expired on its own, and a leaked token could not be revoked at all.
* Frequently-read, rarely-changing reference data (e.g. `HabitCategory`, `TaskPriorityLevel` listings) had no caching layer, meaning every request re-queried the database for data that changes infrequently.
* The initial `RedisConnectionFactory` bean was created with `new LettuceConnectionFactory()` and no host/port/password configuration, meaning it silently always connected to `localhost:6379` regardless of what was set in `application.properties` or environment variables — a real deployment-breaking bug had it gone unnoticed until production.

### **Decision:**

* Introduce Redis as shared infrastructure for two distinct, separately-configured purposes, kept conceptually separate even though they run on the same Redis instance:
  1. **General-purpose caching** (`CachingConfig`, `@EnableCaching` + `RedisCacheManager`) for read-heavy, rarely-changing business data via `@Cacheable`.
  2. **JWT blocklist (denylist)** for token revocation — a logged-out token's `jti` is stored in Redis (via `RedisTemplate` directly, not through the `@Cacheable` abstraction) with a TTL matching the token's remaining lifetime, and `JwtAuthenticationFilter` checks this blocklist before accepting an otherwise-valid, unexpired token.
* Fix `RedisConnectionFactory` to source its host/port/password from application configuration instead of hardcoding a no-argument `LettuceConnectionFactory`.
* Do not treat the blocklist as "cached data" — it is a revocation-lookup mechanism with its own TTL semantics (tied to token expiry, not a fixed cache duration), so it is implemented against `RedisTemplate` directly rather than reusing the `@Cacheable`/`RedisCacheManager` path meant for business data.

### **Why:**

* Stateless JWTs have no built-in revocation mechanism by design; a server-side denylist checked at request time is a recognized mitigation for scenarios that require immediate invalidation (such as logout or a compromised token), without abandoning the stateless model for every token.
  **Source:** [OWASP JSON Web Token Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_Cheat_Sheet.html)
* `RedisCacheConfiguration`'s `entryTtl(Duration)` and `enableTimeToIdle()` are the framework-documented way to bound cache entry lifetime for the Spring Cache abstraction; `enableTimeToIdle()` specifically requires Redis 6.2.0+, which must be confirmed against whatever Redis version is used in deployment (e.g. a managed Redis add-on).
  **Source:** [Spring Data Redis Reference — Redis Cache](https://docs.spring.io/spring-data-redis/reference/redis/redis-cache.html)
* Hardcoding connection details in a `@Bean` method bypasses environment-based configuration entirely, which is the same class of problem already addressed for application secrets elsewhere in this project (config belongs in the environment, not hardcoded in source).

### **Impact:**

* Logout can now genuinely invalidate a token immediately (subject to the blocklist being checked on every authenticated request), rather than relying solely on the token's natural expiration window.
* `RedisConnectionFactory` now respects `application.properties`/environment configuration for host, port, and password, fixing what would otherwise be a silent failure to connect to any non-local Redis instance in a deployed environment.
* `CachingConfig` and the token blocklist mechanism remain independently configurable — clearing or resizing the business-data cache has no effect on token revocation behavior, and vice versa.
* Before enabling `enableTimeToIdle()` in production, the deployed Redis version must be confirmed to be 6.2.0 or newer.

---

## [JWT Authentication Filter Refactor — Single Source of Truth for Authorities, Not Duplicated in Token Claims]

### **Problem:**

* `JwtUtils.generateAccessToken()` originally embedded the user's roles as a JWT claim, with the stated goal of avoiding a database lookup during request authentication (a common stateless-JWT optimization).
* In practice, `JwtAuthenticationFilter` must call `UserDetailsService.loadUserByUsername()` regardless, in order to construct the `UserDetails` object required by `UsernamePasswordAuthenticationToken` — and that lookup already returns the user's current roles as part of the same query.
* This meant the roles embedded in the JWT were redundant: the "avoid a database query" justification for storing them in the token did not hold, since the query happens anyway on every authenticated request.
* Storing roles in the JWT also introduced a staleness risk: if an admin changed a user's role, the change would not take effect until the user's existing token expired and a new one was issued, since the filter would otherwise have preferred the (stale) claim data over the fresh database read.
* Separately, `JwtAuthenticationFilter` read the token from a header named `"Authentication"` instead of the standard `"Authorization"` header, meaning no client sending a token in the conventional way would ever be authenticated.
* Exceptions thrown while parsing an invalid or expired token (`InvalidCredentialsException`, raised from `JwtUtils.extractAllClaims()`) were not caught inside the filter, and since servlet filters run before Spring MVC's dispatcher, these exceptions would bypass `GlobalExceptionHandler` entirely and surface as an unhandled 500 error instead of the intended 401 response via `JwtAuthenticationEntryPoint`.

### **Decision:**

* Remove the `role` claim from JWT generation entirely. `JwtAuthenticationFilter` sources authorities exclusively from `UserDetailsService.loadUserByUsername()` (i.e. from the database, via `CustomUserDetailsService`), since that call is already unavoidable for constructing `UserDetails`.
* JWT claims are limited to what cannot be cheaply re-derived on every request: subject (username), token type (`access`/`refresh`), issued-at/expiration, and the `jti` used for blocklist lookups.
* Fix the header name read by the filter to the standard `Authorization` header.
* Wrap the token-parsing/validation logic inside `JwtAuthenticationFilter` in a try/catch for the custom JWT exceptions, allowing the filter chain to continue with no authentication set (rather than propagating the exception) when a token is malformed or expired — letting `JwtAuthenticationEntryPoint` handle the resulting 401 for any endpoint that actually requires authentication.

### **Why:**

* Storing data in a token specifically to avoid a database read only pays off if that read is actually avoided; here it wasn't, since `UserDetailsService` is called on every request regardless. Keeping a second, potentially stale copy of the same data (in the token) for no realized performance benefit is unnecessary duplication.
* Deriving authorities from the database on every request, rather than from token claims, means a role change takes effect on the user's very next request rather than only after their current token expires — a meaningful correctness property for an admin-managed roles system.
* Servlet filters execute before Spring MVC's `DispatcherServlet`, so exceptions thrown inside a filter are not visible to `@RestControllerAdvice`-based exception handling; they must be handled locally within the filter (or delegated explicitly to the configured `AuthenticationEntryPoint`/`AccessDeniedHandler`) to produce a consistent API response instead of a generic server error.

### **Impact:**

* JWT payloads are smaller and contain no data that can go stale relative to the database.
* Role/permission changes made by an admin take effect immediately on the affected user's next request, without waiting for token expiration.
* Requests with a valid `Authorization: Bearer <token>` header are now actually recognized by the filter (previously silently ignored due to the incorrect header name).
* Malformed or expired tokens now result in the request proceeding as unauthenticated (and subsequently a clean 401 via `JwtAuthenticationEntryPoint` if the endpoint requires authentication) instead of an unhandled 500 error.
* This trades a small, already-necessary database read per authenticated request for always-current authorization data — an explicit, deliberate choice given this project's scale, not a default assumed without considering the alternative.

---

## [MapStruct Write Accessors — Field-Level `@Setter` for Structural Relationships vs. `ignore = true` for Rule-Guarded Fields]

### **Problem:**

* Compiling `HabitCalendarMapper`, `TaskCompletionMapper`, `HabitTaskMapper`, and `HabitCompletionMapper` failed with MapStruct errors of the form `Property "x" has no write accessor in Y`, for relationship fields: `HabitCalendar.habitWeek`, `TaskCompletion.habitTask`/`habitCalendar`, `HabitTask.taskPriorityLevel`/`habitCategory`, and `HabitCompletion.habit`/`habitCalendar`.
* These errors surfaced from the mapper-based `update()` pattern already anticipated in `REFACTORING_BACKLOG.md` (`HabitTaskMapper.update(request, taskPriorityLevel, habitCategory, @MappingTarget habitTask)`): the service resolves each ID to its full entity and passes it into the mapper as a parameter, and MapStruct auto-matches that parameter to the target property of the same name/type — which requires a write accessor MapStruct can call, and none of the affected entities exposed one for these fields.
* Two superficially equivalent fixes were on the table: add a blanket `@Setter` to unblock compilation, or mark every one of these mappings `@Mapping(target = "x", ignore = true)`. Neither is a real decision on its own — a blanket `@Setter` risks quietly reopening fields that intentionally have no public setter because a domain method (`updateRate()`, `markComplete()`/`markIncomplete()`) already enforces a business rule on them, while blanket `ignore = true` just defers the same "how does this field actually get written" question to the service without resolving it, and can silently mean a field that legitimately needs the mapper to set it, never gets set at all.

### **Decision:**

* Classify each relationship field involved in a mapper `update()` method by **whether the entity enforces an invariant when that field is reassigned**, not by whichever option happens to make the code compile:
  * **Structural relationships with no reassignment invariant** — `HabitTask.taskPriorityLevel`, `HabitTask.habitCategory`, `HabitCalendar.habitWeek`, `TaskCompletion.habitTask`, `TaskCompletion.habitCalendar`, `HabitCompletion.habit`, `HabitCompletion.habitCalendar` — get a **field-level Lombok `@Setter`** (never a class-level `@Setter`/`@Data`). The service resolves the ID to the entity and passes it into the mapper's `update(request, resolvedEntity, @MappingTarget entity)` method, consistent with the existing layering rule that mappers receive service-resolved entities as parameters rather than looking them up themselves.
  * **Fields with an entity-enforced business rule on reassignment** — `PercentageDay.rate` (guarded by `updateRate()`), `HabitCompletion.complete` (guarded by `markComplete()`/`markIncomplete()`) — stay setter-less. The mapper marks these `@Mapping(target = "x", ignore = true)`, and the service calls the entity's own domain method directly after the mapper runs the rest of the update.
* Document the test to apply going forward: *does a named domain method on the entity enforce an invariant when this field changes?* If yes, no setter — ignore it in the mapper and call the domain method from the service. If no, add a field-level setter and let the mapper set it directly.

### **Why:**

* This directly extends the project's already-adopted rich domain model direction (`PercentageDay.updateRate()`, `HabitCompletion.markComplete()`/`markIncomplete()`): a field that already has a validating domain method must not gain a parallel, unvalidated `@Setter` path, since that setter would let any future caller bypass the entity's own invariant entirely — the same defect class the `PercentageDay.rate` setter removal was meant to close in the first place.
* MapStruct's default mapping strategy resolves target properties via accessor methods and requires a visible setter (or an explicit strategy/expression) to write into a property from a source parameter — a property with no write accessor cannot be part of an automatic mapping, which is why the compile error appears specifically on relationship fields the mapper is trying to set from a resolved-entity parameter.
  **Source:** [MapStruct Reference Guide — Mapping Object References / Bean Mapping](https://mapstruct.org/documentation/reference-guide/)
* Structural relationship fields (`taskPriorityLevel`, `habitCategory`, `habitWeek`, `habitTask`, `habitCalendar`, `habit`) carry no validation logic of their own when reassigned — they are plain associations, not business-rule-guarded state — so exposing a narrow, field-level setter for exactly these fields does not reintroduce the anemic-model problem the project has been deliberately moving away from; it only exposes write access where no invariant exists to protect.

### **Impact:**

* `HabitCalendarMapper`, `TaskCompletionMapper`, `HabitTaskMapper`, and `HabitCompletionMapper` now compile: `HabitCalendar.habitWeek`, `TaskCompletion.habitTask`/`habitCalendar`, `HabitTask.taskPriorityLevel`/`habitCategory`, and `HabitCompletion.habit`/`habitCalendar` each receive a field-level `@Setter`, scoped to that field only.
* `PercentageDay.rate` and `HabitCompletion.complete` remain setter-less; any mapper touching them uses `@Mapping(target = "rate"/"complete", ignore = true)`, with the service calling `updateRate()`/`markComplete()`/`markIncomplete()` explicitly — no regression on the rich domain model work already done for these fields.
* `ENGINEERING_RULES.md` (Section 8: DTOs & Mapping) now documents this classification test explicitly, so future mapper `update()` methods on new relationship fields are resolved the same way instead of re-litigating "setter vs. ignore" per entity.
* This pass covered `HabitCalendarMapper`, `TaskCompletionMapper`, `HabitTaskMapper`, and `HabitCompletionMapper`; any future entity with a `@ManyToOne`/`@OneToOne` relationship written through a mapper `update()` method should be classified using the same test before adding either a setter or an `ignore = true`.

---

## [Fix — AOP Service-Logging Aspect Could Break the Calls It Was Meant to Only Observe]

### **Problem:**

* The first draft of `AopLogging` (the single `@Aspect`/`@Around` advice adopted for cross-cutting service-layer logging, per the earlier AOP activation decision) resolved the current user for its log line by casting `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` directly to `CustomUserDetails`, with no null check and no try/catch, executed *before* `joinPoint.proceed()` and outside any exception handling.
* Any service call made with no authenticated `CustomUserDetails` in context — which includes `AuthenticationService.login()`/`register()` themselves (the whole point of those calls is that no one is authenticated yet), and any future `@Scheduled` job such as the planned `HabitCompletionCleanupScheduler` — would throw a `NullPointerException` or `ClassCastException` at that line. Because this happened *before* `proceed()` was reached, **the underlying service method never executed at all**: a logging concern was capable of taking down login, registration, and background jobs entirely.
* The `@Pointcut` expression itself, `execution(* ..service.*.*(..)))`, had one extra trailing `)` — an unbalanced-parenthesis AspectJ expression that risks a pointcut parse failure at context startup — and used `..service` (singular), which would not match the project's actual `com.mts.aadati.services` (plural) package, meaning even a syntactically valid version of this pointcut would silently advise nothing.
* The advice unconditionally logged raw method arguments and return values (`Arrays.toString(args)`, `result`) for every service call, with no exceptions for methods that take or return DTOs/entities containing plaintext passwords, JWTs, or other PII (`AuthenticationService.register`/`login` being the clearest case) — a direct CWE-532 (insertion of sensitive information into a log file) risk.
* The aspect had no explicit `@Order` relative to the class-level `@Transactional` advice already present on every service, so nothing guaranteed it wrapped *outside* the transaction boundary — if it ended up wrapping inside instead, a successful "Exiting" log line could be written before a later commit-time failure, misrepresenting the actual outcome of the call.

### **Decision:**

* Rewrite `resolveCurrentUserId()` as a small, self-contained helper that never throws: it treats a missing/anonymous/non-`CustomUserDetails` principal as a normal, expected case (fallback value `"anonymous"`), not an error condition, and wraps its own body in a try/catch (fallback `"unknown"`) so no future change to this logic can propagate an exception into the advice itself.
* Fix the pointcut to a syntactically valid, fully-qualified expression matching the real package: `execution(* com.mts.aadati.services.*.*(..))`.
* Drop `args`/`result` from the logged output entirely. The advice now logs method signature, resolved user id, and timing only — enough for tracing without risking secrets in the log stream. Argument-level tracing, if ever genuinely needed, requires an explicit per-DTO allow-list/masking mechanism, not a blanket dump — this is deferred until an actual need appears (same "don't build it speculatively" test applied elsewhere in this project).
* Add `@Order(Ordered.HIGHEST_PRECEDENCE)` to `AopLogging` so it deterministically wraps *outside* the `@Transactional` advice — the logged "Exiting"/"Exception" outcome now reflects the actual committed result, not just the raw method return before commit/rollback.
* Pass the caught exception object itself (not just `e.getMessage()`) as the final SLF4J argument on the failure log line, so the stack trace is preserved for debugging.
* Document all of the above as standing rules for this aspect (and any future cross-cutting aspect) in `ENGINEERING_RULES.md` (new Section 10: AOP — Cross-Cutting Logging), so the same class of mistake doesn't get reintroduced later.

### **Why:**

* A cross-cutting concern that is explicitly scoped to "logging only" (per the earlier AOP activation decision) must not be able to alter or block the business outcome of the call it wraps — the aspect throwing before `joinPoint.proceed()` is reached is a direct violation of that scope, regardless of intent.
* The OWASP Logging Cheat Sheet explicitly warns against writing sensitive data (credentials, tokens, personal data) into application logs; unconditionally logging method arguments/return values on a pointcut that covers `AuthenticationService` violates this directly.
  **Source:** [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
* Spring AOP advisor ordering between two aspects (a custom `@Aspect` and the framework's transactional advisor) is not guaranteed unless at least one declares an explicit `@Order`; relying on default/declaration order for something whose correctness depends on wrapping outside a transaction boundary is fragile and was made explicit instead.
  **Source:** [Spring Framework Reference — Advice Ordering](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html#aop-ataspectj-advice-ordering)
* AspectJ `execution(..)` pointcut expressions require balanced parentheses and an exact package match to compile/match at all; an unbalanced expression is a startup risk, and a mismatched package silently produces zero coverage with no error — both are worth verifying explicitly rather than assuming the pointcut "just works" once it compiles.
  **Source:** [AspectJ Programming Guide — Pointcut Expressions](https://www.eclipse.org/aspectj/doc/released/progguide/semantics-pointcuts.html)

### **Impact:**

* `AopLogging` can no longer throw before the wrapped service method executes; unauthenticated/system-context calls (login, register, scheduled jobs) now log with a fallback user id (`"anonymous"`/`"unknown"`) instead of crashing.
* The pointcut now matches the real `com.mts.aadati.services` package with valid syntax; service methods are actually being advised, where before they may not have been at all.
* No method arguments or return values are logged by default anywhere in the application via this aspect, removing a live risk of plaintext credentials/tokens appearing in logs.
* The aspect's logged outcome for every service call now reflects the true post-commit result, since it's guaranteed to wrap outside `@Transactional`.
* Exception log lines now retain the full stack trace via SLF4J's throwable-argument convention, instead of just the exception's message string.
* `ENGINEERING_RULES.md` gains a new Section 10 (AOP — Cross-Cutting Logging) codifying these constraints for any future aspect work, so the same review doesn't need to happen from scratch next time.

---
