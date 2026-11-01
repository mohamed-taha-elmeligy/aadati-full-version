# Aadati — Orchestration/AutoGenerate Engine Design Notes

Reference notes for the orchestration ("Engine") layer — deliberately deferred until the Repository/Service/Controller pass was finished (see `REFACTORING_BACKLOG.md` §12). This file captures the general background on "engine" design in software, the build-vs-framework decision, and the **actual architecture being implemented**. Code examples below are real, reviewed implementations, not sketches — but several (marked below) are pasted chat content from the user only, never seen as uploaded files, and are recorded here as the best-known current state, not a verified compile.

---

## 1. What a "Software Engine" Is, Generally

A software engine is an independent component that drives core processing according to rules/data, decoupled from presentation (UI/HTTP) concerns — it should keep working even if the layer that calls it changes completely.

**Common categories** (for context, not all relevant to Aadati):
- **Rule Engine** — applies business rules to data (Drools, discount logic).
- **Workflow/Orchestration Engine** — coordinates steps across multiple systems/services (Camunda, Temporal, Airflow). **Closest category to Aadati's Engine.**
- **Calculation Engine** — computes derived values from changing inputs (pricing systems).
- **Scheduling/Generation Engine** — auto-generates periodic data (billing cycles). **Also directly relevant to Aadati.**
- **Template/Rendering Engine** — data + template → output (Thymeleaf).
- **Search/Recommendation Engine** — ranks/filters results (Elasticsearch).

**Shared design principles across all of them:**
1. **Separate the trigger from the logic** — what causes it to run (schedule, event, startup) must be independent of what it does, so either can change without touching the other.
2. **Idempotency is non-negotiable** — anything that runs automatically/periodically must tolerate being re-run without duplicating or corrupting data. This is the single most important property for Aadati's Engine specifically.
3. **Composition, not duplication** — the engine orchestrates existing Services; it does not reimplement their business logic.
4. **Clear, independently testable pipeline steps.**
5. **Observability from day one** — nothing calls an engine directly expecting an HTTP response, so logging/execution history is the only way to know what happened.
6. **Explicit handling of partial failure** — decide deliberately whether a failure halts everything, halts only dependents, or is skipped and logged.

**Common patterns used to build them:** Strategy (swappable algorithms), Pipeline/Chain of Responsibility (sequential steps), Command (loggable/retryable actions), Scheduler + Idempotent Job (the dominant Spring pattern), State Machine (not required here).

---

## 2. Build vs. Adopt Spring Batch — Decision (unchanged)

**Decision: build a custom, lightweight engine instead of adopting Spring Batch.**

**Why:** Aadati's generation/calculation workload is small-scale and periodic (weekly calendar/week rows, daily/weekly percentage skeleton rows, daily task/habit completion rows) — not large-batch ETL processing millions of records. Pulling in Spring Batch's full machinery (~7 auto-created metadata tables, `Job`/`Step`/`Chunk` vocabulary) for this workload is infrastructure for a scenario that doesn't exist here — the same "What NOT to Build" principle from `ENGINEERING_RULES.md` §5.

**Borrowed from Spring Batch anyway, in lightweight form:** a small execution-history table gives the same restart/visibility value `JobRepository` provides, without the operational weight — see `JobRunStore`/`JobRun` below (interface finalized; concrete JPA-backed implementation still unwritten — see §6).

---

## 3. Architecture (current, implemented) — confirmed package layout (verified directly against the IDE project tree)

```
com.mts.aadati.engine                  → Engine, EngineDates (placement of these two at the bare
                                          package root, rather than inside core, is a known open
                                          inconsistency — see note below)
com.mts.aadati.engine.app.generate     → Generator<T> implementations (WeekGenerator, DayGenerator,
                                          PercentageWeekGenerator, PercentageDayGenerator,
                                          TaskCompletionGenerator, HabitCompletionGenerator)
com.mts.aadati.engine.app.calculate    → CompletionChanged, CompletionListener,
                                          PercentageCalculationService — event-driven percentage
                                          recalculation; NOT a Job/Generator/Calculator under the
                                          cron/Engine orchestration path at all — see §3a below.
com.mts.aadati.engine.app.history      → JobRunEntity, JobRunRepository, JpaJobRunStore — concrete
                                          JPA-backed implementation of JobRunStore.
com.mts.aadati.engine.configs          → ClockConfig, SchedulerConfig — actual `@Configuration`
                                          bean-wiring classes only.
com.mts.aadati.engine.core.domain      → JobRun, JobRunStore, RunStatus.
com.mts.aadati.engine.core.generate    → Generator (interface only; implementations live in app.generate).
com.mts.aadati.engine.core.job         → Cron, Job, JobIds, JobRegistry, JobScheduler.
```

**Organizing principle confirmed from the real tree:** `core` holds pure contracts/domain types (interfaces, enums, constants shared by both producers and consumers), `app` holds concrete implementations of those contracts, `configs` holds only actual Spring `@Configuration` classes. `Cron` and `JobIds` were both moved into `core.job` during this round specifically to match this principle (they were briefly misplaced in `app`/`configs` in an earlier snapshot of the tree) — confirmed fixed.

**Still-open placement inconsistency (flagged, not yet acted on):** `Engine` and `EngineDates` both sit bare at the `engine` package root rather than inside `core` — `EngineDates` in particular is a pure utility (`horizon(LocalDate)`) consumed by `app.generate` implementations, matching the exact shape of `Cron`/`JobIds` which were already relocated into `core.job` for this reason. Recommended (not yet confirmed): move `EngineDates` into `core` or `core.generate`; `Engine` itself arguably belongs in `core` or `core.job` alongside `JobScheduler`/`JobRegistry`, which it orchestrates.

### Naming — final

An earlier draft of this document used `JobGraphResolver` and (at one point) `JobEngine` as component names. **Both were revisited during implementation; the names below are the confirmed final ones:**

| Earlier draft name | **Final name** |
|---|---|
| `JobGraphResolver` | **`JobRegistry`** |
| `JobEngine` (an intermediate name used briefly to avoid a too-generic name) | **`Engine`** — confirmed final by the user; the too-generic-name concern was raised and explicitly overridden. |
| (no equivalent) | **`JobIds`** — centralized `String` id constants (mirrors the existing `Uri` constants convention). **Standing rule: every job-id/dependency reference goes through `JobIds.XXX`, never a hardcoded string literal.** Lives in `core.job`. |

### `onError` — kept, not dropped (correction to an earlier draft decision)

An earlier draft of this document said `onError()` was "dropped from `Job`/`Generator`/`Calculator` entirely." **This was superseded.** `onError(Exception, int)` is a `default void onError(Exception e, int attempt) {}` **no-op hook on `Job`** — purely an optional side-effect notification point invoked by `Engine` between failed retry attempts, wrapped in `safely()` (so a buggy `onError` override can never itself crash the retry loop). It carries **zero retry-management responsibility** — retry counting, backoff, and give-up logic are entirely internal to `Engine`. A `Generator`/`Calculator` implementation never catches its own exceptions or retries internally; it just does the work and lets exceptions propagate.

### Components

| Component | Responsibility |
|---|---|
| `Job` | Base contract the `Engine` depends on: `id()` (via `JobIds`), `dependsOn()`, `cron()`, `onError(Exception, int)` (default no-op), `execute()`. |
| `Generator<T>` | Extends `Job`. Adds `before()/generate()/after()` for "create new (skeleton) rows" work — the only shape actually needed for this project's recurring-row generation. Provides its own `default execute()` wiring its steps together — `Job.execute()` is the only method `Engine` actually calls. |
| `JobRegistry` | Collects all `Job` beans (Spring auto-injects `List<Job>`), builds the dependency graph from `dependsOn()`, topologically sorts via **Kahn's algorithm**. Runs **once, eagerly, at startup** — a circular or missing dependency fails loudly at boot, not mid-cycle. **Finalized, closed, no outstanding review items.** |
| `JobRunStore` / `JobRun` | Persistence/history **abstraction** (interface + plain domain object, `engine.core.job.domain`). `Engine` depends only on this interface, **never directly on JPA entities/repositories** — the concrete JPA-backed implementation belongs in a separate package (proposed `engine.history`), not yet written. A future JPA entity must **not** be named `JobRun` (collision risk with the domain interface's companion type) — use e.g. `JobRunEntity`/`GenerationRun` instead. |
| `Engine` | Takes the sorted list from `JobRegistry`. On every `runCycle()` call runs the **entire** sorted list (self-healing, per-job `before()` gating), not just a "due" job. Owns retry, failure recording via `JobRunStore`, and dependent-skipping (a failure skips only transitive dependents; independent jobs still run). Stays free of Spring trigger/scheduling annotations — those live on `JobScheduler`. |
| `JobScheduler` | For each `(job, cron)` pair, registers a `TaskScheduler.schedule(runnable, new CronTrigger(cron.expression()))` whose callback calls `engine.runCycle()` (**fixed** — an earlier draft callback mistakenly called a single-job `run(job)`; see §6 for confirmation status). Also owns the `@EventListener(ApplicationReadyEvent.class)` application-startup trigger (by rule, this must live here, not on `Engine`). |
| ~~`JobTrigger` (routed through `Engine.run(Job job)`)~~ | **Superseded, see §5b.** The idea of routing same-day/single-entity creation through the Engine (`Engine.run(Job job)` for one job) was replaced with a fully independent mechanism (`CompletionProvisioner` + `ProvisioningTrigger`) that never touches `JobRegistry`/`Engine`/`JobScheduler` at all — see §5b for the final design and the reasoning. The Engine stays exclusively cron/periodic; nothing event-driven lives inside it. |
| `SchedulerConfig` | Defines the `engineTaskScheduler` bean: `ThreadPoolTaskScheduler` with `poolSize=1`, serializing all cron-triggered executions on one thread to prevent concurrent same-instant `runCycle()` calls. An additional `AtomicBoolean` guard inside `Engine.runCycle()` is still recommended as defense-in-depth on top of this. |

### Why "run the full list on every trigger" is the key resilience property

Every trigger source (a specific job's cron firing, or the app's startup run) converges on `Engine`'s orchestration — the Engine has exactly one entry point, `runCycle()`, and no event-driven path of its own (see §5b for why same-day/single-entity visibility is handled entirely outside the Engine instead). For the cron path, `runCycle()` walks the **entire** topologically-sorted job list — not just the job whose cron matched. Each `Job`'s own `before()` is what decides whether there's actually anything to do (returns empty if already caught up). This means:
- A job that missed its own scheduled run (crash, deploy window, bug) gets picked up and retried the next time **any** job's cron fires — not just at its next scheduled tick, which for a `YEARLY` job could otherwise mean waiting up to a year.
- No separate "catch-up" mechanism is needed — it falls out naturally from combining "run everything" with "`before()` is a cheap no-op gate."

### Single Source of Truth

The database is the only source of truth for "what's already been generated." `before()` always re-derives the resume point from the DB — never from in-memory/instance state. This is also why there must be **no mutable instance fields** on any `Job` implementation: they're Spring singletons (`@Component`), and any state that needs to cross method calls must flow through explicit method parameters/return values, never a field (this exact mistake was caught once in `TaskCompletionController` earlier in this project, and again in an early `DayGenerator` draft before being corrected).

### Transactional guardrails (standing rule)

- History-recording calls (`JobRunStore.failed()` especially) must never be caught inside the same transaction as the operation they're recording — via `@Transactional(REQUIRES_NEW)` on the recorder's own methods, or by never wrapping `Engine`'s orchestration methods in an outer `@Transactional`.
- `markStaleRunsAsFailed()` — **caller and staleness threshold explicitly deferred by the user** (Agile-style, decide when they get there). Do not assume a value or a caller for either.

---

## 4. Interfaces (current, finalized shape)

```java
public interface Job {
    String id();                        // via JobIds, never a hardcoded literal
    List<String> dependsOn();           // default: List.of() — no dependency
    List<Cron> cron();                  // a job may have more than one trigger schedule
    default void onError(Exception e, int attempt) {}   // optional no-op hook, wrapped in safely()

    void execute();                      // the ONLY method Engine actually calls
}
```

```java
public interface Generator<T> extends Job {
    Optional<LocalDate> before();              // resume point from the DB; empty = nothing to do
    List<T> generate(LocalDate startDate);     // pure generation logic from startDate to horizon/today
    void after(List<T> generated);             // batch persistence (single save-all, no-op on empty list)

    @Override
    default void execute() {
        before().ifPresent(start -> after(generate(start)));
    }
}
```

**`Calculator<T>` — abandoned, not just "not yet written."** The original plan was a `Calculator<T>` interface mirroring `Generator<T>`'s shape, under the same `Job`/cron/`Engine` orchestration, for "recompute an existing value" work (e.g. the actual `PercentageDay`/`PercentageWeek` percentage calculation). This has been **superseded by an event-driven design** confirmed in the real project tree (`engine.app.calculate`): a `CompletionChanged` event (record), published when a completion's state actually changes, is picked up by `CompletionListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)` — confirmed, so the listener only ever reacts to a change that has actually durably committed, never to one that might still roll back), which invokes `PercentageCalculationService` to recompute the affected value immediately. This is a better fit than a cron-driven `Calculator<T>` would have been: a percentage has no reason to be recalculated on a schedule — it only needs to change when its underlying completions change, so reacting to the state-change event directly is both simpler and lower-latency than periodic recomputation. **`app.calculate`'s classes do not implement `Job`/`Generator`/any cron-orchestrated contract at all** — they sit entirely outside the `JobRegistry`/`Engine`/`JobScheduler` cron path. Internals of `CompletionChanged`/`CompletionListener`/`PercentageCalculationService` not yet reviewed in detail (only their existence, names, and the `AFTER_COMMIT` phase are confirmed so far).

```java
public enum Cron {
    EVERY_MINUTE("0 * * * * *"),   // testing only
    HOURLY("0 0 * * * *"),
    DAILY("0 0 0 * * *"),
    WEEKLY("0 0 0 * * MON"),
    MONTHLY("0 0 0 1 * *"),
    YEARLY("0 0 0 1 1 *");

    private final String expression;
    Cron(String expression) { this.expression = expression; }
    public String expression() { return expression; }
}
```

**Role mapping to familiar vocabulary** (Spring Batch, for anyone reading this who knows it):

| Aadati concept | Spring Batch equivalent |
|---|---|
| `before()` | `ItemReader` |
| `generate()` / `process()` | `ItemProcessor` |
| `after()` | `ItemWriter` |
| `dependsOn()` + topological sort | `Job` flow / `Step` ordering |
| `JobRun` / future `GenerationRun` table | `JobRepository` metadata |
| `JobScheduler` | `JobLauncher` + trigger |

---

## 5. Reviewed Implementations

### `EngineDates` (shared utility — prevents horizon drift between generators)

Single shared source for "how far ahead do we generate" (`EngineDates.horizon(LocalDate today)`), currently Jan 1 of `today`'s year + 1, aligned with the `YEARLY` cron. **Standing rule:** all generation-horizon computation goes through this one method — never computed independently per generator.

**Exception to the horizon rule:** `PercentageWeekGenerator`/`PercentageDayGenerator` and the two completion generators (`TaskCompletionGenerator`, `HabitCompletionGenerator`) intentionally stop at `today`, not `EngineDates.horizon(...)` — you can't have a percentage, or a completion checkbox, for a day/week that hasn't happened yet. Confirmed intentional, not a bug, for all four.

### `WeekGenerator implements Generator<HabitWeek>`

- `dependsOn()` → `List.of()`.
- `before()`: resumes from `weekService.findLatest().map(w -> w.getEndWeek().plusDays(1))`, or on cold start, the Monday of the current week (`TemporalAdjusters.previousOrSame(MONDAY)`).
- `generate()`: walks `startDate` to `EngineDates.horizon(...)` in 7-day steps, using `IsoFields.WEEK_OF_WEEK_BASED_YEAR`/`WEEK_BASED_YEAR` — confirmed matching the key convention `DayGenerator` relies on.
- `after()`: `weekService.addAll(generated)` — single batch save.

### `DayGenerator implements Generator<HabitCalendar>`

- `dependsOn()` → `List.of(JobIds.WEEK_GENERATOR)`.
- `before()`: resumes from `calendarService.findLatest().map(c -> c.getDate().plusDays(1))`, or `today` on cold start. Empty once past `EngineDates.horizon(today)`.
- `generate(startDate)`: batch-fetches all `HabitWeek`s overlapping `[startDate, horizon]` in one query, builds a `"weekNumber-year"` lookup map, walks day-by-day. Throws `IllegalStateException` if a required week is missing (fail loud — can only mean an ordering/dependency bug).
- `after()`: `calendarService.addAll(generated)` — single batch save + single cache eviction.
- Injects `Clock` (needs a `@Bean Clock clock()` in config).

**Known accepted asymmetry:** on cold start, `WeekGenerator` backs up to the Monday of the current week, while `DayGenerator` starts exactly at `today`. Confirmed intentional.

### `PercentageWeekGenerator implements Generator<PercentageWeek>` — pasted by user, not yet confirmed compiling

- `dependsOn()` → `List.of(JobIds.WEEK_GENERATOR)`. `cron()` → `List.of(Cron.WEEKLY)`.
- `before()`: non-advancing resume — re-derives from `percentageWeekService.findLatestWeekStart()`, which **deliberately catches newly-registered users in the just-processed week** rather than only ever moving forward (praised design: a user who registers mid-week still gets that week's skeleton row generated).
- `generate()`: stops at `today`, not a horizon (see exception above). For each `(user, week)` pair not already present, creates a zero-value `PercentageWeek` skeleton row. **This generator only creates the skeleton row — it does not compute the actual percentage value.** The actual value is computed separately and reactively — see the `app.calculate` event-driven design in §3a/§5.
- Dependencies filled in this round:
  - `UserService.getAllUsers()` → `userRepository.findAllByIsEmailVerifiedTrue()`. **Corrected this round:** an earlier version of this method also filtered on `isDeleted=false`, but the real `users` table DDL has no `is_deleted` column at all — `User` does not support soft-delete. Confirmed: filter on `isEmailVerified=true` only (accepted as a harmless transitional state: this filter may currently return a near-empty user list since Email Verification isn't fully built yet, but the Generator only creates zero-value skeleton rows, so an empty result is harmless, not a bug).
  - `PercentageWeekService.findLatestWeekStart()` → `repository.findFirstByOrderByWeek_StartWeekDesc().map(pw -> pw.getWeek().getStartWeek())`.
  - `PercentageWeekService.findKeysBetween(start, end)` → builds `"userId|weekId"` keys. **Rewritten mid-round** from an unsafe JPQL `CONCAT()` approach to an `Object[]` projection with the key built in Java — see the new `ENGINEERING_RULES.md` §2 rule on this (composite keys must never be built via SQL-side string conversion).
  - `PercentageWeekService.addAll(List<PercentageWeek>)` → standard no-op-on-empty + `saveAll`.
  - `JobIds.PERCENTAGE_WEEK_GENERATOR` constant — flagged as missing, one-line fix given, **not yet confirmed added.**

### `PercentageDayGenerator implements Generator<PercentageDay>` — pasted by user, not yet confirmed compiling

Structurally parallel to `PercentageWeekGenerator`, keyed by date instead of week:
- `dependsOn()` → `List.of(JobIds.DAY_GENERATOR)`.
- Dependencies filled in this round:
  - `HabitCalendarService.findBetween(start, end)` → new `HabitCalendarRepository.findAllByDateBetween(...)`.
  - `PercentageDayService.findLatestDate()` → `repository.findFirstByOrderByCalendar_DateDesc().map(pd -> pd.getCalendar().getDate())`.
  - `PercentageDayService.findKeysBetween(start, end)` → `"userId|date"` keys via `Object[]` projection (same safe pattern as `PercentageWeekService`).
  - `PercentageDayService.addAll(List<PercentageDay>)` → standard no-op-on-empty + `saveAll`.
  - `JobIds.PERCENTAGE_DAY_GENERATOR` — presumed already existing (no compile error reported for it).

### `TaskCompletionGenerator implements Generator<TaskCompletion>` — pasted by user, not yet confirmed compiling

- `dependsOn()` → `List.of(JobIds.DAY_GENERATOR)`. `cron()` → `List.of(Cron.DAILY)`.
- `before()`: resumes from `completionService.findLatestDate()`, stops at `today`.
- `generate()`: for each `(HabitCalendar, HabitTask)` pair where `shouldCreate(task, date)` is true and no existing completion key matches, creates a `complete=false` skeleton row.
- `shouldCreate()` recurrence logic (ported from a pre-Engine class, `InitializationTaskCompletion` — see below) handles real calendar edge cases well:
  - `MONTHLY`: `date.getDayOfMonth() == Math.min(startDate.getDayOfMonth(), date.lengthOfMonth())` — correctly clamps a task started on the 31st to the last day of shorter months, instead of silently never matching.
  - `YEARLY`: a Feb-29 anniversary correctly falls back to Feb 28 in non-leap years.
  - `WEEKLY`: matches only the **same single day-of-week as `startDate`** — confirmed (via the old pre-Engine code, which has the identical limitation) to be a **pre-existing behavior, not a new regression** introduced by the port. Previously attributed to an unused `HabitTask.habitDayWeekIds` (plural) field — **this premise is now in doubt**: the real DDL has no junction table between `habit_task` and `habit_day_week` (unlike `habit` ↔ `habit_day_week`, which does have one — `habit_day_of_week`). Awaiting confirmation of whether `habitDayWeekIds` exists on `HabitTask` at all before treating this as an actionable backlog item — see `REFACTORING_BACKLOG.md` §12 for the caveat added this round.
  - `ONCE`: **corrected during this round's review.** The straightforward port (`date.equals(startDate)`) would silently lose a `ONCE` task's completion forever if the Engine missed its exact start date (crash/deploy window). Restored the old code's resilient semantics instead: `!completionService.existsByHabitTask(task)` (new `TaskCompletionRepository.existsByHabitTask(HabitTask)` / `TaskCompletionService.existsByHabitTask(...)`) — creates the completion the first time the Engine runs on or after the start date, whenever that is.
- Dependencies filled this round: `HabitTaskService.findAllActive()` → new `HabitTaskRepository.findAllByIsActiveTrue()` (confirmed field name `isActive`, not `isDeleted`, via the old pre-Engine code's `findByUserAndHabitTaskIdAndIsActiveTrue`/`task.deactivate()`). `TaskCompletionService.findLatestDate()`, `findKeysBetween()` (`"habitTaskId|date"`, `Object[]` projection), `addAll()` — same shapes as the Percentage services.
- `TaskCompletion.builder()` — flagged as unresolved in the user's IDE, but the pre-Engine `InitializationTaskCompletion` class already uses `TaskCompletion.builder()...build()` successfully in three places, so the entity almost certainly already supports it; the IDE error is most likely a cascading effect of the other unresolved methods in the same file, not a missing `@Builder` on the entity. Expected to self-resolve once the above methods are added — not yet confirmed.

### Pre-Engine class superseded: `InitializationTaskCompletion` / `InitializationHabitCompletion`

Both are legacy, pre-Engine classes (one fully commented out) that did this generation work manually via three separate methods each (`add...ToCompletion()`, `addLater...ToCompletion()`, `addNew...ToCompletion(entity)`). Mined for domain logic (recurrence rules, field names) while designing the `Generator<T>`-based replacements; **not themselves part of the Engine architecture** and expected to be deleted once the new generators are confirmed working. Found and fixed one real behavioral regression during the port — see `ONCE` handling above. Also surfaced, and fixed separately (see `TaskCompletionService.updateCompletion` below), a business rule this migration does **not** own: auto-deactivating a `ONCE`-type `HabitTask` once its completion is marked done.

### `TaskCompletionService.updateCompletion(...)` — fixed this round (not an Engine class, but found while tracing `TaskCompletion` dependencies)

Two problems found in the existing method and fixed:
1. **Broken/wrong duplicate check.** `repository.existsByHabitTask_User_UserIdAndAndTaskCompletionIdNot(...)` had a literal `AndAnd` typo (would fail Spring Data's derived-query parsing at startup) and, even fixed, the semantics were wrong for an update-by-ID operation (it would block a user with more than one `TaskCompletion` ever from updating any of them). The user's intent was an **ownership check**, not a duplicate check. Replaced with a direct comparison on the already-loaded entity — no extra query needed:
   ```java
   if (!completion.getHabitTask().getUser().getUserId().equals(userId))
       throw new ResourceOwnershipException(TASK_COMPLETION_NOT_OWNED_BY_USER);
   ```
   (`ResourceOwnershipException`/message constant names are placeholders pending the user's actual exception taxonomy.)
2. **Relocated the "deactivate a `ONCE` task once done" rule here**, since it's a side effect of `complete` actually flipping, not of generation. Made symmetric per the user's decision: completing a `ONCE` task's completion deactivates its parent `HabitTask`; un-completing it reactivates it (needs a new `HabitTask.activate()` counterpart to the existing `deactivate()`, and `habitTaskRepository`/`HabitTaskService` injected into `TaskCompletionService` if not already present).

### `HabitCompletionGenerator implements Generator<HabitCompletion>` — drafted this round by the assistant (not yet reviewed/compiled by the user)

Mirrors `TaskCompletionGenerator`'s shape but keyed off `HabitDayWeek` membership instead of a recurrence-type switch:
- `dependsOn()` → `List.of(JobIds.DAY_GENERATOR)`. `cron()` → `List.of(Cron.DAILY)`. Stops at `today` (same reasoning as the other completion generators).
- `generate()`: for each `HabitCalendar` in range, looks up `dayWeekService.findHabitsByDayOfWeek(calendar.getDate().getDayOfWeek().toString())` and creates a skeleton `HabitCompletion` for each `(habit, date)` pair not already present in a `findKeysBetween(...)`-derived `Set<"habitId|date">`. `.complete(false)` in the builder call is **correct as originally written** — a prior round incorrectly "fixed" this to `.completed(false)` based on a mistaken confirmation; the real `habit_completion`/`task_completion` DDL shows **both** tables use the column name `complete`, not `completed`. Reverted — see §5a for the corrected `PercentageCalculationService` getter call too.
- Needs (status unconfirmed — **two open questions from this round, not yet answered**):
  1. Whether `HabitCompletionService`/`HabitCompletionRepository` already exist from earlier controller work (just need the 3 new methods added) or need to be scaffolded from scratch.
  2. Whether a newly-created `Habit` (or `HabitTask`, same question applies to `TaskCompletionGenerator`) should get same-day completion visibility synchronously at creation time (mirroring the old `addNewHabitToCompletion`/`addNewTaskToCompletion` methods), since the daily-batch `before()` model only picks up a same-day-created entity on the **next** day's cron run otherwise.
- New methods proposed (same shape as the Percentage/Task services): `JobIds.HABIT_COMPLETION_GENERATOR`, `HabitCompletionRepository.findFirstByOrderByHabitCalendar_DateDesc()` / `findHabitIdAndDateBetween(...)` (`Object[]` projection), `HabitCompletionService.findLatestDate()` / `findKeysBetween()` / `addAll()`.

---

## 5a. `engine.app.calculate` — event-driven percentage recalculation (reviewed in full this round)

Replaces the originally-planned `Calculator<T>` interface entirely (see §6). Three classes, all pasted by the user and reviewed:

```java
public record CompletionChanged(UUID userId, LocalDate date) {}
```
Deliberately minimal payload — only the identity of what changed, not derived data. The handler re-derives everything from the DB, same "single source of truth" principle as `Generator.before()`.

```java
@Component @RequiredArgsConstructor @Slf4j
public class CompletionListener {
    private final PercentageCalculationService calculation;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CompletionChanged e) {
        try {
            calculation.recalculateDay(e.userId(), e.date());
            calculation.recalculateWeek(e.userId(), e.date());
        } catch (Exception ex) {
            log.error("Recalculation failed for user {} on {}", e.userId(), e.date(), ex);
        }
    }
}
```
- `@TransactionalEventListener(AFTER_COMMIT)` — confirmed: only reacts to a change that has actually durably committed, never one that might still roll back. **Requires `CompletionChanged` to always be published from inside an active `@Transactional` method** — with the default `fallbackExecution=false`, if it's ever published outside a transaction, the listener silently never fires. Not yet verified that every publish site (`TaskCompletionService.updateCompletion`, and `HabitCompletionService`'s equivalent) is `@Transactional`; `updateCompletion` is confirmed `@Transactional`, the `HabitCompletion` equivalent not yet checked.
- `@Async` — keeps recalculation off the original request's critical path. Needs `@EnableAsync` configured somewhere (not yet confirmed) and a sized thread pool for the async executor (not yet confirmed — default `SimpleAsyncTaskExecutor` creates an unbounded new thread per call, usually not what you want in production).
- `recalculateWeek` depends on `recalculateDay` having already committed (it reads the just-written `PercentageDay.rate`). This works correctly because `recalculateDay` is `@Transactional(REQUIRES_NEW)` — it commits and returns before `recalculateWeek` starts, so the ordering here is sound, not accidental.
- **Known gap, logged to `REFACTORING_BACKLOG.md` §12:** no retry at all. A failure (or a dropped event entirely, e.g. a crash between commit and the async listener running) just gets logged — unlike the `Job`/`Generator` world, there's no "next cycle" to naturally retry it. A stale `rate` persists until the same user/date happens to produce another `CompletionChanged` later, if ever.

```java
@Service @RequiredArgsConstructor @Slf4j
public class PercentageCalculationService {
    private final HabitCompletionService completionService;
    private final PercentageDayService dayService;
    private final PercentageWeekService weekService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateDay(UUID userId, LocalDate date) {
        BigDecimal rate = dayRate(completionService.findByUserAndDate(userId, date));
        if (!dayService.updateRate(userId, date, rate))
            log.warn("No PercentageDay for user {} on {}: generator will create it", userId, date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateWeek(UUID userId, LocalDate date) {
        List<PercentageDay> days = dayService.findByUserAndWeekOf(userId, date);
        if (days.isEmpty()) { log.warn("No PercentageDay for user {} in week of {}", userId, date); return; }
        if (!weekService.updateRate(userId, date, weekRate(days)))
            log.warn("No PercentageWeek for user {} in week of {}", userId, date);
    }
    // dayRate(): sum of HabitCompletion.getHabit().getPoint() weighted by c.isComplete(), divided by total points available that day.
    // weekRate(): simple arithmetic average of that week's PercentageDay.getRate() values.
}
```
**Confirmed design decisions from this round's review:**
- **Scope: `HabitCompletion` only.** `TaskCompletion` deliberately does not contribute to the percentage — confirmed intentional, not an oversight.
- **Field name correction (this round, against the real DDL):** both `habit_completion` and `task_completion` use the column name `complete` (not `completed`) — a prior round's confirmation that `HabitCompletion` uses `completed` was **wrong** and has been reverted. The getter is `c.isComplete()`, not `c.isCompleted()`; `HabitCompletionGenerator`'s original `.complete(false)` builder call was correct all along.
- **Weekly average method: simple average of the week's daily rates**, not a points-weighted average across the whole week — confirmed intentional. (The two methods give materially different numbers on weeks where different days have different total available points; simple averaging was the confirmed choice.)
- **Graceful handling of a missing skeleton row** (`updateRate` returns `false`, no exception) — accepted as a rare edge case (realistically only possible right after a fresh deploy, before the startup catch-up cycle completes), not treated as an error. Note the `log.warn` comment ("generator will create it") is slightly optimistic: the generator will create the zero-value row, but the specific rate change that triggered this event is permanently lost unless another `CompletionChanged` fires for the same date later — same root cause as the no-retry gap above.
- **New repository/service methods delivered this round:** `HabitCompletionService.findByUserAndDate(userId, date)`, `PercentageDayService.updateRate(userId, date, rate)` (bulk `@Modifying` update, not fetch+save), `PercentageDayService.findByUserAndWeekOf(userId, date)` (Monday–Sunday range, consistent with `WeekGenerator`'s ISO week), `PercentageWeekService.updateRate(userId, date, rate)` (matches the week containing `date` via `BETWEEN pw.week.startWeek AND pw.week.endWeek`, no separate week-boundary lookup needed).

### 🔴 Critical fixes found this round, cross-checking the real DDL against the already-pasted code

1. **`dayRate()` scale bug — fixed.** `percentage_day.rate`/`percentage_week.rate` are both `NUMERIC(5,2)` with `CHECK (rate BETWEEN 0 AND 100)` — a genuine 0–100 percentage, not a 0–1 fraction. The original `dayRate()` computed `completed.divide(total, 2, HALF_UP)`, which produces a 0–1 fraction (e.g. `0.50` for 50% complete). This is **not caught by the CHECK constraint** (a fraction like `0.50` still satisfies `BETWEEN 0 AND 100`), so it would have silently stored wrong percentage values for every day except a perfect 0% or a rate ≥ 1. Fixed:
   ```java
   return BigDecimal.valueOf(completed)
           .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
           .multiply(BigDecimal.valueOf(100))
           .setScale(2, RoundingMode.HALF_UP);
   ```
   `weekRate()` needed no change — it averages already-stored (now correctly 0–100-scaled) `PercentageDay.rate` values.
2. **`habit.point` is `DOUBLE PRECISION` in the real schema, not an integer as previously assumed.** This upgrades the earlier "minor style note" about `dayRate()` accumulating in `double` before converting to `BigDecimal` from a theoretical concern to a real precision risk (classic floating-point summation error, e.g. `0.1 + 0.2 != 0.3` exactly, compounding across many habits). Not yet fixed — `point` would need to be read into `BigDecimal` at the start of the loop instead of `double` for full correctness; left as a judgment call pending how large/fractional real `point` values get in practice.
3. **`users` has no `is_deleted` column at all.** `User` does not support soft-delete. **Corrected:** `UserService.getAllUsers()`/`UserRepository` now filter on `isEmailVerified=true` only (see §5, `PercentageWeekGenerator`'s dependencies, updated above).
4. **`percentage_week.grade VARCHAR(100) NOT NULL`, no default** — exists in the DDL but is never referenced by any pasted Generator/Calculator code so far. **User confirmed a default/computation for it already exists elsewhere in their own code**, not yet shown to the assistant — no action needed here, just noting the column exists and isn't yet part of the reviewed `PercentageWeek`-writing code paths.
5. **No junction table between `habit_task` and `habit_day_week`** in the real DDL (unlike `habit` ↔ `habit_day_week`, via `habit_day_of_week`). This puts the previously-logged "`HabitTask.habitDayWeekIds` exists but `WEEKLY` doesn't consult it" backlog item in doubt — **awaiting confirmation of whether this field/relationship exists on `HabitTask` at all** before treating it as an actionable item (see `REFACTORING_BACKLOG.md` §12 caveat).

**Minor, non-blocking DDL observations also surfaced this round (not yet acted on):**
- `habit.type BOOLEAN NOT NULL DEFAULT TRUE` — unclear semantic meaning from the column name alone; worth a rename or a comment.
- `task_priority_level` has no `updated_at`/`created_at`, unlike the structurally similar `habit_category` which has both — inconsistency, not yet confirmed intentional.
- Schema uses H2's `RANDOM_UUID()` for PK defaults — fine for a test/dev DB, but will need a dialect-appropriate equivalent (e.g. Postgres's `gen_random_uuid()`) if these migrations are meant to run against the production DB directly.

---

## 5b. Same-day single-entity visibility — `CompletionProvisioner` + `ProvisioningTrigger` (design finalized this round)

**The problem these solve:** a `Generator`'s `before()` resumes from a date **cursor** — one cheap query for the frontier, never a "scan for anything missing" check (which would mean comparing the full user×date space on every run — expensive, and would defeat the entire point of the cursor design). This means a `Generator`, by design, can never see "today, right now" for an entity created after its cron already ran today (new `User`, new `Habit`, new `HabitTask`). Not a bug — an intentional, accepted cost trade-off of the cron/cursor model. A genuinely separate, event-driven mechanism is required for same-day visibility, and it must stay **completely outside** `JobRegistry`/`Engine`/`JobScheduler` — an earlier idea to route it through `Engine.run(Job job)` (see the now-struck `JobTrigger` row in §3's Components table) was considered and dropped: it would re-run a whole `Generator`'s batch logic for every user just to fix one user's gap, which is wasteful compared to a provisioner scoped to exactly the one entity that needs it.

**`CompletionProvisioner` (`@Service`)** — single source of truth for "make sure this one entity's row(s) exist and are correct, right now":
```java
public void ensureForHabit(Habit habit, LocalDate date) {
    if (matchesToday(habit, date) && !habitCompletionService.exists(habit.getHabitId(), date))
        habitCompletionService.create(habit, date); // complete=false

    // Order matters: a brand-new incomplete habit changes today's rate
    // denominator, so recalculation must happen AFTER creation, inside this
    // same method — not left to the caller to remember as a second step.
    calculation.recalculateDay(habit.getUser().getUserId(), date);
    calculation.recalculateWeek(habit.getUser().getUserId(), date);
}

public void ensureForTask(HabitTask task, LocalDate date) {
    if (shouldCreate(task, date) && !taskCompletionService.exists(task.getHabitTaskId(), date))
        taskCompletionService.create(task, date); // complete=false
    // No recalculate call — TaskCompletion is confirmed excluded from the percentage (§5a).
}
```

**`ProvisioningTrigger` (`@Component`)** — pure dispatch, one class, one `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` method per domain event (`HabitCreated`, `HabitTaskCreated`), each a one-line delegation to `CompletionProvisioner`. All sequencing/ordering logic lives in the Provisioner's methods, never in the listener — mirrors the already-proven `CompletionListener`/`CompletionChanged` pattern in §5a.

**`UserRegistered`/`ensureForUser` dropped entirely — decided this round, user-caught.** A brand-new user has no habits, no completions, nothing to recalculate; `ensureForUser` would only ever produce a meaningless `rate = 0` row through the normal zero-total branch of `dayRate()`. There is no same-day visibility gap to close here at all — unlike `HabitCreated`/`HabitTaskCreated`, where the user can act on the new entity *today* and genuinely needs to see that reflected before tonight's cron, a fresh registration has no "today's activity" that the nightly `Generator`s would otherwise miss. Removed: the `UserRegistered` record, `CompletionProvisioner.ensureForUser(...)`, `ProvisioningTrigger.on(UserRegistered e)`, and the planned `eventPublisher.publishEvent(new UserRegistered(...))` call in `UserService.createUser()` — never implemented, now never needed. `ProvisioningTrigger` ends up with exactly 3 methods: `HabitCreated`, `HabitTaskCreated`, `CompletionChanged`.

**Shared-logic rule (avoids duplicating recurrence-matching in two places):** the batch `Generator`s (`HabitCompletionGenerator`, `TaskCompletionGenerator`) and `CompletionProvisioner` must share the same **matching predicate** (`matchesToday(habit, date)` / `shouldCreate(task, date)`) extracted into one method each calls — but the Generators keep their own bulk `saveAll` path for performance. They must **not** call `CompletionProvisioner` per-row in a loop, which would turn an O(1) batch insert into N individual queries and defeat the reason the Generators batch in the first place.

**Resulting architecture boundary (now final):** the Engine (`Job`/`Generator`/`JobRegistry`/`Engine`/`JobScheduler`) is exclusively cron/periodic — nothing event-driven lives inside it, by design, not by omission. `CompletionProvisioner`/`ProvisioningTrigger` is a fully independent, event-driven path for single-entity same-day visibility, parallel to (but architecturally separate from) `engine.app.calculate`'s `CompletionListener`. Not yet implemented/compiled — design is settled.

**Code written this round (not yet compiled/confirmed), based on the real entities (`HabitCompletion`, `TaskCompletion`, `PercentageDay`, `PercentageWeek`, `HabitCalendar`):** `HabitCreated`/`HabitTaskCreated` (ID-only payload, same minimal-payload principle as `CompletionChanged`; `UserRegistered` dropped, see below), the upsert-capable `recalculateDay`/`recalculateWeek` (using `PercentageDay`/`PercentageWeek`'s real constructors + `updateRate()`, no separate zero-value creation path), `CompletionProvisioner`, and `ProvisioningTrigger`. **Two explicit gaps left for the user to close, not guessed at:** `ensureForHabit`'s day-matching check and `ensureForTask`'s recurrence check must be the *exact same* predicate `HabitCompletionGenerator`/`TaskCompletionGenerator` already use internally (extracted so both call one method), not re-derived copies — risk of drift otherwise. New service methods needed: `PercentageDayService.findByUserAndHabitCalendar`, `PercentageWeekService.findByUserAndHabitWeek`, `HabitCompletionService.existsByHabitAndCalendar`, `TaskCompletionService.existsByHabitTaskAndCalendar`. Also needs `eventPublisher.publishEvent(...)` added to `HabitService.createHabit()`/`HabitTaskService.createHabitTask()`.

**Task side — resolved and finalized this round, against the real `TaskCompletionGenerator` source (user-pasted):** the shared predicate gap above is closed for Task. `TaskCompletionGenerator`'s private `shouldCreate(HabitTask, LocalDate)`/`isYearlyMatch(...)` were extracted verbatim into a new `@Component`, `TaskRecurrenceMatcher` (injected with `TaskCompletionService` + `Clock`), which both `TaskCompletionGenerator.generate()` and `CompletionProvisioner.ensureForTask()` now call — no re-derived copy anywhere. `TaskCompletionGenerator` keeps its own `Set<String>`-keyed bulk-dedupe check for batch performance; `ensureForTask` uses the equivalent single-row `TaskCompletionService.existsByHabitTaskAndCalendar(habitTaskId, habitCalendarId)` — same existence question, different mechanism sized to a single row vs. a batch, not a logic divergence.

**Caught and reverted this round: `ensureForTask` must not read `SecurityContextHolder`.** A user-drafted version resolved `CustomUserDetails` via `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` and passed `userDetails.id()` into an ownership-checked `getHabitTaskOrThrow(habitTaskId, userId)`. Rejected for two reasons (now codified in `ENGINEERING_RULES.md` §13): (1) `ProvisioningTrigger` invokes this from an `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)` method, running on a pooled executor thread after the originating transaction already committed — `SecurityContextHolder`'s default `MODE_THREADLOCAL` does not carry the web request's context onto that thread, so `getAuthentication()` is `null`/anonymous far more often than not, an intermittent NPE risk; (2) even if it worked, "whoever is logged in right now" isn't the right question for an async background path — the task's owner is already authoritative via `HabitTask`→`Habit`→`User`, and the event existing at all already proves `HabitTaskService.createHabitTask()` authorized the real caller at the real request. Final shape uses an unscoped `taskService.getHabitTaskOrThrow(habitTaskId)` — no user param, no Security Context, in `ensureForTask`:
```java
public void ensureForTask(UUID habitTaskId, LocalDate date) {
    HabitTask task = taskService.getHabitTaskOrThrow(habitTaskId);
    HabitCalendar calendar = calendarService.findFirstByDateOrThrow(date);

    if (taskRecurrenceMatcher.shouldCreate(task, date)
            && !completionService.existsByHabitTaskAndCalendar(habitTaskId, calendar.getHabitCalendarId()))
        completionService.create(new TaskCompletion(false, calendar, task));
}
```
New methods added to close this: `HabitTaskService.getHabitTaskOrThrow(UUID habitTaskId)` (unscoped; a separate `(id, callerUserId)` overload stays available for actual Controller-driven, caller-scoped lookups elsewhere), `TaskCompletionService.existsByHabitTaskAndCalendar(UUID, UUID)` + `TaskCompletionService.create(TaskCompletion)`.

**Task phase (of the two-phase Habit/Task split) is now fully closed at the design level** — `TaskCompletionGenerator`, `TaskRecurrenceMatcher`, and `CompletionProvisioner.ensureForTask()` all agree on one predicate, with no Security Context dependency. Only `HabitTaskCreated` triggers provisioning (create-only); Task update/delete deliberately do **not** touch `TaskCompletion` once created, since it carries its own `complete` state that must survive the parent `HabitTask` changing. Habit phase (`HabitCompletion`/`PercentageDay`/`PercentageWeek`, plus its update/delete behavior) remains open, picked up next per the user's explicit "Task first" sequencing.

**Habit side — matching check resolved this round, against the real `HabitCompletionGenerator`/`HabitDayWeekService` source (user-pasted).** Unlike Task, `HabitCompletionGenerator` has no standalone `shouldCreate(habit, date)` predicate to extract — it queries the *inverse* direction for batch efficiency: `HabitDayWeekService.findHabitsByDayOfWeek(day)` returns every `Habit` scheduled for a given day-of-week in one query (backed by `HabitDayWeekRepository.findActiveHabitsByDayOfWeek(day)`, which already filters `isActive` — the Generator's extra in-memory `.filter(Habit::isActive)` is therefore redundant, a harmless minor cleanup noted but not required). For the single-entity Provisioner case, a mirrored single-row query was added instead of forcing the Generator's batch shape into a loop (same accepted pattern as Task's Set-dedupe vs. single-exists):
```java
// HabitDayWeekRepository — new, same join `findActiveHabitsByDayOfWeek` uses, scoped to one habit
@Query("""
    SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
    FROM Habit h JOIN h.habitDayWeeks hdw
    WHERE h.habitId = :habitId AND hdw.dayOfWeek = :day AND h.isActive = true
""")
boolean existsActiveHabitByHabitIdAndDayOfWeek(@Param("habitId") UUID habitId, @Param("day") DayOfWeek day);
```
`h.habitDayWeeks` is an **assumed** collection-field name on `Habit`, not yet confirmed against the real entity — flagged to the user, needs correcting if wrong. The new `HabitDayWeekService.existsActiveHabitByHabitIdAndDayOfWeek(...)` wrapper is **deliberately left uncached**, unlike every other read method in that service — see the cache-region finding below.

**Cache-region mismatch spotted while reading `HabitDayWeekService` (flagged, not fixed — deferred to backlog):** `addDay()`'s `@CacheEvict(allEntries = true)` has no explicit `value`, so it only clears the class-level default region (`CacheNames.HABIT_DAY_WEEK + LIST_SUFFIX`, the same region `findByDayAsc()` uses) — but `findHabitsByDayOfWeek()`/`findHabitsByDayAndUser()` are cached under a *different*, explicitly-specified region (`CacheNames.HABIT + LIST_SUFFIX`/`PAGE_SUFFIX`). `addDay()` can never evict those, which is a pre-existing possible staleness gap independent of this round's work. Not investigated further or fixed — out of scope for the current Task/Habit provisioning topic; tracked in `REFACTORING_BACKLOG.md`.

**`ensureForHabit` — finalized:**
```java
public void ensureForHabit(UUID habitId, LocalDate date) {
    Habit habit = habitService.getHabitOrThrow(habitId);
    HabitCalendar calendar = habitCalendarService.findFirstByDateOrThrow(date);

    boolean appliesToday = habitDayWeekService.existsActiveHabitByHabitIdAndDayOfWeek(habitId, date.getDayOfWeek());

    if (appliesToday && !habitCompletionService.existsByHabitAndCalendar(habitId, calendar.getHabitCalendarId()))
        habitCompletionService.create(new HabitCompletion(false, calendar, habit));

    calculation.recalculateDay(habit.getUser().getUserId(), date);
    calculation.recalculateWeek(habit.getUser().getUserId(), date);
}
```
`appliesToday` already folds in the `isActive` check (via the query's own `isActive = true` clause), so no separate `habit.isActive()` re-check is needed in the Provisioner. New methods added to close this: `HabitCompletionService.existsByHabitAndCalendar(UUID, UUID)` + `HabitCompletionService.create(HabitCompletion)`, mirroring the Task-side equivalents exactly.

**`PercentageCalculationService` made upsert-capable this round, against the real class source (user-pasted).** The real implementation confirmed `dayService.updateRate(userId, date, rate)` / `weekService.updateRate(userId, date, rate)` already do their own find-by-user-and-date/week internally and return `boolean` (found-and-updated vs. not found) — this means the two service methods flagged earlier as needed, `PercentageDayService.findByUserAndHabitCalendar`/`PercentageWeekService.findByUserAndHabitWeek`, are **no longer needed**, superseded by this discovery. What's actually needed is just the create-on-miss branch:
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recalculateDay(UUID userId, LocalDate date) {
    BigDecimal rate = dayRate(completionService.findByUserAndDate(userId, date));
    if (!dayService.updateRate(userId, date, rate)) {
        HabitCalendar calendar = calendarService.findFirstByDateOrThrow(date);
        dayService.save(new PercentageDay(rate, calendar, userService.getUserOrThrow(userId)));
        log.info("Created missing PercentageDay for user {} on {}", userId, date);
    }
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recalculateWeek(UUID userId, LocalDate date) {
    List<PercentageDay> days = dayService.findByUserAndWeekOf(userId, date);
    if (days.isEmpty()) { log.warn("No PercentageDay for user {} in week of {}", userId, date); return; }

    BigDecimal rate = weekRate(days);
    if (!weekService.updateRate(userId, date, rate)) {
        HabitWeek week = calendarService.findFirstByDateOrThrow(date).getHabitWeek();
        weekService.save(new PercentageWeek(rate, week, userService.getUserOrThrow(userId)));
        log.info("Created missing PercentageWeek for user {} in week of {}", userId, date);
    }
}
```
Two new dependencies added to the service (`HabitCalendarService`, `UserService`), used only on the miss branch — the normal hot-path (row already exists) is untouched. New methods needed: `PercentageDayService.save(PercentageDay)`, `PercentageWeekService.save(PercentageWeek)`, `UserService.getUserOrThrow(UUID)` (assumed to already exist, same naming convention as `getHabitOrThrow`/`getHabitTaskOrThrow` — not yet confirmed). **Side note, not acted on:** the real `dayRate()` calls `c.isCompleted()`, which conflicts with this document's own earlier §5a note claiming the confirmed getter is `c.isComplete()` (field `complete`, not `completed`) — one of the two is wrong; flagged for the user to clarify whenever convenient, not blocking this work.

**Final package structure and the `CompletionListener`→`ProvisioningTrigger` merge — confirmed by the user this round, kept deliberately simple, no restructuring.** The user consolidated everything into one tree (screenshot-verified):
```
engine.app.generate/
  common/
    TaskRecurrenceMatcher
  triggers/
    bridges/
      CompletionChanged
      HabitCreated
      HabitTaskCreated
    CompletionProvisioner
    PercentageCalculationService
    ProvisioningTrigger
  (TaskCompletionGenerator, HabitCompletionGenerator, WeekGenerator, DayGenerator — existing siblings)
```
(`UserRegistered` removed from `bridges` — see the drop decision above.) The standalone `engine.app.calculate.CompletionListener` is **dissolved** — its one method (`on(CompletionChanged e)`) is now a third method on `ProvisioningTrigger`, alongside `HabitCreated`/`HabitTaskCreated`, same `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + `safely()` shape. `PercentageCalculationService` moved into the same `triggers` package as its primary caller. Considered and explicitly **not pursued**: promoting `triggers` to a sibling of `generate` (e.g. `engine.app.triggers`) to keep the "architecturally separate from the periodic Engine" distinction visible at the package level, not just in this document's prose — the user has no alternative structure in mind and prefers to keep the current simple layout; not worth the churn without a concrete problem it solves. Minor, non-blocking style note spotted in the merged class: the `CompletionChanged` listener method still spells out `phase = TransactionPhase.AFTER_COMMIT` instead of the `AFTER_COMMIT` static import the other three methods use — cosmetic only, worth a quick normalize pass whenever the user is in that file next.

---

## 6. Open / Not Yet Decided

- **`Engine` implementation fix** — `JobScheduler`'s per-job cron callback was found calling a single-job `run(job)` instead of the full `runCycle()`; a fix was given — **not yet confirmed applied.**
- **Two log-clarification questions**, still unanswered: (1) whether the runtime log reviewed for the fix above was captured before or after it; (2) whether `WeekGenerator`'s own `job_run` activity appears earlier in the fuller log, confirming topological order held end-to-end.
- ✅ ~~`JobTrigger` (event-driven single-job path)~~ — **design finalized, see §5b.** Superseded the `Engine.run(Job job)`-routed idea entirely — the final mechanism (`CompletionProvisioner` + `ProvisioningTrigger`) lives completely outside `JobRegistry`/`Engine`/`JobScheduler`, scoped to exactly one entity per event instead of re-running a whole `Generator`'s batch. Not yet implemented/compiled — design settled.
- **`JobRunStore`'s concrete implementation** (`JobRunEntity`, `JobRunRepository`, `JpaJobRunStore`, `RunStatus`) — confirmed to exist in the real project tree, but internals not yet reviewed in detail.
- ✅ **`EngineDates`/`Engine` package placement — confirmed already correct.** User's real package tree (verified via screenshot) shows `Engine` at `engine.core.Engine` and `EngineDates` at `engine.core.generate.EngineDates`, both already nested under `core`. The earlier note that they sat "bare at the `engine` package root" was wrong (based on an incomplete picture); no move needed.
- **`markStaleRunsAsFailed()`** — caller and staleness threshold explicitly deferred by the user (Agile-style).
- **Transactional boundary** between `generate()`/`process()` and `after()` — still undecided (single `@Transactional` wrapping both, or incremental commits).
- **Compile confirmation still pending** for all four pasted-by-user generators: `PercentageWeekGenerator`, `PercentageDayGenerator`, `TaskCompletionGenerator` (with the restored `ONCE` semantics and corrected dependency methods), and `HabitCompletionGenerator` (drafted by the assistant, not yet reviewed/compiled by the user at all).
- **`HabitCompletionGenerator` open questions:** whether `HabitCompletionService`/`HabitCompletionRepository` already exist (just need the 3 new methods) or need scaffolding from scratch; same-day completion visibility for a newly-created `Habit`/`HabitTask` (sync at creation time vs. wait for next cron cycle) — not yet decided, applies to `TaskCompletionGenerator` too.
- **`HabitTask.habitDayWeekIds` existence unconfirmed** — the real DDL has no junction table between `habit_task` and `habit_day_week`, putting the "`WEEKLY` should support multiple days" backlog item in doubt. Needs confirmation before being actionable.
- **`habit.point` as `DOUBLE PRECISION`** — `PercentageCalculationService.dayRate()` still accumulates it in `double` before converting to `BigDecimal`; confirmed a real (not just theoretical) precision risk now that the DDL shows `point` is a genuine float column. Not yet fixed — a judgment call pending real-world `point` value ranges.
- **Minor DDL style notes, not yet acted on:** `habit.type BOOLEAN` has an unclear name; `task_priority_level` is missing `updated_at`/`created_at` unlike the similar `habit_category`; schema uses H2's `RANDOM_UUID()`, which will need a dialect-appropriate equivalent if these migrations run directly against a production DB other than H2.
- **No self-healing/retry for `CompletionListener`** — logged to `REFACTORING_BACKLOG.md` §12 as a future reconciliation-job idea; not urgent, not designed yet. One concrete cause of staleness under this umbrella — the missing-skeleton-row case — is now addressed directly via `CompletionProvisioner`/`ProvisioningTrigger` (see §5b), not the general reconciliation job; the reconciliation idea remains open for the other cause (listener failure/dropped event after the row already exists).
- ✅ **`job_run` retention/cleanup — DECIDED: plain `@Scheduled`, not an Engine `Job`. Reconsidered twice and reaffirmed both times.** Kept deliberately outside `JobRegistry`/`Engine` for two reasons: cleaning up the Engine's own execution-history table is infrastructure maintenance, not domain generation, so it sits a layer below the Engine conceptually; and more importantly, if `JobRegistry` itself ever fails at startup (a cycle, a bug in the topological sort), every registered `Job` — including a cleanup job, if it were one — would fail to run right when `job_run` would most need pruning (a uniquely self-referential risk: `job_run` is the Engine's *own* table). An independent `@Scheduled` method has no dependency on the Engine's health. **`job_run` cleanup itself is explicitly deferred — a separate, later effort**, unaffected by the six domain cleanups below becoming actual `Job`s (that pivot doesn't reopen this one, since the self-referential risk is specific to `job_run` and doesn't apply to ordinary domain tables).
- ✅ **Six pre-existing domain `cleanupOldXxx()` methods — DECIDED this round: each becomes a real Engine `Job`, not a standalone `Cleaner`/`MaintenanceScheduler`.** `HabitCalendarService.cleanupOldCalendars()`, `HabitWeekService.cleanupOldWeeks()`, `TaskCompletionService.cleanupOldCompletions()`, `HabitCompletionService.cleanupOldHabitCompletions()`, `PercentageDayService.cleanupOldPercentageDays()`, `PercentageWeekService.cleanupOldPercentageWeeks()` already existed, unwired to anything. An earlier `Cleaner` interface + `order()` int + standalone `MaintenanceScheduler` was proposed and dropped — unlike `job_run`, these clean ordinary domain tables, carrying the same Engine-outage risk any existing `Generator` already accepts, so there's no reason not to reuse the Engine's own machinery directly: `dependsOn()` instead of a hand-rolled `order()` (gets `JobRegistry`'s proven topological sort, which fails loudly at startup on a cycle/bad id instead of silently misordering), `JobScheduler` instead of a new scheduler class, `Engine`'s retry + `JobRunStore` tracking instead of a bare try/catch. A separate `CleanRunStore` was also considered and rejected — `job_run`/`JobRunStore` already covers pass/fail/attempts, and each method's own `log.info("Cleaned up {} rows...")` already covers the deleted-count detail.
  - **FK ordering is confirmed mandatory, not just best-practice**, after reviewing the real entity code: `PercentageDay.habitCalendar`/`PercentageWeek.habitWeek` are real `@ManyToOne(optional = false)`/`@JoinColumn(nullable = false)` foreign keys with no cascade on the owning side, and `HabitCalendar`'s `cascade = CascadeType.ALL, orphanRemoval = true` toward `HabitCompletion`/`TaskCompletion` is irrelevant here because **all six methods use bulk `@Modifying @Query("DELETE FROM X WHERE x.createdAt < :cutoff")` deletes**, which bypass the persistence context (and therefore any JPA cascade) entirely.
  - **Final `dependsOn()` graph**, built directly from the confirmed `NOT NULL` FK columns (`habit_calendar_id`, `habit_week_id`):
    ```
    TaskCompletionCleanupJob/HabitCompletionCleanupJob/PercentageDayCleanupJob/PercentageWeekCleanupJob → dependsOn: []
    HabitCalendarCleanupJob → dependsOn: [TASK_COMPLETION_CLEANUP, HABIT_COMPLETION_CLEANUP, PERCENTAGE_DAY_CLEANUP]
    HabitWeekCleanupJob     → dependsOn: [HABIT_CALENDAR_CLEANUP, PERCENTAGE_WEEK_CLEANUP]
    ```
  - Each is `implements Job` directly (not `Generator<T>` — no generation/resume-cursor shape, just "delete older than cutoff"), `cron() = List.of(Cron.MONTHLY)`, delegating straight to its service's existing method. Retention cutoffs remain each service's own internal decision. Six new `JobIds` constants added. Full detail and code in `REFACTORING_BACKLOG.md` §12.
  - **Minor, non-blocking note:** at least `PercentageWeek`'s cleanup query filters on `createdAt`, the row's creation timestamp, not the business date it represents — harmless at current retention scales, worth revisiting only if a `Generator`'s horizon is ever extended much further.
- ✅ **`ReferenceDataIntegrityJob` — two real bugs found and fixed, confirmed.** (1) The `private static final Set<HabitDayWeek>` field on this `@Component` singleton was removed in favor of a local variable inside `execute()` — it was never cleared between daily runs and violated the standing rule against mutable fields on Spring singletons. (2) The missing-row check now compares the plain `DayOfWeek` enum values (correct built-in equality) instead of full `HabitDayWeek` entity objects — the original `existingSet.contains(new HabitDayWeek(day))` silently never matched anything, since `HabitDayWeek` has no `equals()`/`hashCode()` override and fell back to identity equality, meaning every run would have concluded all 7 rows were "missing" and tried to re-insert them daily.
- ✅ **Startup reference-data seeding — FULLY DECIDED (expanded this round).** The old pre-Engine `initialization()` method (now commented out) revealed the real scope is five things, not just `HabitDayWeek`: `Role` (3 rows), `TaskPriorityLevel` (5 rows), `HabitCategory` (now 40 rows, expanded from the original 20), `HabitDayWeek` (7 rows), and a default admin `User`. They do **not** all get the same treatment — confirmed `Role` and `TaskPriorityLevel` both expose `...IsDeletedFalse` repository methods (soft-deletable, admin-manageable), same as the already-confirmed `HabitCategory`. Only `HabitDayWeek` has no soft-delete at all and is genuinely immutable.
  - **`HabitDayWeek` only** — two-layer approach:
    1. **Flyway migration** does the initial `INSERT` of the 7 fixed rows at migration time — guarantees the data exists from the first moment the schema is applied, before the Spring context even starts. Confirmed done (migrations finished in full this round).
    2. **A daily `Job`**, `ReferenceDataIntegrityJob implements Job` (not a `Generator<T>` — no horizon to generate, just a fixed set to verify) — `dependsOn()` empty, `cron()` daily. No separate "fixed ids" bookkeeping needed at all: `HabitDayWeek.dayWeekId` is simply `DayOfWeek.getValue()` (1=MONDAY..7=SUNDAY), a plain `long`, not a generated/UUID value — so re-constructing `new HabitDayWeek(day)` naturally reproduces the exact original id every time. This is a **self-healing reconciliation job**, not a seeder — its purpose is recovering from accidental deletion/corruption after the fact, since losing a `HabitDayWeek` row is always a bug, never a legitimate admin action. **Written, bugs fixed, confirmed working.**
  - **`Role`, `TaskPriorityLevel`, `HabitCategory`, and the admin `User` — all four handled by a single revived `ApplicationRunner`** (the cleaned-up, uncommented version of the old `initialization()` logic), checking existence before inserting each (idempotent, safe to run every startup), entirely **outside** the Engine/`JobRegistry`. Deliberately **not** given a daily reconciliation job like `HabitDayWeek`: all four are soft-deletable/admin-editable, so forcibly re-inserting one an admin intentionally removed (e.g. soft-deleting a `HabitCategory`, or removing a `TaskPriorityLevel`) would silently override a legitimate admin decision. A one-time "exists? if not, create the defaults" check at every startup is the correct and sufficient level of protection here — not full periodic resurrection.
  - The admin `User` specifically could never have been a Flyway row anyway — it needs `${APP_ADMIN_USERNAME}`/`${APP_ADMIN_PASSWORD}` resolved from the Spring environment and run through the `PasswordEncoder` bean, neither of which a static migration file can do.



