# Aadati

A backend-first habit and task tracking API built with Spring Boot, designed to demonstrate production-grade backend engineering practices, not just CRUD.

Users create recurring habits and tasks, complete them day by day, and get daily and weekly completion percentages that show consistency over time, not just a checklist or a streak.

---

## Tech Stack

| Area                 | Technology                                                               |
|----------------------|--------------------------------------------------------------------------|
| Language / Framework | Java 21, Spring Boot 3.5                                                 |
| Persistence          | Spring Data JPA, PostgreSQL (prod), H2 (dev), Flyway migrations          |
| Caching              | Redis (Spring Data Redis, Lettuce)                                       |
| Security             | Spring Security, stateless JWT (access + refresh), Redis token blocklist |
| Rate limiting        | Bucket4j backed by Redis                                                 |
| Mapping              | MapStruct, Java records for DTOs                                         |
| API docs             | springdoc-openapi (Swagger UI)                                           |
| Observability        | Spring Boot Actuator, Micrometer, Prometheus                             |
| Build                | Maven (wrapper included)                                                 |

---

## Getting Started

```bash
git clone https://github.com/mohamed-taha-elmeligy/aadati-full-version.git
cd aadati-full-version

# Runs with the "dev" profile: in-memory H2, verbose logging, no setup required
./mvnw spring-boot:run
```

- Base URL: `http://localhost:8082/aadati/api`
- Swagger UI: `http://localhost:8082/aadati/api/swagger-ui.html`

To run against PostgreSQL and Redis, set `SPRING_PROFILES_ACTIVE=prod` and provide the required environment variables (database credentials, JWT secret, admin credentials, and so on). See `.env.example` and `application-prod.properties` for the full list. Production has no insecure fallback defaults for secrets.

> Docker / docker-compose and CI are planned, see the [backlog](./docs/Aadati-BACKLOG.md).

---

## Architecture

At runtime a request flows through the following layers:

```
Request -> Security filter chain -> Controllers -> Services -> Repositories -> PostgreSQL
                                                      |
                                                      +-> Redis (cache, JWT blocklist, rate-limit buckets)
                                                      +-> Domain events -> async provisioning -> percentage recalculation
```

- **Security:** stateless chain with CSRF disabled. The JWT filter runs first, then `RateLimitFilter` (so the authenticated user is already known when the rate-limit policy and key are resolved), then role-based authorization. `/auth/**` is public, `/user/**` requires USER or ADMIN, `/admin/**` requires ADMIN. Method security is enabled.
- **Tokens:** access and refresh JWTs carry a `jti`. Logout writes the `jti` to a Redis blocklist, so a token can be revoked before it expires.
- **Rate limiting:** Bucket4j with three policies: `AUTH` (5/min, per IP), `API` (100/min, per user), `ANONYMOUS` (20/min, per IP). It fails open if Redis is unreachable, so an outage degrades limiting instead of taking the API down.
- **Caching:** `RedisCacheManager` with per-cache TTLs (5 minutes by default), transaction-aware, falling back to the database if Redis is unavailable.
- **Events:** habit and completion changes publish domain events. A listener runs `@Async` with `@TransactionalEventListener(AFTER_COMMIT)`, creates any missing rows, and recalculates the day/week percentages in separate transactions.
- **Schema:** owned by Flyway. JPA runs with `ddl-auto=validate`.
- **Orchestration engine:** a custom job engine (topological ordering, bounded retry, full run history in `job_run`) generates calendar and completion rows and runs cleanup jobs. See [`docs/ENGINE_DESIGN_NOTES.md`](./docs/ENGINE_DESIGN_NOTES.md).

Diagrams:

- [Runtime architecture](./docs/architecture/aadati-runtime-architecture.html): request flow, security, caching, async events, job engine
- [Code dependency graph](./graphify-out/graph.html): generated with [Graphify](https://github.com/Graphify-Labs/graphify), with a plain-language summary in [`GRAPH_REPORT.md`](./graphify-out/GRAPH_REPORT.md)

Both diagrams are tool-generated from the codebase. Download the HTML files and open them locally, since GitHub shows them as source.

---

## Core Features

- **Habits:** recurring behaviors scheduled by day of week, with categories and completion tracking.
- **Habit Tasks:** one-off or recurring tasks linked to habits, with recurrence types (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`, `ONCE`) and priority levels.
- **Progress Tracking:** daily and weekly completion percentages (`PercentageDay`, `PercentageWeek`) with grade classification.
- **Orchestration Engine:** a small custom job system, deliberately not Spring Batch (the reasoning is in the refactoring log).
- **Role-based Authentication:** JWT auth with refresh tokens, separate permissions for users and admins, and admin-only role management.

---

## Engineering Highlights

- **Error handling:** a custom exception hierarchy mapped to RFC 7807 `ProblemDetail` responses by a single `GlobalExceptionHandler`.
- **Auditing:** JPA Auditing through a shared `@MappedSuperclass` instead of duplicated `@PrePersist`/`@PreUpdate` code.
- **Race-safe uniqueness:** a database constraint plus a translated exception, not only a pre-check.
- **Caching done carefully:** Redis caching across read-heavy services, with real bugs found and fixed along the way (serialization of `Page<T>`, cache-key collisions, stale existence guards).
- **Security fixes:** refresh token removed from the URL path, a password leak in `CustomUserDetails` closed, JWT authorities resolved from the database rather than duplicated in token claims.
- **Logging:** one AOP aspect for request and service logging instead of manual `log.info` calls scattered across services.
- **Configuration:** separate dev and prod Spring profiles, with every secret resolved from environment variables.
- **Mapping:** MapStruct for Entity/DTO conversion and Java records for immutable DTOs.

The reasoning behind each item, with sources, is in the [Refactoring Log](./docs/REFACTORING_LOG.md).

---

## About This Project

Aadati started as a first solo backend project, built without a mentor, to learn Spring Boot end to end. Once the core features worked, it moved into a deliberate refactoring phase. The code wasn't broken, but "it runs" and "it's built the way a production system should be" are two different bars, and closing that gap on purpose is where the real learning happened.

How that refactor was done:

- **Finding real bugs, not just style issues.** A JWT secret with no default that could silently run empty, a logging category with a capitalization typo that meant debug logs were never emitted, a reconciliation job that would have re-inserted all its managed rows on every run because of a missing `equals()`/`hashCode()`, and an admin password persisted without a `PasswordEncoder`. All were caught and fixed.
- **Replacing intuition with sourced decisions.** Every non-trivial choice (exception design, JPA auditing, DTO/entity mapping, transaction handling, building a small engine instead of adopting Spring Batch) is backed by an official source such as Spring docs, OWASP, or RFC 7807. See [Refactoring Log](./docs/REFACTORING_LOG.md) for how each was applied.
- **One layer at a time.** Early on, changing entities, DTOs, and mappers together caused hard-to-trace compile errors. The rest of the refactor finished one layer (entities, DTOs, mappers, repositories, services) before touching the next.
- **Honesty about what's needed.** Several best practices were deliberately rejected (for example `createdBy`/`updatedBy` auditing and a dedicated rate-limit exception) because the project has no real use for them yet. This README describes features as they exist today, not as they are planned.

---

## Documentation

- [Refactoring Log](./docs/REFACTORING_LOG.md): every V2 decision with problem, decision, why, and impact
- [Backlog](./docs/Aadati-BACKLOG.md): what is done and what is still in progress
- [Coding Rules](./docs/Aadati-RULES.md)
- [Engine Design Notes](./docs/ENGINE_DESIGN_NOTES.md)

---

## Project Status

Under active development. Core features and refactoring are both ongoing. The [backlog](./docs/Aadati-BACKLOG.md) tracks what is in progress, and the [Refactoring Log](./docs/REFACTORING_LOG.md) records everything already shipped.

---

## License

MIT License. See the [LICENSE](./LICENSE) file for details.