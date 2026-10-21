# Aadati

A backend-first habit and task tracking API built with Spring Boot, designed to demonstrate production-grade backend engineering practices — not just CRUD.

---

## Vision

Aadati is designed for anyone who wants to build better habits, organize daily activities, and understand their progress over time.

It helps users turn personal goals and routines into measurable actions by combining recurring habit tracking, task management, and daily and weekly progress tracking.

### What Makes Aadati Different

Aadati focuses on **consistency and measurable progress**, rather than simply recording whether something was completed.

Users can create recurring habits based on their weekly routines, manage related or independent tasks, and track their completion percentage over time. Daily and weekly progress metrics provide a broader picture of consistency than a simple checklist or streak alone.

---

## About This Project

Aadati started as a first solo backend project, built without a mentor, to learn Spring Boot end to end. Once the core features worked, the project moved into a deliberate refactoring phase — not because the original code was broken, but because "it runs" and "it's built the way a production system should be" are two different bars, and closing that gap on purpose is where the real learning happened.

### Why This Was Refactored

The goal wasn't to rewrite the project from scratch — it was to take a working backend and systematically raise it to a standard that would hold up in an actual codebase review, while documenting every decision along the way. Concretely, that meant:

- **Finding real bugs, not just style issues.** A JWT secret with no default that could silently run with an empty value, a logging category with a capitalization typo that meant debug logs were never actually emitted, a self-healing reconciliation job that would have re-inserted all of its managed rows on every single run because of a missing `equals()`/`hashCode()` override, an admin account whose password was being persisted without ever calling a `PasswordEncoder` — these were caught and fixed, not assumed away.

- **Replacing intuition with sourced decisions.** Every non-trivial choice — exception design, JPA auditing, the mapping strategy between DTOs and entities, transaction handling, the choice to build a small custom job-orchestration engine instead of adopting Spring Batch — is backed by an official source (Spring's own docs, OWASP, RFC 7807) rather than "this felt right." See `Governed-Refactoring-Standards.md` for the standards used and [`REFACTORING_LOG.md`](./REFACTORING_LOG.md) for how each one was applied.

- **Fixing one layer at a time.** Early on, changing entities, DTOs, and mappers simultaneously caused a cascade of hard-to-trace compile errors. The rest of the refactor followed a stricter rule: finish one architectural layer (entities, then DTOs, then mappers, then repositories, then services) before touching the next.

- **Being honest about what's actually needed.** Not every "best practice" got applied — several were deliberately rejected (e.g. `createdBy`/`updatedBy` auditing, a dedicated rate-limit exception) because the project has no real use case for them yet. Adding infrastructure for a scenario that doesn't exist is its own kind of technical debt. The same honesty applies to this README: features below are described as they actually exist today, not as they're planned to exist.

The result is documented as a paper trail, not just a diff: [`REFACTORING_LOG.md`](./REFACTORING_LOG.md) covers what changed and why, and [`Aadati_BACKLOG.md`](./Aadati-BACKLOG.md) tracks what's still in progress.

### Core Features

- **Habits** — recurring behaviors scheduled by day of week, with categories and completion tracking.

- **Habit Tasks** — one-off or recurring tasks linked to habits, with recurrence types (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`, `ONCE`) and priority levels.

- **Progress Tracking** — daily and weekly completion percentage calculation (`PercentageDay`, `PercentageWeek`) with grade classification.

- **Orchestration Engine** — a small, custom-built job-orchestration system (not Spring Batch — see `REFACTORING_LOG.md` for why) that drives calendar generation, completion-row generation, and reference-data integrity. Dependency-aware job ordering (topological sort), bounded retry, and full execution history (`job_run`) are built in, not bolted on. Documented in detail in `ENGINE_DESIGN_NOTES.md`.

- **Role-based Authentication** — JWT-based auth with role/permission separation between regular users and admins, including refresh-token support.

### Tech Stack

- **Language / Framework:** Java 21, Spring Boot 3

- **Persistence:** Spring Data JPA, PostgreSQL (production), H2 (local development), Flyway migrations

- **Caching:** Redis (Spring Data Redis, Lettuce)

- **Security:** Spring Security, JWT

- **Mapping:** MapStruct

- **API Documentation:** springdoc-openapi (Swagger UI)

- **Observability:** Spring Boot Actuator, Micrometer, Prometheus

- **Build:** Maven

### Engineering Highlights

This project isn't just "it works" — it's an exercise in doing things the way a production codebase should:

- Centralized exception hierarchy (`ResourceNotFoundException`, `DuplicateResourceException`, etc.) mapped to RFC 7807 `ProblemDetail` responses.

- JPA Auditing (`@CreatedDate`/`@LastModifiedDate` via a shared `@MappedSuperclass`) replacing duplicated `@PrePersist`/`@PreUpdate` boilerplate, rolled out entity by entity.

- Consistent `userId`-based repository queries instead of loading full entities just to filter by identity.

- Race-condition-safe uniqueness checks (database constraint + translated exception, not just a pre-check).

- A custom orchestration engine built deliberately instead of adopting Spring Batch, with an explicit `dependsOn()` dependency graph, bounded retry, and resume-based generation that only regenerates what's missing rather than reprocessing from scratch.

- Redis caching rolled out across every read-heavy service, with several real caching bugs (serialization, cache-key collisions, stale-filter mismatches) caught and fixed along the way, not just `@Cacheable` applied blindly.

- Cross-cutting request/service logging via a single AOP aspect instead of repetitive manual `log.info`/`log.error` calls scattered through every service method.

- Environment-separated configuration: dev and prod run against entirely separate Spring profiles (H2 vs. PostgreSQL, verbose vs. quiet logging), with every secret-bearing property resolved from an environment variable with no insecure fallback default in production.

See [`REFACTORING_LOG.md`](./REFACTORING_LOG.md) for the full reasoning behind each of these, with sources.

---

## Getting Started

```bash
# Clone the repo
git clone https://github.com/mohamed-taha-elmeligy/aadati-full-version.git
cd aadati-full-version

# Run with Maven (defaults to the "dev" profile — in-memory H2, no setup required)
./mvnw spring-boot:run
```

By default the app runs under the `dev` Spring profile (H2 in-memory database, verbose logging) with no configuration needed. To run against PostgreSQL/Redis instead, set `SPRING_PROFILES_ACTIVE=prod` along with the required environment variables (database credentials, JWT secret, admin credentials, etc.) — see `application-prod.properties` for the full list.

`[Docker/CI setup instructions once that work is finished — see REFACTORING_BACKLOG.md]`

---

## Project Status

This project is under active development: core feature work and refactoring are both ongoing, with the orchestration engine being the current focus. See [`Aadati_BACKLOG.md`](./Aadati-BACKLOG.md.md) for what's done and what's still in progress, and [`REFACTORING_LOG.md`](./REFACTORING_LOG.md) for the history of everything already shipped.

---

## License

This project is licensed under the MIT License — see the [LICENSE](./LICENSE) file for details.
