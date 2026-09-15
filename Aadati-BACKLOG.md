# Aadati — Refactoring Backlog

Everything remaining to get Aadati to portfolio-ready state. Organized by category, roughly in priority order. Check items off as they're done, and move completed items into `REFACTORING_LOG.md` as proper `[Fix]`/`[Adopt]` entries with problem/decision/why/impact.

**Status legend:** ✅ Done · 🔄 In Progress · ⬜ Not Started

---

## 1. Security (do first, independent of everything else)

- ⬜ Move all secrets out of `application.properties` into environment variables:
  - `spring.datasource.password`
  - `token.service.secret.key` (JWT signing key)
  - `app.admin.username` / `app.admin.password`
- ⬜ Remove any hardcoded fallback values on `${ENV_VAR:default}` patterns for secrets — app should fail to start if the env var is missing, not run with a known default.
- ⬜ Add `.env`/any file with real secret values to `.gitignore`.
- ⬜ Fix the `expiresIn` bug in `AuthenticationService`: currently hardcoded to `System.currentTimeMillis() + 3600000` (1 hour), while the actual JWT expiration is `token.service.jwt.expiration=900000` (15 minutes). Derive `expiresIn` from the same config value used to generate the token.
- ⬜ Decide on refresh tokens: either implement the flow properly (`generateRefreshToken()` in `JwtService`, a `/auth/refresh` endpoint, populate the field) or remove the unused `token.service.refresh.expiration` config and the always-`null` `refreshToken` field until it's actually built.
- ⬜ Split logging config by profile (`application-dev.properties` / `application-prod.properties`) — `debug=true`, `hibernate.SQL=DEBUG`, and `BasicBinder=TRACE` should not run by default; the last one in particular can log bound SQL parameter values (including sensitive data).

---

## 2. Entities — JPA Auditing Migration

- 🔄 In progress: migrating entities to extend `Auditing` (`@MappedSuperclass` with `@CreatedDate`/`@LastModifiedDate`) instead of individual `@PrePersist`/`@PreUpdate` timestamp logic.
  - ✅ `User` — migrated (Builder and helper-method audit done; verified via usage grep before removing anything)
  - ✅ `Role` — migrated
  - ✅ `PercentageWeek` — migrated; grade-calculation logic kept as a separate `@PrePersist`/`@PreUpdate` method (`calculateGrade()`) since it's domain logic, not generic auditing; `@AllArgsConstructor` removed after confirming zero usage
  - ⬜ `PercentageDay` — same pattern as `PercentageWeek`, not yet done
  - ⬜ `Habit`
  - ⬜ `HabitCategory`
  - ⬜ `HabitWeek`
  - ⬜ `HabitDayWeek`
  - ⬜ `HabitTask`
  - ⬜ `HabitCalendar`
  - ⬜ `TaskCompletion`
  - ⬜ `TaskPriorityLevel`
  - ⬜ Confirm full entity list against `src/main/java/com/mts/aadati/entities/` — the list above is from what's been discussed, not necessarily exhaustive.
- ✅ `HabitCompletion` — intentionally **excluded** from this migration (kept on manual `@PrePersist`/`@PreUpdate`) because its `completedAt` logic depends on business state (`complete` flag), not generic creation/modification tracking.
- ⬜ For each entity migrated: before deleting any method found in the old version, `grep` the codebase to confirm it's actually unused (learned from the `User` helper-methods incident — some "looked unused" methods, like the Builder, were actually load-bearing).
- ⬜ Enable `@EnableJpaAuditing` once at the application config level (confirm this exists — referenced in the log but verify it's actually in the codebase).
- ⬜ Audit for the same "hardcoded literal-by-literal switch" pattern found in `PercentageWeek`/`PercentageDay` grade calculation elsewhere in the codebase, in case it's repeated (see `BACKLOG` note that was folded into this file).

---

## 3. Exception Handling

- ⬜ Build a single `@RestControllerAdvice` covering the whole application. Currently only 4 of 14 controllers have any exception handling at all, and it's inconsistent per-controller `try/catch`.
- ⬜ Fix the dead custom exception class: `com.mts.aadati.exeption.AuthenticationServiceException` (note: package typo `exeption`) is never thrown — `AuthenticationService` throws Spring's built-in `AuthenticationServiceException` instead, almost certainly via IDE auto-import. Either fix the import to use the custom class consistently, or replace it with properly named domain exceptions (`InvalidCredentialsException`, `UserAlreadyExistsException`, `ResourceNotFoundException`, etc.) and throw those from services.
- ⬜ Once domain exceptions exist, wire them into the `@RestControllerAdvice` with consistent `ApiResponse.error(...)` responses.
- ⬜ Remove the per-controller manual `try/catch` blocks in the 4 controllers that have them, once the global handler covers those cases.
- ⬜ Follow OWASP REST Security Cheat Sheet guidance: error responses should be generic (no stack traces/internal details leaked to the client).

---

## 4. Validation

- ⬜ Add `@Valid` to `HabitCategoryController.addAll()` — currently missing on the `List<HabitCategoryRequest>` parameter.
- ⬜ Audit every batch/list endpoint (`addAll`, similar bulk operations across other controllers) for the same gap — remember `@Valid` on a `List<T>` parameter requires `@Validated` on the controller class to actually trigger per-item validation.

---

## 5. Repository / Service / Controller Layer Pass

- ⬜ **HabitCompletion cleanup scheduler** — deferred until this point in the pass is reached. Adds a `@Scheduled` job (via a dedicated `HabitCompletionCleanupScheduler` class + `@EnableScheduling` config) that hard-deletes `HabitCompletion` records older than ~6 months, since accumulating them indefinitely serves no confirmed reporting/analytics need (explicitly decided against retaining old completions for historical stats). Repository method: `deleteAllByCreatedAtBefore(Instant cutoffDate)` with `@Modifying`. Cron schedule needs to be finalized (currently drafted as running twice a year — confirm exact months before enabling) and tested against a non-production database first, since it performs an irreversible hard delete.

(Planned to happen after Exception Handling, per the agreed order — this is a placeholder category to fill in once that layer-by-layer work starts.)

- ⬜ Re-check services for correct use of the now-MapStruct mappers and record-based DTOs — confirm no leftover calls assuming the old class-based DTO getters/setters.
- ⬜ Confirm all `update()`-style mapper calls that resolve an ID to an entity (e.g. `HabitCompletionMapper`) fetch that entity in the service layer, not the mapper — mappers shouldn't do repository access.
- ⬜ (Add specific findings here as this pass happens.)

---

## 6. Testing

- ⬜ Fix the Testcontainers dependency mismatch: `pom.xml` lists `testcontainers:mysql`, but the project uses PostgreSQL — should be `org.testcontainers:postgresql`.
- ⬜ Write unit tests for `AuthenticationService` (login/register success + failure paths), specifically including tests that would catch the `expiresIn` bug and the refresh-token gap.
- ⬜ Write unit tests for other services with real business logic (not just pass-through CRUD) — e.g. `PercentageWeekService`/`PercentageDayService` grade calculation once auditing migration is done there.
- ⬜ Write at least one integration test using Testcontainers (dependency already present, currently unused) for a core repository.
- ⬜ Replace the empty default `contextLoads()` test with real coverage — current baseline is effectively 0%.
- ⬜ (Optional, once tests exist) Add JaCoCo to `pom.xml` and check the coverage report to confirm services/business logic are actually covered, not just DTOs/config.

---

## 7. Docker & CI/CD

- ⬜ Write a multi-stage `Dockerfile` (build with Maven, run with a slim JRE image).
- ⬜ Write `docker-compose.yml` wiring the app + PostgreSQL for local dev.
- ⬜ Add a GitHub Actions workflow (`.github/workflows/*.yml`): checkout → set up JDK 21 → `mvn test` → (optionally) build/push Docker image.

---

## 8. Documentation

- ⬜ Write `README.md`: what the project does, tech stack, architecture diagram, setup instructions, how to run tests/Docker, API docs link.
- ⬜ Delete the leftover Spring Initializr `HELP.md` boilerplate.
- ⬜ Add meaningful `@Operation`/`@Schema` descriptions to Swagger-documented endpoints (currently whatever auto-generates by default, if anything).

---

## 9. Architectural Direction — Pass `userId` Instead of Full `User` Entity in Repository Queries

Decision made while reviewing `PercentageDayRepository`: since the authenticated user's ID is available directly from the Security Context, repository query methods should accept the raw `UUID userId` rather than requiring the caller to first load a full `User` entity just to pass it in. This avoids an unnecessary extra `userRepository.findById(...)` lookup before every query, and makes clear that the method only needs the identity, not any other `User` data.

- ✅ `PercentageDayRepository` — already written using `user.userId` in `@Query`/derived method parameters (e.g. `findAllByUser_UserId(UUID userId, ...)`). This is the target pattern going forward.
- ⬜ **Audit other repositories for the same standardization**, since several were written accepting a full `User` object instead:
  - `HabitRepository` — currently takes `User user` in most methods (`findAllByUserAndIsActiveTrue`, `filterHabits`, etc.)
  - `HabitTaskRepository` — same pattern (`findAllByUserAndIsActiveTrue`, `filterTasks`, etc.)
  - `HabitCompletionRepository` — same pattern (`findAllByUser`, `findTodayByUser`, etc.)
  - Check the rest of the repository list for the same `User user` parameter pattern.
- ⬜ **Exception case to watch for:** if any existing query actually uses a field from `User` beyond its ID (e.g. `user.email`, `user.roles`) inside the JPQL, that query genuinely needs the full `User` entity (or an explicit join) and should **not** be converted to `userId`-only.
- ⬜ This is a project-wide consistency pass — don't mix `userId`-based and `User`-based methods going forward for new code; before starting the broader conversion, finish confirming no repository requires `User`-specific fields beyond the ID (see exception case above).

---

## 10. Architectural Direction — Rich Domain Model (DDD-style)

Decision made while working on `PercentageDay.updateRate()`: the project will lean toward a **rich domain model** (entities protect their own invariants via named business methods) rather than an anemic model (entities as plain data holders with public setters + validation living entirely in services). This is a deliberate, project-wide style choice — not a one-off fix for `PercentageDay`.

- ✅ `PercentageDay.updateRate(BigDecimal)` — kept as the domain method; validation lives here, not duplicated in the service.
- ⬜ Remove `@Setter` from `PercentageDay.rate` — a public setter bypasses `updateRate()`'s validation entirely, which defeats the point. The setter needs to go for this pattern to actually hold.
- ⬜ Remove the now-duplicate rate validation from the service layer (`PercentageDayService` update path) — call `existing.updateRate(...)` only, let the entity throw if invalid.
- ⬜ **Larger, deferred pass:** identify other fields across entities that have a business rule attached (not just plain data) — e.g. `User.emailVerified`, `HabitCompletion.complete` — and consider replacing their public `@Setter` with named domain methods (`markEmailVerified()`, `markComplete()`/`markIncomplete()`) instead of generic setters. `HabitCompletion` already has `markComplete()`/`markIncomplete()` methods from earlier — check whether it *also* still has a public `@Setter` on `complete` that bypasses them the same way `PercentageDay.rate` did.
- ⬜ Do **not** start this broader pass until the current Auditing migration and Exception Handling work (Sections 2–3) are finished — this is a project-wide style decision and deserves its own dedicated pass, not something mixed into the current layer-by-layer refactor.

---

## 11. Deferred / Not Yet Decided

- ✅ **AOP** — activated. Concrete use case identified: cross-cutting logging (method entry/exit, execution time, failure logging) across all service-layer methods, replacing what would otherwise be repetitive manual `log.info`/`log.error` calls in every service method. Implementation: a single `@Aspect` with an `@Around` advice on `execution(* com.mts.aadati.services.*.*(..))`. Scope is intentionally limited to logging — this is not a general-purpose AOP adoption for business logic, validation, or security concerns, which stay in their existing dedicated layers (services, `@RestControllerAdvice`, Spring Security). Ensure the aspect's logging doesn't duplicate what `GlobalExceptionHandler` already logs for exception cases — the aspect covers method entry/exit and success/failure timing, the handler covers the exception details themselves.

- ⬜ **`Set` vs `List` for collections + `equals`/`hashCode`** — deferred. Needed only if a query ever requires `JOIN FETCH` on two `@OneToMany`/`@ManyToMany` collections on the same entity at once (triggers `MultipleBagFetchException` with `List`). Switching to `Set` requires: (1) updating the affected entity field type, (2) updating any repository/service code that depends on `List` ordering, (3) implementing `equals`/`hashCode` on the entity based on ID with a constant `hashCode()` (not ID-based) to avoid breaking `Set` semantics before the entity is persisted. Not worth doing preemptively — revisit only when a real dual-collection fetch need appears (see `HabitTask`/`HabitCompletion` fetch discussion in JPQL-Join-Fetch-Rules.md).

- ⬜ **`AuthenticationServiceException` cleanup** — ensure the misused `AuthenticationServiceException` throw sites in `AuthenticationService` are migrated to the new `InvalidCredentialsException`/`ResourceNotFoundException`/`OperationFailedException` classes (see Section 3, now designed in `Aadati-Exception-Design.md` and logged in `REFACTORING_LOG.md`).


- ⬜ **`createdBy`/`updatedBy` auditing** — explicitly decided **against** for now (Aadati has no admin-editing-other-users'-data scenario that would need it). Only revisit if that scenario actually appears in the product.
- ⬜ Consider `google-java-format` vs. keeping IntelliJ's default 4-space formatting — decided to **keep the 4-space IntelliJ default**, no plugin needed.
- ⬜ Decide on `lombok.config` with `copyableAnnotations` for `@Qualifier` on `JwtAuthenticationFilter`'s `HandlerExceptionResolver` field — needed if `@RequiredArgsConstructor`/`@AllArgsConstructor` is used on that class (confirm which constructor style it currently uses, since `@Qualifier` on a field is silently dropped otherwise and can cause a `NoUniqueBeanDefinitionException` at startup).

---

## 12. Once All of the Above Is Done

- ⬜ Deploy the finished app to a real cloud environment (Railway or similar) so the portfolio links to something live, not just source code.
- ⬜ Update GitHub README / CV / LinkedIn with the live URL once deployed.
- ⬜ Write LinkedIn posts summarizing key refactoring decisions, using `REFACTORING_LOG.md` entries as source material (one post per related group of changes, not one per entry).

---

<!--
When an item here is completed:
1. Check it off (⬜ → ✅) or remove it.
2. If it was a significant architectural decision, add a full [Fix]/[Adopt] entry to REFACTORING_LOG.md with Problem/Decision/Why/Impact and sourced links.
3. Small mechanical fixes (e.g. removing one unused import) don't need a log entry — just check them off here.
-->