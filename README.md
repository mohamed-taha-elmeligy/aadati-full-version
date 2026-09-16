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

- **Finding real bugs, not just style issues.** A live database password committed in plaintext, a JWT `expiresIn` value that didn't match the actual token lifetime, silent duplicate-check logic that could never fire, cleanup jobs that were one copy-paste away from deleting the wrong table's data — these were caught and fixed, not assumed away.
- **Replacing intuition with sourced decisions.** Every non-trivial choice — exception design, JPA auditing, the mapping strategy between DTOs and entities, transaction handling — is backed by an official source (Spring's own docs, OWASP, RFC 7807) rather than "this felt right." See `Governed-Refactoring-Standards.md` for the standards used and `REFACTORING_LOG.md` for how each one was applied.
- **Fixing one layer at a time.** Early on, changing entities, DTOs, and mappers simultaneously caused a cascade of hard-to-trace compile errors. The rest of the refactor followed a stricter rule: finish one architectural layer (entities, then DTOs, then mappers, then repositories, then services) before touching the next.
- **Being honest about what's actually needed.** Not every "best practice" got applied — several were deliberately rejected (e.g. `createdBy`/`updatedBy` auditing, a dedicated rate-limit exception) because the project has no real use case for them yet. Adding infrastructure for a scenario that doesn't exist is its own kind of technical debt.

The result is documented as a paper trail, not just a diff: `REFACTORING_LOG.md` covers what changed and why, and `Aadati-BACKLOG.md` tracks what's still in progress.

### Core Features

- **Habits** — recurring behaviors scheduled by day of week, with categories and completion tracking
- **Habit Tasks** — one-off or prioritized tasks linked to habits, with recurrence types and priority levels
- **Progress Tracking** — daily and weekly completion percentage calculation (`PercentageDay`, `PercentageWeek`) with grade classification
- **Calendar Engine** — automatic generation of daily/weekly calendar structure (`HabitWeek`, `HabitCalendar`)
- **Role-based Authentication** — JWT-based auth with role/permission separation between regular users and admins
- **Automated Cleanup** — scheduled jobs to prune old completion records while preserving long-term calendar structure

### Tech Stack

- **Language / Framework:** Java 21, Spring Boot
- **Persistence:** Spring Data JPA, PostgreSQL
- **Security:** Spring Security, JWT
- **Mapping:** MapStruct
- **Build:** Maven

### Engineering Highlights

This project isn't just "it works" — it's an exercise in doing things the way a production codebase should:

- Centralized exception hierarchy (`ResourceNotFoundException`, `DuplicateResourceException`, etc.) mapped to RFC 7807 `ProblemDetail` responses
- JPA Auditing instead of duplicated `@PrePersist`/`@PreUpdate` boilerplate across entities
- Consistent `userId`-based repository queries instead of loading full entities just to filter by identity
- Race-condition-safe uniqueness checks (database constraint + translated exception, not just a pre-check)
- Scheduled cleanup jobs sized to each entity's actual growth rate, not a one-size-fits-all retention policy

See [`REFACTORING_LOG.md`](./REFACTORING_LOG.md) for the full reasoning behind each of these, with sources.

---

## Getting Started

`[TBD — setup instructions once Docker/CI work is finished]`

```bash
# Clone the repo
git clone https://github.com/mohamed-taha-elmeligy/aadati-full-version.git
cd aadati-full-version

# Run with Maven
./mvnw spring-boot:run
```

---

## Project Status

This project is under active refactoring. See [`Aadati-BACKLOG.md`](./Aadati-BACKLOG.md) for what's done and what's still in progress.

---

## License

This project is licensed under the MIT License — see the [LICENSE](./LICENSE) file for details.
