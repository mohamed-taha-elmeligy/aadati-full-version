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
* One `HabitCalendarRepository` query attempted to `JOIN FETCH` two `@OneToMany` collections (`habitCompletions` and `taskCompletions`) in the same query for a `COUNT`, which would produce an incorrect (multiplied) count due to the cartesian product created by fetch-joining two to-many collections simultaneously.

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
* One service caught a `ResourceNotFoundException`-equivalent internally (via a checked lookup failure) and silently returned an empty result instead of letting the error propagate — the same "swallow the error, return empty" anti-pattern previously identified and rejected in the SAS project's reactive controllers.
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

## [Adopt Redis Caching for the Service Layer — Entity Caching Boundaries, Not a DTO-Returning Service Layer]

### **Problem:**

* Introducing `@Cacheable` on service methods (`CachingConfig`, `CacheTtl`, `CacheNames`) raised a real question: services return Entities (Section 1), and a naive `RedisSerializer.json()` cache will serialize whatever the service returns — meaning a cached `User` would include its password hash, since caching happens inside the service layer, *before* any Controller-side DTO mapping ever gets a chance to strip sensitive fields. Unlike a normal API response, there was no existing "filter" between the raw entity and what actually leaves the service boundary once caching sits in front of it.
* The proposed fix under consideration was to have services return DTOs instead of Entities specifically to make caching safe — which would have reversed the foundational Layering rule (Section 1: "Services work with Entities, not DTOs... Service-to-service calls use Entities, never DTOs") for every service in the codebase, not just the ones that happen to be cached.
* Separately, caching a JPA entity with an uninitialized lazy `@ManyToOne`/`@OneToOne` relationship risks `LazyInitializationException` (or silently wrong data) when Jackson serializes it outside its persistence context; and an entity rehydrated from a cache hit is a detached POJO (built by Jackson via reflection), not a JPA-managed instance — mutating it does not get picked up by dirty-checking the way a live-session entity would.
* A `@CachePut`/`@CacheEvict` executed inside a `@Transactional` service method could write to Redis before the surrounding transaction actually commits, risking a cache entry for data that is later rolled back — the same class of "proxy ordering vs. transaction boundary" problem already identified and fixed for the `AopLogging` aspect.
* Fast-changing entities (`HabitCompletion`, `TaskCompletion` — daily completion toggles) were initially considered for caching alongside everything else, despite an unfavorable read/write ratio that would make caching a net cost (constant invalidation) rather than a benefit.
* A second alternative was raised while working through the DTO-vs-Entity question: inserting a project-wide `AppService` layer between every Controller and Service to own the Controller↔Service conversion (effectively relocating the mapping responsibility Section 1 already assigns to Controllers). This would have been a sweeping, all-controllers-at-once change made to solve a single-field problem, and would not, on its own, have solved the lazy-relationship serialization risk either — that risk exists regardless of which layer holds the mapping code, since it's caused by *when* (relative to the Hibernate session) serialization happens, not *what type* the caller-facing method returns.
* Once `@JsonIgnore` was chosen for `User.password`, a further consequence needed to be worked out: a `User` rehydrated from a cache hit will have `password = null`, since the field was never written into the cached JSON to begin with — any code path that needs the *real* password hash (credential verification during login, a future "verify current password" flow) would silently and permanently fail against a cached entity, with no exception to signal why.

### **Decision:**

* **Keep the Layering rule unchanged: services still return Entities, including from `@Cacheable` methods.** The sensitive-data risk is solved at the entity/serialization level instead — `@JsonIgnore` (or an equivalent field-level exclusion) is applied directly to fields that must never leave the service layer in any serialized form (starting with `User.password`), protecting every serialization path (cache, accidental future logging, etc.), not just Redis specifically. The proposed `AppService` layer is rejected for this problem specifically — it's the same "separate Converter layer" Section 1 already reasons through and reserves for genuinely complex reactive composition, which doesn't apply to this blocking-style project, and it wouldn't have addressed the lazy-relationship risk below regardless.
* **Any lookup path that needs the real value of a `@JsonIgnore`d field for its own logic is never `@Cacheable`, full stop** — it queries the repository directly. Concretely: whatever method(s) `AuthenticationService`/`CustomUserDetailsService` use to fetch a `User` for credential verification during login must bypass caching entirely, since a cache-hit `User` would carry `password = null` and silently fail every credential check after the first. This is evaluated per call site (does *this* call need the real value, or just the rest of the entity?), the same way `HabitCompletion`/`TaskCompletion` are excluded per-entity — here it's a correctness-driven exclusion rather than a churn-rate-driven one.
* **Only apply `@Cacheable` to queries whose entity graph is already fully, eagerly initialized** (i.e. the relevant relationships are already `JOIN FETCH`-ed per the existing Repository Rules) — never to an entity with an uninitialized lazy relationship. Callers that need to mutate a cache-hit entity must explicitly `repository.save(...)`, the same as any other detached JPA entity.
* **`CacheTtl` + `CacheNames` become the single source of truth for cache TTL policy and naming** — mirroring the already-established pattern of `PageableUtils` (pagination) and `Auditing` (timestamps): one centralized place, no ad-hoc `Duration`/string literals scattered through services. `CacheTtl` tiers TTLs by data volatility: reference/lookup data (`Role`, `TaskPriorityLevel`, `HabitDayWeek`/`HabitWeek`) gets long TTLs (12h/1h); standard entities (`User`, `Habit`, `HabitTask`, `HabitCalendar`) get shorter, tiered item/list/page TTLs (30m/15m/10m); computed data (`PercentageDay`, `PercentageWeek`) gets the shortest TTL (10m) since it's derived and freshness matters most there.
* **`HabitCompletion` and `TaskCompletion` are excluded from caching entirely**, applying the same "size infrastructure to the entity's actual growth/change rate" reasoning already used for cleanup-job retention (Section 7) — high-churn, write-heavy data doesn't benefit from caching; it just adds invalidation overhead for a near-zero hit rate.
* **`RedisCacheManager` is configured with `.transactionAware()`**, deferring the actual Redis write/evict until the enclosing Spring-managed transaction commits successfully — this holds at the `CacheManager` level regardless of advisor ordering between `@Transactional` and the cache interceptor, which is a stronger guarantee than the explicit `@Order` needed for the `AopLogging` aspect.
* **`CacheErrorHandler` logs and falls through on `GET`/`PUT` failures** (the call proceeds to the database, so the caller still gets a correct answer, just without the performance benefit), but **logs `EVICT`/`CLEAR` failures at a higher severity (`error`)**, since a failed eviction risks serving stale data for the rest of that key's TTL — a correctness-adjacent risk, not just a performance one — while still allowing the underlying write to succeed, since Aadati's short TTLs on volatile data bound the staleness window.

### **Why:**

* Reversing Section 1's Layering rule for the sake of one cross-cutting concern (caching) would have meant every service's contract became conditional on "is this method ever cached," reintroducing exactly the kind of layer-mixing inconsistency the project's refactoring process rules (Section 6: "one architectural layer at a time") were adopted to prevent. The actual risk (a sensitive field reaching an external store) is narrower than "services must never return entities" and has a narrower fix.
* `@JsonIgnore` at the field level is a defense-in-depth control: it protects the field from *any* serialization path the entity might ever cross (cache, an accidental future `log.info(user)`, a debugging endpoint), not just the one currently under discussion — consistent with the project's broader "no secrets ever leave the trust boundary" posture already applied to configuration (Section 7).
  **Source:** [Jackson Annotations — `@JsonIgnore`](https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations)
* Hibernate lazy proxies are tied to an open persistence context; serializing one after the session has closed (which is guaranteed to be the case by the time an object reaches a cache store) is a well-documented failure mode, and the correct mitigation is to only cache fully-initialized graphs rather than to add a Hibernate-aware serializer module to paper over uninitialized proxies.
  **Source:** [Hibernate ORM User Guide — Fetching and Lazy Loading](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching)
* `RedisCacheManager.RedisCacheManagerBuilder#transactionAware()` is Spring Data Redis's documented mechanism for deferring cache writes to align with transaction outcomes, specifically to prevent a cache from reflecting a write that a surrounding transaction later rolls back.
  **Source:** [Spring Data Redis Reference — Redis Cache / Transactions](https://docs.spring.io/spring-data-redis/reference/redis/redis-cache.html)
* Caching only pays off when reads meaningfully outnumber writes; a fast-accumulating, frequently-toggled entity has a read/write ratio that makes caching pure overhead — the same reasoning already applied when sizing `HabitCompletion`'s cleanup retention independently of other entities (Section 7).
* A cache-layer failure is not equivalent to a business-logic failure being swallowed (the "return empty on internal failure" anti-pattern rejected in Section 4): the database remains the authoritative source of truth and is still consulted on a cache miss/error, so the caller's result is still correct — only the performance optimization is lost, which is exactly what a `CacheErrorHandler` is meant to make survivable.
* Widening the change to a project-wide `AppService` layer for a single-field caching concern would have meant touching every existing Controller and Service at once, directly contradicting the project's own refactoring process rule of changing one architectural layer at a time (Section 6), and adding another proxied bean layer at exactly the moment two other proxy-ordering bugs (the `AopLogging` aspect, the `@Transactional`/cache-write ordering above) had just been found and fixed — more layers here meant more surface area for the same class of bug, not less.

### **Impact:**

* No service method's return type changed because of caching; Section 1's Layering rule holds without exception for this concern.
* `User.password` (and any future field with the same sensitivity) is excluded from serialization at the entity level, closing the caching leak without touching the service contract — and incidentally hardening every other serialization path in the application against the same class of leak.
* The credential-verification lookup path (login) is identified as permanently excluded from caching, before any `@Cacheable` annotation was actually placed anywhere near it — avoiding a silent, hard-to-diagnose "login stops working after the first successful attempt" defect that would only have shown up in testing/production, not at compile time.
* No project-wide `AppService`/Controller↔Service converter layer was introduced; Controllers keep doing DTO↔Entity mapping directly via injected MapStruct mappers, per Section 1, unchanged.
* `@Cacheable` is limited, by convention, to queries already returning a fully-initialized entity graph; this composes directly with the existing `JOIN FETCH`-only-when-read repository discipline instead of requiring new infrastructure (e.g. a Hibernate-aware Jackson module).
* `CacheTtl`/`CacheNames` give the project a single, auditable place to see and adjust every cache's TTL and naming, matching the `PageableUtils`/`Auditing` pattern already established for other cross-cutting defaults.
* `HabitCompletion` and `TaskCompletion` remain entirely uncached, consistent with their existing short cleanup-retention treatment — both decisions stem from the same underlying fact about these entities' write-heavy nature.
* A rolled-back transaction can no longer leave a stale/incorrect entry in the cache, since `.transactionAware()` defers the write until commit, independent of interceptor ordering.
* Redis outages degrade the application to "no caching benefit," not "wrong data" or "broken requests" — `GET`/`PUT` failures are quiet fallbacks to the database, while `EVICT`/`CLEAR` failures are surfaced loudly (`error`) precisely because they're the one failure mode that can leave *incorrect* (stale) data being served, bounded by each cache's TTL.
* `ENGINEERING_RULES.md` gains a new Section 11 (Caching) codifying all of the above, so the next entity added to caching is evaluated with the same checklist (sensitive fields, lazy relationships, read/write ratio, TTL tier) instead of re-deriving it from scratch.

---

## [Fix — Cache Key/Scope Collision and Missing List-Cache Eviction in `TaskPriorityLevelService`]

### **Problem:**

* `findPriorityLevel(int)` (returns any priority level, deleted or not) and `findActivePriorityLevel(int)` (returns only non-deleted ones) were both `@Cacheable` under the same default cache (`CacheNames.PRIORITY_LEVEL`, from the class-level `@CacheConfig`) using the identical literal key `#priorityLevel`. Spring's cache abstraction resolves a hit purely by `(cache name, key)` — it has no awareness of which query produced the cached value — so a call to one method could silently be served the other's cached result for the same `priorityLevel` value.
* This collision had a concrete correctness consequence in `toggleDeleted(UUID)`: its `@CachePut(key = "#result.priorityLevel")` wrote the freshly toggled entity into that same shared slot regardless of the resulting `isDeleted` value. A priority level just soft-deleted via `toggleDeleted` would still be returned by `findActivePriorityLevel` for the rest of that key's TTL, even though a fresh call to `findByPriorityLevelAndIsDeletedFalse` would correctly find nothing. This is the same defect shape as the still-open `isEmailActive()` cache-namespace-mixing finding from `UserService` — a raw business key reused as a cache key across methods with different result-set scopes.
* Separately, `addPriority(TaskPriorityLevel)` performed no `@CacheEvict` at all, while `updatePriority`/`toggleDeleted` both evicted the `LIST_SUFFIX` cache. A newly added priority level would not appear in any cached list/color endpoint (`findPriorityLevelDesc`, `getAllColor`, `findActivePriorityLevelAsc`/`Desc`, `getActiveAllColor`) until that cache's TTL expired.
* Minor inconsistency: `findPriorityLevelAsc()` had no `@Cacheable` at all while its `Desc` counterpart did, with no stated reason for the asymmetry.

### **Decision:**

* Give the "active-only" scope its own key namespace within the same cache: `findActivePriorityLevel` now keys as `'active:' + #priorityLevel`, leaving `findPriorityLevel`'s existing `#priorityLevel` key to represent the "any" scope. Same cache (`CacheNames.PRIORITY_LEVEL`), disjoint keys.
* `toggleDeleted` now maintains both scopes explicitly and conditionally, evaluated against `#result` after the entity is saved:
  * always `@CachePut(key = "#result.priorityLevel")` — the "any" scope reflects the entity's current state regardless of its deleted flag.
  * `@CachePut(key = "'active:' + #result.priorityLevel", condition = "!#result.deleted")` — only refreshes the active-scope slot when the entity is actually still active.
  * `@CacheEvict(key = "'active:' + #result.priorityLevel", condition = "#result.deleted")` — removes the active-scope slot when the toggle just soft-deleted the record, instead of leaving a stale "active" entry behind.
* `addPriority` now carries `@CacheEvict(value = CacheNames.PRIORITY_LEVEL + CacheNames.LIST_SUFFIX, allEntries = true)`, matching the eviction already done by `updatePriority`/`toggleDeleted`.
* `findPriorityLevelAsc()` is now `@Cacheable` under `LIST_SUFFIX` keyed by `#root.methodName`, identical in shape to `findPriorityLevelDesc()`.
* `updatePriority`'s existing `allEntries = true` full-cache wipe (which clears both the "any" and every `'active:'`-prefixed key at once) is kept as-is: since `priorityLevel` is itself the mutable field the cache is keyed on, there is no way to compute a stale key's identity after the entity has already changed, so a full wipe is the correct — and, given how infrequently this reference table changes, cheap — way to guarantee no orphaned entry survives an update.

### **Why:**

* Spring's declarative caching resolves a cache hit purely by `(cache name, key)`; it has no visibility into which repository method/query produced the value the first time. Two methods that read different subsets of the same table must be given different keys (or different caches) whenever their result sets can disagree for the same parameter value, or a hit meant for one method's contract can be silently handed to the other's caller.
  **Source:** [Spring Framework Reference — Cache Abstraction, Declarative Annotation-based Caching](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
* For `@CachePut`/`@CacheEvict`, the target method has already run by the time these operations are applied — unlike `@Cacheable`'s `condition`, which is evaluated *before* invocation — so their `condition`/`key` SpEL expressions can safely reference `#result`. This is what allows a single write path to correctly route the outcome to the right scope-specific slot, instead of needing branching imperative cache-management code inside the service method itself.
  **Source:** [Spring Framework Reference — Cache Abstraction, Conditional Caching](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html#cache-annotations-put-condition)
* This is the same underlying principle Section 11's `@JsonIgnore`/credential-lookup bullet already established for a different trigger (a field silently returning `null` on a cache hit): before caching a method, identify exactly what contract a cache hit must honor, not just what parameter it happens to be keyed on.

### **Impact:**

* `findActivePriorityLevel` can no longer return a stale/incorrect result inherited from `findPriorityLevel`'s cache slot, or vice versa — each scope now has its own key namespace.
* A priority level toggled to soft-deleted immediately stops being served by `findActivePriorityLevel`'s cache, instead of remaining visible for up to that cache's TTL.
* Newly added priority levels now appear in list/color endpoints immediately after creation instead of waiting out `LIST_SUFFIX`'s TTL.
* `findPriorityLevelAsc()`/`findPriorityLevelDesc()` are now symmetric in caching behavior.
* `ENGINEERING_RULES.md` Section 11 gains a new bullet generalizing this "scope-aware cache key" rule, so the next entity with an "any" vs. "active-only" (or similarly scoped) pair of read methods is caught at design time instead of by a later production bug. The still-open `isEmailActive()` finding in `UserService` is the same defect class and should be fixed the same way once addressed.

---

## [Confirm — `Role.name` as a Mutable Natural Key via `@NaturalId(mutable = true)`]

### **Problem:**

* `Role.name` is a unique, updatable field (`existsByNameAndRoleIdNot`, used by `updateRole` to enforce uniqueness on rename) — fundamentally different in kind from the surrogate `roleId` (`@Id @GeneratedValue(strategy = GenerationType.UUID)`), which is a pure database identity with no business meaning of its own. Nothing in the mapping distinguished "the field the database identifies rows by" from "the field the business actually identifies a `Role` by," and no existing rule captured this for any entity sharing the same shape (a unique, updatable identifying field alongside a surrogate `@Id`).

### **Decision:**

* Confirmed `Role.name` is annotated `@NaturalId(mutable = true)`, alongside its pre-existing `@Column(unique = true)`.
* Generalized the finding into a new rule: any field enforced unique via an `existsByFieldAndIdNot(value, id)`-style update-time check (a mutable business key, per the existing Repository Rules update-exclusion bullet) is annotated `@NaturalId(mutable = true)` on the entity. This applies going forward to every entity with the same shape — starting with `TaskPriorityLevel.priorityLevel`, confirmed via `TaskPriorityLevelService` to have the identical `existsByPriorityLevelAndTaskPriorityLevelIdNot` pattern, and `HabitCategory`, which still needs its entity mapping checked.

### **Why:**

* `@NaturalId` is Hibernate's dedicated mechanism for marking a business/natural key distinct from a surrogate `@Id`, and enables `Session.byNaturalId()`-style lookups without weakening or replacing the existing DB-level uniqueness constraint — `@Column(unique = true)` still does the actual constraint enforcement, since `@NaturalId` alone does not generate one.
  **Source:** [Hibernate ORM User Guide — Natural Ids](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#naturalid)
* The `mutable = true` flag matters specifically because this project's identifying fields are legitimately editable through their own `update()` service methods (`updateRole`, `updatePriority`) — omitting it would leave Hibernate assuming the natural id, once set, never changes, which is factually wrong for this domain and would cause Hibernate's natural-id tracking to miss a change if natural-id caching is ever adopted later.
* This makes explicit, at the entity level, a distinction that was previously only implicit in the uniqueness-exclusion repository pattern (Section 2): "how the database identifies a row" (`@Id`, an implementation detail, never business-meaningful, never editable) versus "how the business identifies the record" (the natural id — meaningful, and here, editable).

### **Impact:**

* `Role.name` is correctly documented on the entity itself as a mutable natural key, closing this finding for `Role`.
* `ENGINEERING_RULES.md` Section 2 gains a rule tying every `existsByFieldAndIdNot`-style update-exclusion method to a required `@NaturalId(mutable = true)` annotation on the corresponding entity field, so the same audit doesn't need to be re-derived per entity.
* `TaskPriorityLevel.priorityLevel` is flagged as the next entity to verify against this rule (its service-layer shape already matches); `HabitCategory`'s mapping still needs to be reviewed. This entry documents the rule and the `Role` confirmation only — the sibling-entity audit remains open.

---

## [Fix — `Page<T>` Cannot Be Deserialized From Redis on a Cache Hit (`PercentageWeekService`)]

### **Problem:**

* `PercentageWeekService` had 9 `@Cacheable` methods returning `Page<PercentageWeek>` directly from repository pagination queries. `Page`'s concrete runtime implementation, Spring Data's `PageImpl`, has no default constructor and no Jackson `@JsonCreator` — so the first call (cache miss) serializes fine into Redis, but every subsequent call for the same key (cache hit) throws `InvalidDefinitionException: Cannot construct instance of org.springframework.data.domain.PageImpl (no Creators, like default constructor, exist)` while Jackson deserializes the cached JSON back into an object. This is a widely-documented Spring Data + Jackson + Redis pitfall, not specific to this project.
* A first attempt wrapped the return value in a `CacheablePage<T> extends PageImpl<T>` subclass with its own `@JsonCreator` constructor. Two mistakes surfaced in review: (a) binding the constructor's `pageable` parameter directly to a `Pageable`/`PageRequest` JSON object reintroduced the identical "no Creators" failure one level deeper, since `PageRequest`/`Sort` have the same construction problem as `PageImpl` itself; (b) `@JsonProperty("total")` didn't match the actual serialized field name (`totalElements`), so it would have silently deserialized as `0` instead of throwing — corrupting pagination metadata with no visible error.
* A follow-up design question was whether the Service could instead return a custom `PageResponse`/`PageModel` type — not extending `PageImpl` at all, and reusable at both the Service layer (caching Entities) and the Controller layer (API response, after MapStruct-mapping content to DTOs) — without violating Section 1's "Services return Entities" rule. Investigating this surfaced a second, independent problem: this project's `RedisCacheManager` builds its value serializer via `RedisSerializer.json()`, which is `GenericJackson2JsonRedisSerializer`'s no-arg constructor — confirmed, via Spring Data Redis's own source, to call `mapper.enableDefaultTyping(DefaultTyping.NON_FINAL, As.PROPERTY)`. `DefaultTyping.NON_FINAL` only embeds a `@class` type hint for non-final classes, and a Java `record` is implicitly `final` — so a record-based wrapper would silently lose its type hint on write, then deserialize as a generic `Object`/`LinkedHashMap` on the next cache hit, reproducing the exact same failure class the fix was meant to solve.

### **Decision:**

* Introduced `PageModel<T>` (`com.mts.aadati.dto.response`): a plain, **non-final class** (deliberately not a record), with fields `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `hasNext`; an explicit `@JsonCreator` constructor with `@JsonProperty`-matched parameter names; a `static <T> PageModel<T> from(Page<T> page)` factory for wrapping a repository result; and a `<R> PageModel<R> map(Function<T,R> mapper)` instance method for the Controller's later Entity→DTO conversion step.
* All 9 `@Cacheable` paginated methods in `PercentageWeekService` now declare `PageModel<PercentageWeek>` as their return type and wrap the repository's `Page<PercentageWeek>` result via `PageModel.from(...)` before returning.
* `findByUserAndHabitWeek` (the single-item `@Cacheable` lookup in the same service) had its missing `sync = true` added, bringing it in line with every other `@Cacheable` method in the class.
* A cosmetic identifier typo, `PERSENTAGE_WEEK_NOT_FOUND` → `PERCENTAGE_WEEK_NOT_FOUND`, was corrected in the same pass (the string value was already spelled correctly; only the Java constant name was misspelled).
* New rules captured: Section 8 gains an explicit, sourced exception to the records-preference for any type round-tripped through this project's Redis cache; Section 11 gains a rule requiring a Jackson-constructible wrapper (never a raw `Page<T>`) for any `@Cacheable` paginated method, with `PageModel` as the canonical example.

### **Why:**

* `PageImpl`'s lack of a Jackson-usable constructor, and the resulting `InvalidDefinitionException` on deserialization, is a long-documented Spring Data + Jackson interaction, confirmed independently across multiple real-world reports.
  **Source:** [Stack Overflow — Error during Deserialization of PageImpl](https://stackoverflow.com/questions/55965523/error-during-deserialization-of-pageimpl-cannot-construct-instance-of-org-spr)
* `GenericJackson2JsonRedisSerializer`'s no-arg constructor (what `RedisSerializer.json()` delegates to) unconditionally activates `DefaultTyping.NON_FINAL` — confirmed directly from Spring Data Redis's own source in a maintainer discussion, not inferred.
  **Source:** [spring-data-redis Issue #2470](https://github.com/spring-projects/spring-data-redis/issues/2470)
* `DefaultTyping.NON_FINAL` deliberately excludes final classes from type-hint embedding (the policy assumes a final class's declared type is already unambiguous) — which is exactly backwards for `GenericJackson2JsonRedisSerializer`'s own deserialization path, since it reads into `Object.class` and relies entirely on the embedded hint to know what to build, regardless of the class's finality.
* Solving this by introducing `PageModel` — rather than trying to coerce `PageImpl` itself into being deserializable, or reversing Section 1 to have the Service return a DTO-shaped page — keeps the fix scoped to serialization/infrastructure, consistent with how the `User.password` caching risk was solved earlier in this project: change what gets serialized, not the Service's layering contract. `PageModel<PercentageWeek>` still carries real Entities in its `content`; nothing about Section 1 is renegotiated.

### **Impact:**

* All 9 paginated `@Cacheable` methods in `PercentageWeekService` now survive a real cache hit instead of throwing on the second identical call — this was a live, reproducible defect, not a hypothetical.
* `PageModel<T>` is reusable across any other current or future service with the same shape (a paginated, cached finder) and, via `.map()`, at the Controller layer for the final Entity→Response-DTO conversion — one class serving both roles, avoiding a second parallel pagination-wrapper type.
* `ENGINEERING_RULES.md` Section 8 now documents a concrete, sourced exception to the records-preference, so a future contributor doesn't default to a `record` for a cached type and silently reintroduce this exact failure mode.
* `ENGINEERING_RULES.md` Section 11 now flags `Page<T>` as never directly cacheable, project-wide — any other service with a paginated `@Cacheable` finder (not just `PercentageWeekService`) needs the same audit.
* `findByUserAndHabitWeek` and the `PERCENTAGE_WEEK_NOT_FOUND` constant name are now consistent with the rest of the class.
* Still open, not part of this entry: whether `PercentageWeek.rate` should be guarded by a domain method like `PercentageDay.updateRate()` instead of the plain setter `updatePercentageWeek` currently calls; and whether the existing `allEntries = true` full-cache-wipe eviction strategy on `PercentageWeek`'s per-user, potentially high-cardinality data needs a more surgical, user-scoped eviction approach at greater scale.

---

## [Fix — `isEmailActive()` Cached Existence Guard Silently Bypassed After the First Call]

### **Problem:**

* `UserService.isEmailActive(String email)` was `@Cacheable(key = "#email")` under the default `CacheNames.USER` cache, but its body performs an existence guard before returning the actual boolean: `if (!userRepository.existsByEmail(email)) throw new ResourceNotFoundException(...)`. Because `@Cacheable` only invokes the method body on a cache miss, this guard — and therefore the entire "does this email even belong to an existing user" check — only ever ran once per key's TTL window; every subsequent call for the same email was served the cached boolean directly, with the guard never re-evaluated.
* Concretely: no write path in `UserService` evicted this entry when the underlying user was deleted or its active/verification status changed. `removeUserById(userId)` only evicts the `#userId`-keyed cache entry shared by the `getUser`/`updateUser` triangle — never the separate `#email`-keyed `isEmailActive` entry for that same user. So a user deleted after their email's status was cached would still have `isEmailActive(email)` silently return the last cached boolean instead of throwing `ResourceNotFoundException`, for as long as that entry's TTL lasted — an existence check that stopped reflecting reality.
* An earlier hypothesis in this same review thread — that the risk was a cache-key/namespace collision between `isEmailActive` (keyed by email) and the other `User`-cache methods (`getUser`/`updateUser`, keyed by `UUID`) — did not hold once the real code was reviewed: different key types (`String` vs. `UUID`) never collide in practice, and `getUser`/`updateUser`/`removeUserById` already form a correctly-synchronized ID-keyed CRUD cache triangle that needed no change. The actual defect was unrelated to key collision — a validation/guard clause that stops re-running once its result is cached.

### **Decision:**

* Remove `@Cacheable` from `isEmailActive(String email)` entirely. It now queries the repository directly on every call, so `existsByEmail`'s guard — and the underlying active-status read — always reflects the current database state.
* No replacement caching strategy (re-keying, a shorter TTL, etc.) was adopted for this method. It joins the existing precedent of excluding correctness/security-relevant lookups from caching altogether (credential verification for login, `hasRole()`/`getRolesByUserId()`), rather than keeping it cached with more careful invalidation — the call volume for an email-activation check (registration/verification-time, not a per-request hot path) doesn't justify the added invalidation surface.
* `getUser`, `updateUser`, and `removeUserById`'s existing ID-keyed cache triangle was reviewed in the same pass and confirmed correct as-is; no change needed there.

### **Why:**

* `@Cacheable` only invokes the underlying method on a cache miss; any exception-throwing guard clause inside that method is therefore also skipped on every cache hit. A guard whose entire purpose is "confirm this still exists / is still valid" is meaningless once it can silently stop running for the rest of a key's TTL — a stronger failure mode than ordinary staleness (a slightly-out-of-date value), because it hides the fact that the record's existence itself changed.
  **Source:** [Spring Framework Reference — Cache Abstraction, `@Cacheable` Semantics](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
* This extends the same principle already behind the `@JsonIgnore`d-field caching exclusion (a call site that needs a real, current value must not be cached) and the roles-caching removal (an authorization decision must not be served from a snapshot) — to cover any exception-based existence/validity guard, not just a field going `null` on a hit.

### **Impact:**

* `isEmailActive(email)` now always reflects the current database state, including correctly throwing `ResourceNotFoundException` for an email that no longer belongs to any user, regardless of what was previously cached for that key.
* No functional change to `getUser`/`updateUser`/`removeUserById`'s existing ID-keyed caching, reviewed and confirmed correct in the same pass.
* `ENGINEERING_RULES.md` Section 11 gains a new bullet generalizing "never `@Cacheable` a method whose body contains an exception-throwing existence/validity guard," closing the long-open `isEmailActive()` finding with a diagnosis distinct from — and more precise than — the cache-namespace-collision hypothesis originally suspected for it.

---

## [Improve — Targeted `@CachePut` Replaces Full-Cache Eviction on `updatePercentageDay`/`updatePercentageWeek`]

### **Problem:**

* `updatePercentageDay`/`updatePercentageWeek` are partial-update methods (Section 1's exception): each only ever mutates `rate` on an already-loaded entity, never the `user`/`habitCalendar`/`habitWeek` relationships that identify it. Despite that, both methods originally invalidated their entire base single-item cache (`@CacheEvict(value = CacheNames.PERCENTAGE_DAY, allEntries = true)` / same for `PERCENTAGE_WEEK`) on every single update — wiping every *other* user's and every *other* record's cached entry too, not just the one that actually changed.
* This is the same "full-wipe blast radius on a per-user, potentially high-cardinality dataset" risk flagged earlier as an open architectural question for `PercentageWeek`/`PercentageDay` caching (unlike small reference tables such as `Role`/`TaskPriorityLevel`, where a full wipe touches a handful of entries at most, `PercentageDay`/`PercentageWeek` scale with active users × tracked habits, so a full wipe on every single-field update is a comparatively expensive, avoidable cost).

### **Decision:**

* Both single-item base caches (`CacheNames.PERCENTAGE_DAY`, `CacheNames.PERCENTAGE_WEEK`) are now refreshed with a targeted `@CachePut` instead of evicted wholesale:
  ```java
  @Caching(
          put = @CachePut(
                  value = CacheNames.PERCENTAGE_DAY,
                  key = "#result.user.userId + '-' + #result.habitCalendar.habitCalendarId"
          ),
          evict = {
                  @CacheEvict(value = CacheNames.PERCENTAGE_DAY + CacheNames.PAGE_SUFFIX, allEntries = true),
                  @CacheEvict(value = CacheNames.PERCENTAGE_DAY + CacheNames.LIST_SUFFIX, allEntries = true)
          }
  )
  public PercentageDay updatePercentageDay(UUID percentageDayId, PercentageDay percentageDay) {
      PercentageDay percentage = getPercentageDayOrThrow(percentageDayId);
      percentage.updateRate(percentageDay.getRate());
      return repository.save(percentage);
  }
  ```
  The key is read off `#result` — the entity that was just loaded and saved — rather than off the incoming `percentageDay` parameter, because that parameter is a partial-update carrier that may not reliably carry the same `user`/`habitCalendar` associations as the persisted record.
* `PercentageWeekService.updatePercentageWeek` received the identical treatment (`key = "#result.user.userId + '-' + #result.habitWeek.weekId"`), since the same precondition holds there: `user`/`habitWeek` are never touched by that update either.
* The `PAGE_SUFFIX`/`LIST_SUFFIX` caches on both services are still fully evicted (`allEntries = true`) on every update, unchanged — a composite page/list result can legitimately contain the just-updated record with its now-stale `rate`, and there is no single well-known key to target for a composite result the way there is for a single-item lookup.
* This targeted-put approach is *not* adopted as a blanket replacement for full eviction everywhere. It only applies because `rate` — the only field these two update methods touch — is provably disjoint from the fields the cache key is built on. A method that lets a key's own field change (e.g. `TaskPriorityLevelService` renaming `priorityLevel`, which that cache is keyed on) still needs full eviction of the base cache, since a targeted put there would refresh the new key while leaving a stale entry parked under the old key for the rest of its TTL.

### **Why:**

* `@CachePut` always executes the annotated method and then writes its result into the cache under the computed key, updating exactly one entry — as opposed to `@CacheEvict(allEntries = true)`, which discards the entire cache region regardless of how many entries the write actually affected.
  **Source:** [Spring Framework Reference — Cache Abstraction, `@CachePut`](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
* Because `@CachePut`'s `key` SpEL is evaluated *after* the method runs, it can safely reference `#result` — the actual persisted entity — which is what makes it safe to key off `user`/`habitCalendar`/`habitWeek` here without needing those fields to come from the (possibly incomplete) input parameter.
* Reusing the exact key expression shape already used by each service's own `@Cacheable` single-item lookup (`findByUserAndHabitCalendar` / `findByUserAndHabitWeek`) guarantees the `@CachePut` writes to the same slot a subsequent cache-hit read will look up — a targeted put with a mismatched key would silently fail to update the entry callers actually read, while leaving the stale one live for its full TTL.

### **Impact:**

* An update to one user's one `PercentageDay`/`PercentageWeek` record no longer evicts every other user's and every other record's cached single-item entry — the base cache's hit rate under concurrent multi-user write traffic improves accordingly, with no correctness trade-off (the one entry that could have gone stale is the one that gets refreshed).
* `ENGINEERING_RULES.md` Section 11 gains a new bullet generalizing when a targeted `@CachePut` is safe versus when full `@CacheEvict(allEntries = true)` is still required, using this pair of services as the reference implementation and `TaskPriorityLevelService` as the contrasting counter-example.
* Still open, unrelated to this entry: whether `PercentageWeek.rate` should go through a domain-guarded method (`updateRate()`, as `PercentageDay` already does) instead of the plain `setRate()` currently called by `updatePercentageWeek` — pending review of `PercentageWeek.java`; and `PercentageDayService.findByUserAndHabitWeek(UUID userId, HabitWeek habitWeek)` still takes the full `HabitWeek` entity instead of a `UUID habitWeekId`, an unresolved Section 2 inconsistency inside this same file, not yet corrected.

---

## [Fix — Broken Cache-Key SpEL (`#methodName`), Cache-Name Collision Risk, and Self-Correction of the Existence-Guard Rule (`HabitWeekService`)]

### **Problem:**

* All seven `@Cacheable` methods in `HabitWeekService` (`findByYear`, `findByStartWeekBetween`, `findByEndWeekBetween`, `findAllByStartWeekAsc`, `findTopByCreatedAtDesc`, `findFirst`, `findHabitCalendarsByWeekId`) built their cache key using `#methodName` instead of `#root.methodName`. In Spring's cache SpEL, a `#name` expression resolves against the evaluation context's registered variables (method parameters, `#result`, `#root`) — there is no variable named `methodName` registered by default, only the root object's `methodName` *property*, reachable as `#root.methodName` (or the unqualified `methodName`, but never `#methodName`). Every call to any of these seven methods would throw a `SpelEvaluationException` (`"variable 'methodName' cannot be found"`) at the point the key is evaluated — a runtime crash on first invocation, not a silent staleness bug.
* Beyond the crash itself, `#root.methodName` is load-bearing here for a second reason: `findByStartWeekBetween` and `findByEndWeekBetween` share the exact same default cache (`CacheNames.WEEK + CacheNames.PAGE_SUFFIX`, from the class-level `@CacheConfig`, neither overrides `value =`) and build an otherwise **identical-shaped key** (`#start + '-' + #end + '-' + #pageNumber + '-' + <methodName>`) from the same parameter types. Without a correctly-resolving method-name component, the two methods would silently serve each other's cached pages for the same `(start, end, pageNumber)` input — the exact key/scope collision Section 11 already warns about.
* Separately, `HabitWeekService` names its cache `CacheNames.WEEK`, not `CacheNames.HABIT_WEEK` — the literal counter-example Section 11 already cites by name ("named after the entity they represent, unambiguously — `HABIT_WEEK`, not `WEEK` — to avoid visual collision with `PERCENTAGE_WEEK`"). This is a direct violation of an already-agreed rule, not a new finding.
* Reviewing `findByWeekNumberAndYear`/`findFirst` (both cached `Optional...orElseThrow` finders) against Section 11's existence-guard rule as it was worded surfaced a documentation defect in the rule itself: the rule as written ("Never `@Cacheable` a method whose body performs an existence/validity guard that throws") is broad enough to also condemn `findByWeekNumberAndYear`, `findFirst`, and `PercentageDayService`/`PercentageWeekService`'s `findByUserAndHabitCalendar`/`findByUserAndHabitWeek` — all of which were already reviewed and approved as correct in earlier entries. The rule's wording didn't capture the actual distinguishing factor.

### **Decision:**

* Replaced every `#methodName` with `#root.methodName` across all seven `@Cacheable` methods in `HabitWeekService`.
* Renamed the cache constant from `CacheNames.WEEK` to `CacheNames.HABIT_WEEK` and updated every reference in `HabitWeekService` (`@CacheConfig`, and the `value =` overrides in `findByWeekNumberAndYear`, `findFirst`, `findHabitCalendarsByWeekId`, and the evictions in `addWeek`/`cleanupOldWeeks`).
* `findFirst()`'s hardcoded `"HabitWeek not found"` string was replaced with the existing `HABIT_WEEK_NOT_FOUND` constant, for consistency with every other exception message in the class.
* Rewrote Section 11's existence-guard bullet to state the real test precisely: a cached find-or-404 lookup is safe as long as its record's only deletion/mutation path in this service carries a matching eviction for that same cache key; it is unsafe only when no write path in the service evicts that specific key (the actual `isEmailActive` defect), or when the lookup is evaluated against state mutable from outside the service entirely (credential checks, role lookups), where no in-service eviction could ever cover it.
* Two findings from this same review were **not** resolved in this pass and remain open, pending more information: (1) `findHabitCalendarsByWeekId` returns `HabitCalendar` entities cached under `HabitWeekService`'s own cache namespace, meaning `HabitCalendarService`'s own write paths (which it does not yet know about) would need to evict a cache name that isn't theirs — a cross-service eviction-ownership gap; (2) whether `cleanupOldWeeks`'s 20-year retention window is safe against any `HabitCalendar`/`PercentageWeek` row still holding a foreign key to a `HabitWeek` about to be deleted.

### **Why:**

* Spring's cache SpEL evaluation context only resolves `#name` against explicitly registered variables (method parameters by name, plus `#result` for `@CachePut`/`@CacheEvict`, plus the reserved `#root`); the method name is a **property of the root object**, not a registered variable, so it must be reached via `#root.methodName`.
  **Source:** [Spring Framework Reference — Cache Abstraction, SpEL Context](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
* This is the same key-uniqueness concern Section 11 already documents for scope-based collisions (`findX(id)` vs `findActiveX(id)`) — here the collision risk is between two methods with the same parameter shape sharing the same default cache, which `#root.methodName` is specifically what disambiguates.
* The existence-guard rule's rewrite keeps the actual precedent (`isEmailActive`) intact while removing an unintended, overly broad reading that would have contradicted several already-approved finders reviewed earlier in this same project — a documentation-correctness fix, not a reversal of the original decision.

### **Impact:**

* All seven previously-broken `@Cacheable` methods in `HabitWeekService` now evaluate their keys correctly instead of throwing on every call — this was a build-passes-but-crashes-at-runtime defect, not a hypothetical.
* `findByStartWeekBetween`/`findByEndWeekBetween` no longer risk serving each other's cached results for matching `(start, end, pageNumber)` inputs.
* `HabitWeekService`'s cache is now correctly namespaced as `HABIT_WEEK`, consistent with the convention Section 11 already mandated project-wide.
* `ENGINEERING_RULES.md` Section 11's existence-guard rule now accurately reflects what has actually been approved throughout this review, preventing a future contributor from reading it too literally and incorrectly flagging (or un-caching) a perfectly safe finder.
* Still open, not part of this entry: the `findHabitCalendarsByWeekId` cross-service cache-ownership question, and the `cleanupOldWeeks` 20-year/FK-safety question — both pending more source before a fix can be proposed.

---

## [Caching Rollout Across the Habit-Domain Services — `HabitTaskService`, `HabitService`, `HabitDayWeekService`, `HabitCategoryService`, `HabitCalendarService`, and a Retroactive Fix to `HabitWeekService`]

### **Problem:**

* Before caching could even be meaningfully applied to `HabitTaskService`, two pre-existing business-logic bugs surfaced during the design work: `updateHabitTask` checked `existsByUser_UserIdAndTitle` against the entity's **own current, not-yet-updated** title — meaning it always found "a task with this title" (itself) and threw `DuplicateResourceException` on every single call, regardless of input. And `toggleActive`'s underlying `getHabitTaskOrThrow` hardcoded `...AndIsActiveTrue`, so once a task was deactivated it became permanently unreachable by that same lookup — making `toggleActive` a one-way "deactivate-only" operation, not a real toggle, and also blocking `update`/`delete` on any deactivated task. The same one-way-toggle shape (`getHabitOrThrow`/`getCategoryOrThrow` filtering on an active/deleted flag) needed checking across every sibling service before caching a lookup built on top of it.
* Designing `HabitDayWeekService.findHabitsByDayAndUser` (which returns `Habit` entities, a different aggregate than `HabitDayWeek`) reproduced the same cross-service cache-ownership gap already flagged as open in `HabitWeekService.findHabitCalendarsByWeekId`: a service cannot know to evict a cache it doesn't own.
* Designing `HabitCategoryService`'s caching surfaced two cases the earlier `PercentageDay`/`PercentageWeek` targeted-`@CachePut` pattern does **not** safely generalize to: (1) `updateCategory` can change `name`, which is itself the base cache's key field (unlike `rate`, which was provably disjoint from its cache's key) — a targeted put there would leave a stale entry parked under the old name's key; (2) `toggleDeleted` moves a category between the "all" and "active-only" scopes that `findByName`/`findActiveByName` separately represent, and `@CachePut` cannot express "this key should now resolve to not-found," only "here is a new value."
* Designing `HabitCalendarService.findFirstByDate()`'s caching surfaced a defect already latent, unnoticed, in the earlier `HabitWeekService.findFirst()` work: `addWeek`'s eviction only covered `LIST_SUFFIX`/`PAGE_SUFFIX`, never the base cache — but `findFirst()`/`findFirstByDate()` (`findFirstByOrderBy...Desc()`) can have its answer changed by **any** insert, not just one matching an existing key, unlike an ordinary point-lookup (`findByWeekNumberAndYear`, `findByDate`) whose cached entries are unaffected by unrelated new rows.
* `HabitCalendarService.countDaysCompletedHabitsByWeek`/`countDaysCompletedTasksByWeek` are derived from `HabitCompletion`/`TaskCompletion` state — the two entities Section 11 already excludes from caching entirely for being high-churn.

### **Decision:**

* Fixed `HabitTaskService.updateHabitTask` to check the **incoming** `request.title()` against `existsByUser_UserIdAndTitleAndHabitTaskIdNot` (excluding the record itself), and removed the `IsActiveTrue` filter from `getHabitTaskOrThrow`, making deactivation reversible. The equivalent already-correct shape (`request.title()`/`AndHabitIdNot`, an unfiltered `getHabitOrThrow`/`getCategoryOrThrow`) was confirmed present in `HabitService` and `HabitCategoryService` as each was reviewed.
* Applied `PageModel<T>` to every `@Cacheable` method returning `Page<T>` across all five services, per the standing Section 11 rule.
* Applied the targeted-`@CachePut` pattern (Section 11) to `HabitTaskService.updateHabitTask`/`toggleActive` and `HabitService.updateHabit`/`toggleActive`, since neither service's base-cache key fields (`user`/`habitTaskId` or `user`/`habitId`) are touched by those updates, and — after the `IsActiveTrue` filter removal — the base lookup is now active-state-agnostic, making a toggle safely representable as a put rather than requiring a full evict.
* **New principle — cache name follows the entity, not the declaring service class:** `HabitDayWeekService.findHabitsByDayAndUser` is cached under `CacheNames.HABIT + CacheNames.PAGE_SUFFIX` (the cache `HabitService` itself writes to and evicts), not a `HabitDayWeek`-prefixed name — so `HabitService`'s own `add`/`update`/`delete`/`toggle` eviction set naturally covers it with no new coupling code required in either service. This is the concrete resolution pattern for the still-open `HabitWeekService.findHabitCalendarsByWeekId` question (not yet applied there — deferred, see Backlog).
* `HabitCategoryService.updateCategory`/`toggleDeleted` use a full `@CacheEvict(allEntries = true)` on the base cache (not a targeted put), because `name` is a key field for `updateCategory`, and `toggleDeleted` moves a record between two scopes (`findByName` vs. `findActiveByName`) that a single put cannot represent.
* `HabitCalendarService.addCalendar` evicts the entire base cache (`allEntries = true`), not just `LIST_SUFFIX`/`PAGE_SUFFIX`, specifically so `findFirstByDate()` cannot go stale after an insert with a later date. **Confirmed by the user as also applied retroactively to `HabitWeekService.addWeek`**, to close the same gap for `findFirst()`.
* `HabitCalendarService.countDaysCompletedHabitsByWeek`/`countDaysCompletedTasksByWeek` were left deliberately uncached — not a new decision, a direct application of the existing `HabitCompletion`/`TaskCompletion` caching exclusion.
* `HabitService.findByDayOfWeekAndUser`/`countByUserAndDayOfWeek` were missing the `AndIsActiveTrue` filter every sibling listing method has; **confirmed by the user as fixed** in the same pass as the `HabitWeekService.addWeek` correction above.

### **Why:**

* A targeted `@CachePut` is only safe when the fields making up the cache key are provably disjoint from what the write actually changes (established in the `PercentageDay`/`PercentageWeek` entry); `HabitCategory.name` fails that test because it is simultaneously the field being edited and the field the cache is keyed on, so this project's own rule for it — full eviction, not a put — is a correct instance of the caveat already written into Section 11, not an exception to it.
* `@CachePut` always writes a value into the cache under a computed key; it has no mechanism to make a key resolve to "not found," which is exactly what `toggleDeleted` moving a category out of the active scope requires for `findActiveByName` — only `@CacheEvict` can express that.
  **Source:** [Spring Framework Reference — Cache Abstraction, `@CachePut`/`@CacheEvict`](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)
* A `findFirst`/`findTop`/`findLatest`-shaped query's result set is, by definition, sensitive to every insert into its table, not just inserts matching an existing cached key — so treating its cache the same as an ordinary point-lookup's (unaffected by unrelated new rows) understates what the `add`/`create` path needs to invalidate.
* Caching a method whose data is derived from `HabitCompletion`/`TaskCompletion` would reintroduce, one join away, the exact high-churn caching risk Section 11 already excludes those two entities from directly.

### **Impact:**

* `HabitTaskService.updateHabitTask` is now actually callable (previously threw unconditionally on every input), and both `HabitTaskService.toggleActive` and `HabitService.toggleActive` are real bidirectional toggles rather than one-way deactivation switches.
* All `Page<T>`-returning methods across `HabitTaskService`, `HabitService`, `HabitDayWeekService`, and `HabitCalendarService` are now cache-safe (`PageModel`), matching the standing project-wide rule.
* `HabitDayWeekService.findHabitsByDayAndUser` is correctly invalidated by `HabitService`'s own write paths with zero additional coupling code, establishing a reusable pattern for any future method that reads one entity from a different entity's service.
* `HabitCategoryService`'s rename/soft-delete paths no longer risk serving a stale by-name lookup under either the old key or the wrong active/all scope.
* `HabitCalendarService.findFirstByDate()` — and, per the user's confirmation, `HabitWeekService.findFirst()` — can no longer serve a stale "latest" result after a newer record is inserted.
* Two entries deferred to `REFACTORING_BACKLOG.md`, not resolved here: applying the "cache name follows the entity" fix to `HabitWeekService.findHabitCalendarsByWeekId` (now that `HabitCalendarService`'s eviction set is known and confirmed sufficient), and `PercentageDayService.findByUserAndHabitWeek`'s Section 2 parameter-shape inconsistency.

---

## [Security Review — JWT/UserDetails Layer: Authority Prefix, Lazy `roles` Fetch, and `CustomUserDetails` Password Leak]

### **Problem:**

* `CustomUserDetailsService.loadUserByUsername()` built authorities via `new SimpleGrantedAuthority(role.getName())`, while `Role.name` is stored bare (`"ADMIN"`/`"USER"`). `SecurityConfig`'s `.hasRole(ROLE_ADMIN)`/`.hasRole(ROLE_USER)` checks require an authority literally named `"ROLE_ADMIN"`/`"ROLE_USER"` (Spring's `hasRole()` auto-prepends `ROLE_` before comparing). Result: every `hasRole(...)`-gated endpoint (`/aadati/api/admin/**`, `/aadati/api/user/**`) rejected every authenticated user with a 403, regardless of their actual role — the authorization layer was fully broken from first deploy.
* `UserRepository.findByUsername(String username)` had no `JOIN FETCH`/`@EntityGraph` on `roles` (`@ManyToMany`, LAZY by default). `loadUserByUsername()`'s subsequent `user.getRoles()` call was one `spring.jpa.open-in-view=false` config change away from throwing `LazyInitializationException` on every single login — currently only alive because Spring Boot's `open-in-view` default (`true`) happens to be masking it.
* `CustomUserDetails` is a `record implements UserDetails`; its compiler-generated `toString()`/`equals()`/`hashCode()` included the raw BCrypt `password` field, meaning any accidental `log.info(userDetails)`/`log.debug(...)` call anywhere in the codebase would write a password hash straight into the logs.

### **Decision:**

* Changed authority construction to `new SimpleGrantedAuthority("ROLE_" + role.getName())` in `CustomUserDetailsService.loadUserByUsername()`.
* Replaced the derived-query `findByUsername` with an explicit fetch-join query:
  ```java
  @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
  Optional<User> findByUsername(@Param("username") String username);
  ```
* Added an explicit `toString()` override to `CustomUserDetails` that excludes `password`, and annotated the explicit `getPassword()` accessor (added to satisfy the `UserDetails` interface contract) with `@JsonIgnore` as defense-in-depth against accidental Jackson serialization.
* Confirmed (separately) that `AuthenticationService.logout()` correctly blocklists both the access and refresh token using their real `extractExpiration(token).getTime()` values, not a recomputed/hardcoded expiry — no fix needed there, closing an open question from this same review pass.

### **Why:**

* Spring Security's `hasRole("X")` is sugar for `hasAuthority("ROLE_X")` — it always prepends `ROLE_` before comparing against the principal's granted authorities, so authorities must be stored with that prefix already applied at the point they're built, not at the point they're checked.
  **Source:** [Spring Security Reference — Authorization Architecture, `hasRole` vs `hasAuthority`](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)
* `@ManyToMany` associations default to `FetchType.LAZY` in JPA; accessing a lazy collection outside an active persistence context throws `LazyInitializationException`. Relying on `spring.jpa.open-in-view=true` (which keeps a DB connection open for the whole request to paper over this) is explicitly called out by the Spring team as an anti-pattern to move away from, not a fetch strategy to depend on.
  **Source:** [Spring Boot Common Application Properties — `spring.jpa.open-in-view`](https://docs.spring.io/spring-boot/appendix/application-properties/index.html) / [Vlad Mihalcea — "The Open Session In View Anti-Pattern"](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/)
* For Java records, Jackson resolves a logical property name from *all* accessors that map to it (the canonical record-component accessor and any additional JavaBean-style `getX()` method both normalize to the same property name) and merges their annotations onto that single property. This was verified empirically for this exact class shape: an `ObjectMapper.writeValueAsString(...)` call on a `CustomUserDetails` instance produced no `password` key and no trace of the raw password value in the output JSON, confirming `@JsonIgnore` on the explicit `getPassword()` accessor also suppresses the record's separate auto-generated `password()` accessor rather than leaving it exposed.
  **Source:** [FasterXML Jackson — Record support (`jackson-databind` release notes, 2.12+)](https://github.com/FasterXML/jackson-databind/wiki/Jackson-Release-2.12#records-support)

### **Impact:**

* Every `hasRole(...)`-gated endpoint in the application is now actually reachable by users with the correct role — this was a full authorization outage in the original code, not a hypothetical.
* Login no longer depends on `open-in-view` masking a real N+1/lazy-init gap; `findByUsername` now fetches `roles` in the same query deterministically, independent of that config's value.
* `CustomUserDetails` can now be logged, `toString()`'d, or (accidentally) serialized without leaking the BCrypt hash — closed via two independent layers (explicit `toString()` + `@JsonIgnore`), each covering a different accidental-exposure path (log statements vs. JSON serialization).
* `AuthenticationService.logout()` confirmed correct with no changes needed — both the access and refresh token are blocklisted under their real JWT `exp` claim.
* Follow-up left open in `REFACTORING_BACKLOG.md`: auditing and disabling `spring.jpa.open-in-view` project-wide now that this one lazy-association gap is fixed directly, since OSIV may still be masking others.

---

## [First Controller Review — `UserController`: Removing a Redundant `@PreAuthorize`, and Confirming `GlobalExceptionHandler` as the Project's Single `@RestControllerAdvice`]

### **Problem:**

* `UserController.update()` carried `@PreAuthorize("hasRole('ADMIN') || hasRole('USER')")`, while `SecurityConfig` already gates the entire `/aadati/api/user/**` path with `.hasAnyRole(ROLE_USER, ROLE_ADMIN)` at the filter-chain level — before the `DispatcherServlet` ever reaches a controller method. The `@PreAuthorize` expression was therefore checking a condition already guaranteed true for every request that could reach it: functionally dead code that could never deny anything the path rule hadn't already denied. It was also applied to only 1 of the controller's 5 methods, all under the identical path rule, with no semantic reason for the asymmetry.
* Separately, this review confirmed the longstanding `REFACTORING_BACKLOG.md` §3 item — "build a single `@RestControllerAdvice` covering the whole application" — is already done: `GlobalExceptionHandler` exists, uses RFC 7807 `ProblemDetail` (not the originally-envisioned custom `ApiResponse.error(...)` shape — a superseding, and better, decision, since `ProblemDetail` is Spring Boot 3's first-class supported error format), covers the project's full domain-exception taxonomy (`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleViolationException`, `InvalidRequestException`, `InvalidCredentialsException`, `OperationFailedException`, `DataIntegrityViolationException`, `MessagingException`), a `MethodArgumentNotValidException` handler with per-field error detail, and a safe `Exception.class` fallback — with logging severity correctly split between `log.debug` for expected 4xx client errors and `log.warn`/`log.error` for genuine server-side faults.

### **Decision:**

* Removed the `@PreAuthorize` annotation from `UserController.update()`.
* Adopted a project-wide policy (documented in `ENGINEERING_RULES.md` §7): `SecurityConfig`'s path-level `hasRole`/`hasAnyRole` rules are the single source of truth for role-based access on a URL pattern; `@PreAuthorize`/`@EnableMethodSecurity` is reserved exclusively for authorization logic a URL pattern cannot express (e.g. resource-ownership checks keyed off a path/body ID, not the caller's own role).
* Closed the `REFACTORING_BACKLOG.md` §3 "single `@RestControllerAdvice`" item as done, based on the confirmed `GlobalExceptionHandler` implementation described above.

### **Why:**

* Spring Security's `FilterSecurityInterceptor`/`AuthorizationFilter` runs as part of the servlet filter chain, ahead of the `DispatcherServlet` and therefore ahead of any `@PreAuthorize` AOP advice on a controller method — a role check repeated at the method level after an equivalent path-level check has already passed can only ever evaluate to `true`, making it unreachable-in-practice authorization logic rather than a genuine second layer of defense.
  **Source:** [Spring Security Reference — Authorization Architecture (Filter Chain vs. Method Security ordering)](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)
* RFC 7807 (`application/problem+json`) is the standard Spring Boot 3.x itself uses for its own default error responses (`ProblemDetail`), making it the natural fit for a hand-written `@RestControllerAdvice` too — consistent shape whether an error comes from Spring's own machinery or this project's handlers.
  **Source:** [RFC 7807 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807); [Spring Framework Reference — Error Responses (`ProblemDetail`)](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)

### **Impact:**

* `UserController` no longer carries authorization logic split across two layers that say the same thing — `SecurityConfig` is now unambiguously the only place role-gating for this path is decided.
* Any future controller needing `@PreAuthorize` has a documented rule for when it's actually warranted, preventing the same redundant-annotation pattern from recurring.
* `REFACTORING_BACKLOG.md` §3's oldest open item is closed; two new, more specific gaps were found *inside* the now-confirmed `GlobalExceptionHandler` and logged separately as still-open backlog items (not fixed in this entry): a missing `ConstraintViolationException` handler (path/query-param Bean Validation failures currently fall through to a generic 500) and a missing `AccessDeniedException` handler (would misreport a future method-level authorization denial as a 500 instead of 403).

---

## [`GlobalExceptionHandler` — Added `ConstraintViolationException` and `AccessDeniedException` Handlers]

### **Problem:**

* Bean Validation constraints on `@PathVariable`/`@RequestParam` (e.g. `UserController.hasRoles`'s `@NotBlank role`) throw `jakarta.validation.ConstraintViolationException`, a distinct type from `MethodArgumentNotValidException` (which only covers `@RequestBody @Valid` failures). With no dedicated handler, it fell through to the generic `Exception.class` handler: wrong status (500 instead of 400), no field-level detail returned to the client, and a full-stack-trace `log.error` entry for a routine client input mistake.
* Per the `@PreAuthorize` policy just adopted in `ENGINEERING_RULES.md` §7, any future method-level authorization denial throws `org.springframework.security.access.AccessDeniedException`. With no dedicated handler, that would also have fallen through to the generic `Exception.class` handler and returned 500 instead of the correct 403.

### **Decision:**

* Added `@ExceptionHandler(ConstraintViolationException.class)`, building the same `Map<String, String> fieldErrors` shape as the existing `MethodArgumentNotValidException` handler, from `ex.getConstraintViolations()` (`cv.getPropertyPath().toString()` → field key, `cv.getMessage()` → message), returning `400 BAD_REQUEST`.
* Added `@ExceptionHandler(AccessDeniedException.class)`, returning `403 FORBIDDEN` with a generic message and a `log.warn` (not `log.error` — this isn't a server fault).
* Verified empirically (Hibernate Validator 8.0.1, a minimal `ExecutableValidator.validateParameters(...)` reproduction of the exact `hasRoles(userDetails, @NotBlank role)` shape) that `ConstraintViolation.getPropertyPath().toString()` for a method-parameter constraint returns `"methodName.paramName"` (observed: `"hasRoles.role"`), not the bare parameter name. Left as a noted cosmetic follow-up, not a blocking fix: the field key in the response for a path/query-param violation will read `"hasRoles.role"` instead of `"role"`, inconsistent with the clean field names the body-validation handler returns — worth a one-line `substring(lastIndexOf('.') + 1)` strip if/when that consistency matters to a consumer of the API.

### **Why:**

* `ConstraintViolationException` (Jakarta Bean Validation, thrown by `ExecutableValidator` for method/parameter-level constraints) and `MethodArgumentNotValidException` (Spring MVC, thrown for `@RequestBody @Valid` binding failures) are unrelated exception hierarchies — a handler for one does not catch instances of the other, so both are required for full `@Valid`/Bean-Validation coverage across a controller.
  **Source:** [Jakarta Bean Validation Specification — Method and Constructor Validation](https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html); [Spring Framework Reference — Validation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
* `AccessDeniedException` is what Spring Security's method-security AOP advice throws on a failed `@PreAuthorize`/`@PostAuthorize` check; the correct HTTP mapping per RFC 9110 §15.5.4 is 403 (the request is understood and the credentials are valid, but the action is refused), not a generic 500.
  **Source:** [Spring Security Reference — Authorization Architecture](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html); [RFC 9110 §15.5.4 — 403 Forbidden](https://www.rfc-editor.org/rfc/rfc9110#section-15.5.4)

### **Impact:**

* Path/query-parameter validation failures anywhere in the application now return a structured `400` with field-level detail instead of an opaque `500`, and no longer pollute error-level logs for routine client mistakes.
* The codebase is now future-proofed for the first real `@PreAuthorize` usage (per the `ENGINEERING_RULES.md` §7 policy): a denial will correctly surface as `403`, not `500`, with no further `GlobalExceptionHandler` changes needed when that day comes.
* Both `REFACTORING_BACKLOG.md` §3 items this entry addresses are closed. The property-path-prefix cosmetic note is intentionally *not* a backlog item — it's a documented, low-priority observation only, to avoid the backlog accumulating non-actionable nitpicks.

---

## [`UserMetricsController` — First Admin-Metrics Controller: Fixed `@Validated` Gap, Copy-Paste Method-Name Collision, and Added `MethodArgumentTypeMismatchException` Handling]

### **Problem:**

* `UserMetricsController` (mapped under `/aadati/api/admin/count`, `hasRole(ROLE_ADMIN)`-gated) had no class-level `@Validated`, so `@NotBlank` on `countByRole`'s `@PathVariable role` was never actually enforced at runtime — the same "annotation present but inert" trap already caught once in `UserController` (there it was `@NonNull` instead of a real constraint; here the real constraint existed but the class-level trigger for it didn't).
* Two controller methods were both named `countEmailVerified` — a copy-paste artifact. The second one, mapped to `GET /byrole/{role}` and actually calling `userService.countByRoleName(role)`, had nothing to do with email verification. It compiled fine (different parameter lists make it a valid overload) but was misleading, and risked an `operationId` collision in generated OpenAPI docs (per the earlier `SwaggerConfig` review, since springdoc derives `operationId` from the method name by default).
* `countCreatedBetween(@PathVariable Instant start, @PathVariable Instant end)` surfaced a third exception type not yet covered by `GlobalExceptionHandler`: a malformed path variable that can't convert to `Instant` throws `MethodArgumentTypeMismatchException`, distinct from both `ConstraintViolationException` (Bean Validation failures) and `MethodArgumentNotValidException` (body-validation failures). Unhandled, it fell through to the generic `Exception.class` handler and returned 500 instead of 400.

### **Decision:**

* Added `@Validated` to `UserMetricsController`.
* Renamed the second `countEmailVerified` method to `countByRole`, matching the service method it calls (`countByRoleName`).
* Added `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` to `GlobalExceptionHandler`, returning `400 BAD_REQUEST` with a message built from `ex.getName()`/`ex.getValue()`/`ex.getRequiredType()`, logged at `log.debug` (client input error, not a server fault).

### **Why:**

* Spring's `MethodValidationPostProcessor` only wraps a bean in a validating proxy when that bean's class carries `@Validated`; a Bean Validation constraint on a `@PathVariable`/`@RequestParam` of a class missing `@Validated` is silently never checked — this is now confirmed twice in this codebase (`UserController`, `UserMetricsController`), enough to warrant a project-wide audit (tracked in `REFACTORING_BACKLOG.md`).
  **Source:** [Spring Framework Reference — Validation (Method Validation)](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
* A type-conversion failure on a path variable is a client input error (malformed request), not a server fault — the correct response is 400, not 500, the same principle already applied to `ConstraintViolationException`/`AccessDeniedException` in the prior `GlobalExceptionHandler` entry.
  **Source:** [Mastering Backend — @PathVariable in Spring Boot, "Handling Type Conversion Failures"](https://blog.masteringbackend.com/mastering-path-variable-in-spring-boot); [RFC 9110 §15.5.1 — 400 Bad Request](https://www.rfc-editor.org/rfc/rfc9110#section-15.5.1)

### **Impact:**

* `countByRole`'s `@NotBlank` constraint is now actually enforced — a blank/whitespace-only `role` path segment returns a structured `400` via the existing `ConstraintViolationException` handler instead of reaching the service layer unchecked.
* No more ambiguous duplicate method name in the controller; safe for future Swagger/OpenAPI codegen.
* Any malformed path variable anywhere in the application that fails type conversion (not just `Instant` — any `@PathVariable`/`@RequestParam` typed parameter) now returns a structured `400` with a client-actionable message instead of an opaque `500`.
* `GlobalExceptionHandler` has now accumulated handlers for all three distinct validation/binding failure modes a Spring MVC controller can produce (`MethodArgumentNotValidException` for `@RequestBody @Valid`, `ConstraintViolationException` for method-parameter constraints, `MethodArgumentTypeMismatchException` for type conversion) plus `AccessDeniedException` for authorization — full coverage of the client-error space this project's controllers can trigger, confirmed method-by-method across two controllers so far.
* Still open, not part of this entry: project-wide `@Validated` audit (`REFACTORING_BACKLOG.md` §4), and whether `countCreatedBetween` needs a `start <= end` domain guard (not yet answered).

---

## [`AuthController` — Critical Fix: Refresh Token Removed from the URL Path, Plus `UserController.logout()` Hardened to Use the Request's Own `Authorization` Header]

### **Problem:**

* `AuthController.refresh(@PathVariable("refresh") String refreshToken)` was mapped as `POST /aadati/api/auth/refresh/{refresh}` — the refresh token, a long-lived, highly sensitive credential (materially more damaging if leaked than the short-lived access token, since it can mint fresh access tokens indefinitely until it expires or is blocklisted), was transmitted as a raw URL path segment. Regardless of the HTTP method being `POST`, the full path is part of the request line and gets written by default into web server, reverse proxy, load balancer, API gateway, and APM/monitoring logs across the entire infrastructure stack — none of which this project controls once deployed.
* `AuthController.register()` was mapped at `POST /aadati/api/auth/registre` — a typo in a public-facing URL.
* `UserController.logout()` accepted both the access token and refresh token via a client-submitted `TokenPairRequest` body, with no correlation to the identity that authenticated the request. The endpoint required authentication (`/aadati/api/user/**`), but nothing tied the submitted `accessToken` value to the token that actually authenticated the caller — the method trusted a client-supplied string as the thing to blocklist, rather than deriving it from data the server had already independently verified.

### **Decision:**

* Changed `AuthController.refresh` to `POST /aadati/api/auth/refresh` with `@Valid @RequestBody RefreshTokenRequest request`, matching the same shape as `login`/`register`.
* Fixed the `/registre` → `/register` typo.
* Removed the unused `UserMapper` field/import from `AuthController` (neither `register` nor `login` used it directly — `AuthService` already returns the mapped response types).
* Changed `UserController.logout()` to extract the access token from the request's own `Authorization` header (`@RequestHeader("Authorization") String authHeader`, stripping the `"Bearer "` prefix) instead of accepting it in the request body; only the refresh token is still accepted via `@Valid @RequestBody RefreshTokenRequest`.
* A proposed alternative — having the client submit their own `userId` alongside the tokens and adding a `@PreAuthorize` ownership check comparing it to the authenticated principal's id — was considered and rejected: it validates a self-declared field with no cryptographic link to the submitted tokens, so it would not actually prevent a caller from submitting someone else's leaked token pair for blocklisting; it only adds an unrelated check while leaving the real gap open. The chosen fix closes the gap directly by never trusting a client-submitted value for the access token in the first place.

### **Why:**

* RFC 6750 §5.3 (the Bearer Token spec this project's own JWT scheme is built on) explicitly says: *"Bearer tokens SHOULD NOT be passed in page URLs. Browsers, web servers, and other software may not adequately secure URLs in the browser history, web server logs, and other data structures."*
  **Source:** [RFC 6750 §5.3 — OAuth 2.0 Bearer Token Usage, Security Considerations](https://www.rfc-editor.org/rfc/rfc6750#section-5.3)
* OWASP's data-protection guidance states the same rule generally, independent of the Bearer-token spec: *"Do not include sensitive information in the URL or query string, such as an API key or session token."*
  **Source:** [OWASP Developer Guide — Protect Data Everywhere](https://devguide.owasp.org/en/04-design/02-web-app-checklist/08-protect-data/); [CWE-598 — Use of GET Request Method With Sensitive Query Strings](https://cwe.mitre.org/data/definitions/598.html) (the same logging-exposure risk applies to any URL segment, not only GET/query strings)
* The access token that authenticated the current request is, by construction, already known to the server the moment `JwtAuthenticationFilter` validates the `Authorization` header — re-deriving it from that header rather than accepting a client-submitted copy means there is no code path where the "wrong" token could ever be blocklisted; correctness follows from the data's origin, not from an added validation step.

### **Impact:**

* No refresh token — the single most sensitive credential in this project's auth scheme — will ever appear in a URL, request line, or any log that captures one, closing a real, confirmed credential-exposure vector before this project reaches an external reviewer.
* `logout()` can no longer be made to blocklist an access token the caller didn't actually authenticate with; the residual risk on the refresh token side (a caller submitting a leaked refresh token they don't own) was assessed and accepted as low-impact — anyone already holding a valid refresh token can already fully impersonate that session via `/auth/refresh`, so blocklisting it via `logout` grants no additional capability beyond a forced-logout-style denial of service.
* The stale `REFACTORING_BACKLOG.md` §1 item questioning whether the refresh-token flow was ever fully implemented is now confirmed resolved — `JwtUtils.generateRefreshToken`/`isRefreshTokenValid`, `AuthController.refresh`, and `AuthenticationService.logout`'s dual-token blocklisting are all in place and reviewed.
* Still open, low priority: `register()` returning `200` instead of the more RESTful `201 Created`.

---

## [`AuthController.register()` — Corrected Status Code to `201 Created`]

### **Problem:**

* `register()` returned `200 OK` on a successful `POST` that creates a new `User` — REST convention reserves `200` for a successful read/update of an existing resource, while a successful creation should return `201 Created`.

### **Decision:**

* Changed the response to `ResponseEntity.status(HttpStatus.CREATED).body(authService.register(userRequest))`.
* No `Location` header was added — deliberately: this API exposes no `GET /users/{id}`-style endpoint a `Location` header could meaningfully point to (`profile` is only reachable via `GET /user`, scoped to the caller's own id from `@AuthenticationPrincipal`), so adding one would be complexity without a real target.

### **Why:**

* Per RFC 9110 §15.3.2, `201 Created` is the standard response for a request that has successfully resulted in the creation of a resource; `200 OK` is reserved for cases where the response body doesn't represent a newly created resource in that specific sense.
  **Source:** [RFC 9110 §15.3.2 — 201 Created](https://www.rfc-editor.org/rfc/rfc9110#section-15.3.2)

### **Impact:**

* `POST /aadati/api/auth/register` now returns the semantically correct status code for a resource-creation endpoint.
* `REFACTORING_BACKLOG.md`'s last open item from the `AuthController` review pass is closed — `AuthController` and `UserController.logout()` are both fully reviewed with no known open issues.

---

## [`TaskPriorityLevelController` / `AdminTaskPriorityLevelController` — Critical Fix: `@NotBlank` on a `UUID` Path Variable, Plus a Duplicated Typo]

### **Problem:**

* `AdminTaskPriorityLevelController.update()` and `.toggleDelete()` both declared `@NotBlank @PathVariable("priorityID") UUID priorityID`. `@NotBlank` (Jakarta Bean Validation) only supports `CharSequence` types; applied to `UUID`, Hibernate Validator cannot resolve a constraint validator for the pairing at all. Verified empirically (Hibernate Validator 8.0.1, `ExecutableValidator.validateParameters(...)` against this exact method/parameter shape): it throws `jakarta.validation.UnexpectedTypeException` — `"No validator could be found for constraint 'jakarta.validation.constraints.NotBlank' validating type 'java.util.UUID'"` — a `RuntimeException` with no dedicated `GlobalExceptionHandler` entry, meaning it fell through to the generic `Exception.class` handler and returned `500`. Both endpoints were completely non-functional: every call failed before ever reaching `TaskPriorityLevelService`.
* Both controllers separately carried the same typo — `"/searche/{level}"` instead of `"/search/{level}"` — on their respective priority-level lookup endpoint, copy-pasted from one controller into the other.

### **Decision:**

* Removed the `@NotBlank` constraint entirely from both `UUID priorityID` parameters, rather than replacing it with `@NotNull`. A `@PathVariable UUID` can never actually be `null` at the point the method body executes: a missing URL segment fails to match the route (Spring returns `404` without invoking the method), and an unparseable segment throws `MethodArgumentTypeMismatchException` (already handled by `GlobalExceptionHandler`, per the earlier `UserMetricsController` entry) before the method runs. Any not-null-style constraint at this position would be validating a condition the routing layer already makes structurally impossible — dead validation, not a safety net.
* Fixed the `"/searche/{level}"` → `"/search/{level}"` typo in both controllers.

### **Why:**

* Per the Jakarta Bean Validation specification, `@NotBlank`'s constraint validator is only defined for `CharSequence`; a type without a matching `ConstraintValidator` implementation causes Hibernate Validator to raise `UnexpectedTypeException` rather than silently skipping the check — this fails loud, but only the first time the constrained method is actually invoked, which is exactly why it wasn't caught until this review exercised the two endpoints directly.
  **Source:** [Jakarta Bean Validation — `jakarta.validation.constraints.NotBlank` Javadoc](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/notblank); [Hibernate Validator — HV000030 error reference](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/#_hv000030)
* Spring MVC's routing resolves a `@PathVariable`'s presence and type before the controller method is ever invoked (`404` for a missing segment, `MethodArgumentTypeMismatchException` for an unparseable one) — so a not-null constraint applied *after* that point in the pipeline has no case left to actually catch.
  **Source:** [Spring Framework Reference — `@PathVariable`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/pathvariables.html)

### **Impact:**

* `PUT /aadati/api/admin/priority-level/update/{priorityID}` and `PUT /aadati/api/admin/priority-level/toggle/{priorityID}` are now actually callable — this was a full outage on both endpoints, not a hypothetical or edge case.
* Both controllers' priority-level lookup endpoint now resolves at the correct, typo-free path.
* `REFACTORING_BACKLOG.md`'s "audit every other `@PathVariable`/`@RequestParam` for a similar constraint/type mismatch" item remains open as a project-wide follow-up, now that one confirmed instance has been found and fixed.

---

## [`TaskCompletionController` — Critical Fix: Shared Mutable Field on a Singleton Bean (Cross-User Race Condition), Plus an Unscoped Update Endpoint and Another Constraint/Type Mismatch]

### **Problem:**

* `TaskCompletionController` declared `private PageModel<TaskCompletion> completionPageModel;` as an instance field, assigned inside seven different endpoint methods and read back on the immediately following line to build each response. Since `@RestController` beans are singleton-scoped by default, every concurrent HTTP request across every user is served by the *same* controller instance sharing that *same* field — under real concurrent load, one request's thread could read back a different concurrent request's assignment to that field in the gap between the write and the read, returning one user's `TaskCompletion` data in a different user's HTTP response. This is a genuine cross-user data exposure risk, not a hypothetical: it passes all single-request manual testing and only manifests under actual concurrency, which is exactly the class of defect most likely to reach production undetected.
* `update()` was the only endpoint in the controller that didn't take `@AuthenticationPrincipal CustomUserDetails` or pass a caller id into its service call — every other endpoint in the class scoped its query to `(resourceId, callerId)` together (the project's established IDOR-prevention pattern), but `updateCompletion(request)` had no caller identity available to it at all, meaning nothing at the controller level prevented one authenticated user from updating another user's `TaskCompletion` record by id.
* `countByTaskAndUserAndComplete`'s `@Positive @PathVariable("complete") boolean complete` repeated the same class of defect already fixed in `AdminTaskPriorityLevelController`: `@Positive` only supports numeric types, not `boolean`. Verified empirically (Hibernate Validator 8.0.1): `"No validator could be found for constraint 'jakarta.validation.constraints.Positive' validating type 'java.lang.Boolean'"` — an `UnexpectedTypeException` on every call to this endpoint.

### **Decision:**

* Removed the `completionPageModel` field entirely; each method now builds its `PageModel<TaskCompletion>` result as a local variable.
* Added `@AuthenticationPrincipal CustomUserDetails userDetails` to `update()` and changed the service call to `taskCompletionService.updateCompletion(userDetails.id(), request)`, bringing it in line with every other endpoint's ownership-scoping pattern.
* Removed `@Positive` from the `complete` parameter (a `boolean` has no invalid value to constrain against).
* Replaced `@Positive` with `@Min(0)` on every `pageNumber` parameter across the controller, confirming (in response to a question raised during this review) that this project's page numbers are zero-indexed, matching Spring Data's own `Pageable` convention — `@Positive` had been incorrectly excluding page `0`, the first page.
* Documented all of the above as reusable, project-wide rules in a new `ENGINEERING_RULES.md` §12 ("Controllers — REST Layer Conventions"), consolidating this entry's findings with prior controller-review conclusions (the `@PreAuthorize` policy from §7, the `@Validated`-required-for-constrained-path-variables rule, the credentials-never-in-URL rule) into one canonical reference section for future controllers.

### **Why:**

* Spring beans are singleton-scoped by default; an instance field on a singleton is shared, mutable state visible to every thread handling every concurrent request against that bean — using one to pass a value from one line of a method to the next (rather than a local variable) turns an ordinary read-after-write into a data race the moment two requests are in flight at once.
  **Source:** [Spring Framework Reference — Bean Scopes (`singleton` is the default)](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html)
* The IDOR-prevention pattern this project already follows (`(resourceId, callerId)` scoping at the service boundary) only works if every endpoint touching a per-user resource actually supplies the caller id — an endpoint that omits it is a gap in an otherwise-consistent defense, not a stylistic inconsistency.
* `@Positive`'s Jakarta Bean Validation contract only defines behavior for numeric types; Hibernate Validator raises `UnexpectedTypeException` rather than silently ignoring a mismatched type, which is why this failed loudly the moment the endpoint was actually exercised rather than at compile time.
  **Source:** [Jakarta Bean Validation — `jakarta.validation.constraints.Positive` Javadoc](https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/positive)

### **Impact:**

* Eliminates a real cross-user data-race/exposure risk in `TaskCompletionController` — the most severe class of defect found in this review series so far, since it risked leaking one user's private data to another under ordinary production load rather than merely rejecting/misprocessing a request.
* `update()` can no longer be used to modify another user's `TaskCompletion` record purely by knowing/guessing its id (pending final confirmation once `TaskCompletionService.updateCompletion` itself is reviewed, to verify the passed-in `userId` is actually used as an ownership guard and not just accepted and ignored).
* `GET /count/taskid/{taskId}/complete/{complete}` is now actually callable.
* Page-number validation across the controller now correctly allows page `0` as the first page, consistent with Spring Data's convention, closing the question raised earlier in this review.
* `ENGINEERING_RULES.md` §12 now gives future controllers a single place to check before writing similar code, rather than relying on this defect being independently rediscovered per-controller.

---

## [`HabitCompletionController` — Wrong HTTP Method on a Read Endpoint, Plus a Third Project Typo]

### **Problem:**

* `findByCompletionIdAndUser` — a pure lookup, no mutation — was mapped with `@PutMapping("/completion/{completion}")` instead of `@GetMapping`, almost certainly copy-pasted from the `update()` method directly above it in the same class. `PUT` implies an idempotent replace/update to any client, tooling, or intermediary reading the API surface, which is misleading for a read-only endpoint and inconsistent with the identical method in `TaskCompletionController`, correctly mapped as `@GetMapping("/id/{id}")`.
* `getByHabitCalendarAndUser`/`countByCalendarAndUserAndComplete` had "calender" (missing the 'a') repeated across the URL path segment, the `@PathVariable` string literal, and the Java parameter name — internally consistent (compiles and runs correctly) but a spelling mistake throughout, the third one found in this project after `/auth/registre` and `/searche/{level}`.

### **Decision:**

* Changed `findByCompletionIdAndUser` to `@GetMapping("/completion/{completion}")`.
* Fixed "calender" → "calendar" everywhere it appeared in both affected methods.
* Confirmed, not fixed (deliberate, pre-existing product decision, not a defect introduced by this controller): `getTodayByUser`/`getTodayByUserAndComplete`'s "today" boundary is computed against the server's clock/timezone rather than each user's own timezone — carried over from an earlier version of the project. Logged in `REFACTORING_BACKLOG.md` §12 (Deferred) as a known limitation to revisit only if multi-timezone correctness becomes a real product requirement.

### **Why:**

* HTTP method semantics (`GET` = safe/read-only, `PUT` = idempotent replace) are part of a REST API's contract with its own consumers and any generic HTTP tooling (caches, browsers, generated client SDKs) — mismatching them doesn't break Spring's routing, but it breaks the contract those consumers rely on.
  **Source:** [RFC 9110 §9.3 — HTTP Semantics, Method Definitions](https://www.rfc-editor.org/rfc/rfc9110#section-9.3)

### **Impact:**

* `GET /aadati/api/user/habit-completion/completion/{completion}` now correctly advertises itself as a safe, read-only operation, consistent with its `TaskCompletionController` counterpart.
* No more "calender" typo anywhere in this controller's URL surface.
* `HabitCompletionController` is otherwise a clean, correctly-scoped mirror of `TaskCompletionController` — no instance-field state, no constraint/type mismatches, consistent `(resourceId, callerId)` scoping on every endpoint including `update()`. No further issues found.

---

## [`RoleController` — Critical Fix: Missing `@RequestBody` on `create()` and `update()`]

### **Problem:**

* Both `create(@Valid RoleRequest roleRequest)` and `update(@PathVariable UUID roleId, @Valid RoleRequest roleRequest)` were missing `@RequestBody` on their `RoleRequest` parameter. Without it, Spring MVC treats a non-primitive, unannotated method parameter as an implicit `@ModelAttribute`, binding it from servlet request parameters (query string / form-urlencoded fields) rather than deserializing the JSON request body — meaning a normal JSON `POST`/`PUT` client (every other endpoint in this project expects one) would never have its body actually read. Both endpoints were functionally broken for real usage: fields would bind to `null`/default values (likely surfacing as a `@Valid` failure or a downstream `DataIntegrityViolationException`), not the JSON payload the client sent.

### **Decision:**

* Added `@RequestBody` to both parameters: `create(@Valid @RequestBody RoleRequest roleRequest)`, `update(@PathVariable("id") UUID roleId, @Valid @RequestBody RoleRequest roleRequest)`.

### **Why:**

* Spring MVC only deserializes a JSON request body into a method parameter when that parameter is annotated `@RequestBody`; an unannotated complex-type parameter falls back to data-binding from request parameters via `ServletModelAttributeMethodProcessor`, an entirely different resolution path with no knowledge of the request body at all.
  **Source:** [Spring Framework Reference — `@RequestBody`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html); [Spring Framework Reference — `@ModelAttribute`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html)

### **Impact:**

* `POST /aadati/api/admin/role/create` and `PUT /aadati/api/admin/role/update/id/{id}` now actually accept and process the JSON body a normal client sends — this was a complete functional outage on both endpoints, not an edge case.
* `RoleController` is otherwise fully reviewed with no other issues: correct `@NotBlank`/`String` pairing on `searchByName`/`getByName` (no repeat of the type-mismatch bug class), correct admin-only path scoping, and unrestricted `Role` listing correctly judged not to violate the "aggregate-stats-only" admin rule (`ENGINEERING_RULES.md` §7), since `Role` is system/catalog data, not personal user data.

---

## [`HabitService` — Critical Fix: `userId` Leaking In Through the Request DTO, Plus a Consistent `getReferenceById` Optimization]

### **Problem:**

* `HabitRequest` (the `@RequestBody` DTO for `addHabit`/`updateHabit`) originally declared its own `@NotNull UUID userId` field. Because this field lived inside the JSON body the client controls, any authenticated user could set it to an arbitrary value and create (or, before ownership scoping was double-checked, potentially update) a `Habit` attributed to a different user entirely — an IDOR vector distinct from every previous finding in this project, since those were all about a controller trusting a `@PathVariable`/`@RequestParam` id; this one was about trusting a client-supplied field embedded in the body of an otherwise-correct `@Valid @RequestBody` call.
* Separately (not a bug, a performance question raised during this review): `addHabit`/`updateHabit` were calling `repository.findById(...)` on `User`/`HabitCategory`/`HabitDayWeek` purely to re-attach them as foreign keys on the `Habit` being saved — a full `SELECT *` on each association just to read back an id Hibernate already had.

### **Decision:**

* Removed `userId` from `HabitRequest` entirely. `addHabit`/`updateHabit` now take `UUID userId` as a separate method parameter, supplied by the controller from `@AuthenticationPrincipal CustomUserDetails userDetails` — never from the request body.
* Replaced the `findById` association lookups with `getReferenceById(...)` (Spring Data JPA's lazy-proxy accessor) for `User`, `HabitCategory`, and each `HabitDayWeek` in the list, since none of these associations need their actual column data read — only their id is needed to populate the foreign key on `INSERT`/`UPDATE`.
* Kept `HabitDayWeek` references unguarded (no existence check before `getReferenceById`) because these rows are Engine-seeded, fixed, and never deleted — a `getReferenceById` on a genuinely-stable id is safe.
* Added an explicit `categoryRepository.existsByHabitCategoryIdAndIsDeletedFalse(...)` guard before resolving the `HabitCategory` reference, on both `addHabit` and `updateHabit`, throwing `ResourceNotFoundException` (not a separate "inactive" vs. "not found" message, to avoid leaking which case applies) if the category doesn't exist or has been soft-deleted — because unlike `HabitDayWeek`, `HabitCategory` is a user/admin-managed, soft-deletable resource, so its existence and active status can't be assumed safe without checking first.
* Added `@NotEmpty` to `HabitRequest.habitDayWeekIds()` — a `Habit` with zero scheduled days is a domain-invalid state, previously uncaught by any constraint.

### **Why:**

* A `@RequestBody`-bound DTO is just as attacker-controlled as any `@PathVariable`/`@RequestParam` — validating "is this a well-formed UUID" (`@NotNull`) says nothing about "does the caller have authority over this id." Ownership must always be derived server-side from the authenticated security context, never accepted as a field the client supplies, no matter where in the request it's located.
* `getReferenceById` returns an uninitialized proxy and defers any database hit until a non-id field is actually accessed — for an association that's only ever being used to populate a foreign key column, this avoids a wasted round trip entirely. The trade-off is that an invalid id surfaces later, as a `jakarta.persistence.EntityNotFoundException` at flush time, rather than immediately — acceptable here specifically because `HabitDayWeek` ids are guaranteed valid by the Engine, and `HabitCategory`/`User` ids are independently validated *before* the reference is ever created.
  **Source:** [Spring Data JPA Reference — `JpaRepository.getReferenceById`](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)

### **Impact:**

* A user can no longer create or (if the same pattern had reached `updateHabit` unguarded) attribute a `Habit` to another user's account by manipulating the request body — the request body no longer carries an identity claim at all.
* `addHabit`/`updateHabit` now do one cheap `exists` check on `HabitCategory` instead of a full entity fetch, and zero queries at all for `User`/`HabitDayWeek` beyond what's already required to validate them — fewer round trips per write without weakening any actual invariant.
* A `Habit` can no longer be created or updated with an empty day-of-week schedule.
* `getHabitOrThrow(habitId, userId)` (used by `updateHabit`, `deleteHabit`, `toggleActive`, `findByIdAndUser`) confirmed to scope by both ids together — no separate IDOR gap on the rest of the service's mutation/read paths.

---
