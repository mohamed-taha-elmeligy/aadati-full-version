# Graph Report - Aadati  (2026-11-01)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1682 nodes · 5851 edges · 74 communities (26 shown, 48 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 553 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `321eb54b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- TaskPriorityLevel
- Role
- HabitCategory
- HabitTask
- HabitWeek
- InvalidRequestException
- PercentageDay
- JobRunEntity
- uuid
- PageModel
- org.springframework.transaction.annotation.Transactional
- Habit
- lombok.RequiredArgsConstructor
- Job
- CustomUserDetails
- HabitCompletionService
- User
- org.springframework.stereotype.Component
- Auditing
- HabitCompletion
- org.springframework.http.ResponseEntity
- Cron
- JwtUtils
- org.springframework.data.domain.Page
- TaskCompletion
- PercentageWeek
- org.mapstruct.Mapping
- org.springframework.data.jpa.repository.JpaRepository
- RateLimitService
- org.springframework.data.jpa.repository.Query
- .map
- SecurityConfig.java
- jakarta.servlet.http.HttpServletRequest
- HabitCompletionController
- PercentageWeekController
- RateLimitFilter.java
- UserResponse
- CachingConfig.java
- AuthService.java
- JobRun
- HabitCalendar
- JwtAccessDeniedHandler.java
- HabitCalendarController
- RateLimitConfig.java
- AdminUserController
- HabitWeekController
- AopLogging.java
- InvalidCredentialsException
- org.springframework.context.annotation.Configuration
- HabitRepository
- SwaggerConfig.java
- UserController
- AadatiApplication.java
- org.springframework.context.annotation.Bean
- JobScheduler.java
- org.springframework.cache.Cache
- DayGenerator
- MailService
- RateLimitFilter
- Generator
- PasswordEncoderConfig.java
- qodana.yaml
- ReferenceDataIntegrityJob
- AadatiApplicationTests.java
- JobRunCleanupTask.java
- PercentageDayCleanupJob
- PercentageWeekCleanupJob
- HabitCalendarCleanupJob
- BusinessRuleViolationException
- OperationFailedException
- PasswordResetService.java
- com.mts:aadati

## God Nodes (most connected - your core abstractions)
1. `InvalidRequestException` - 121 edges
2. `PageModel` - 114 edges
3. `CustomUserDetails` - 103 edges
4. `HabitCalendar` - 54 edges
5. `HabitCompletion` - 52 edges
6. `HabitCategory` - 52 edges
7. `Habit` - 49 edges
8. `PercentageWeek` - 48 edges
9. `ResourceNotFoundException` - 48 edges
10. `PercentageDay` - 47 edges

## Surprising Connections (you probably didn't know these)
- `HabitTask` --references--> `TaskPriorityLevel`  [EXTRACTED]
  src/main/java/com/mts/aadati/entities/HabitTask.java → src/main/java/com/mts/aadati/entities/TaskPriorityLevel.java
- `HabitTaskService` --references--> `TaskPriorityLevelRepository`  [EXTRACTED]
  src/main/java/com/mts/aadati/services/HabitTaskService.java → src/main/java/com/mts/aadati/repository/TaskPriorityLevelRepository.java
- `TaskPriorityLevelSeeder` --implements--> `Seeder`  [EXTRACTED]
  src/main/java/com/mts/aadati/seed/seeders/TaskPriorityLevelSeeder.java → src/main/java/com/mts/aadati/seed/Seeder.java
- `UserController` --references--> `RoleMapper`  [EXTRACTED]
  src/main/java/com/mts/aadati/controllers/users/UserController.java → src/main/java/com/mts/aadati/dto/mapper/RoleMapper.java
- `Role` --inherits--> `Auditing`  [EXTRACTED]
  src/main/java/com/mts/aadati/entities/Role.java → src/main/java/com/mts/aadati/configs/auditing/Auditing.java

## Import Cycles
- None detected.

## Communities (74 total, 48 thin omitted)

### Community 0 - "TaskPriorityLevel"
Cohesion: 0.06
Nodes (9): AdminTaskPriorityLevelController, TaskPriorityLevelController, TaskPriorityLevelMapper, TaskPriorityLevelRequest, TaskPriorityLevelResponse, TaskPriorityLevel, TaskPriorityLevelRepository, TaskPriorityLevelSeeder (+1 more)

### Community 1 - "Role"
Cohesion: 0.06
Nodes (11): RoleController, RoleMapper, RoleRequest, RoleResponse, Role, RoleRepository, ReferenceDataInitializer, Seeder (+3 more)

### Community 2 - "HabitCategory"
Cohesion: 0.07
Nodes (7): AdminHabitCategoryController, HabitCategoryMapper, HabitCategoryRequest, HabitCategoryResponse, HabitCategory, HabitCategoryRepository, HabitCategorySeeder

### Community 3 - "HabitTask"
Cohesion: 0.07
Nodes (12): HabitTaskMapper, HabitTaskRequest, HabitTask, RecurrenceType, DAILY, MONTHLY, NONE, ONCE (+4 more)

### Community 4 - "HabitWeek"
Cohesion: 0.07
Nodes (5): PercentageWeekGenerator, WeekGenerator, HabitWeek, HabitWeekRepository, HabitWeekService

### Community 5 - "InvalidRequestException"
Cohesion: 0.06
Nodes (8): CustomUserDetailsService, HabitRequest, InvalidRequestException, ResourceNotFoundException, HabitCalendarService, HabitCategoryService, HabitService, PercentageWeekService

### Community 6 - "PercentageDay"
Cohesion: 0.08
Nodes (4): PercentageDayGenerator, PercentageDay, PercentageDayRepository, PercentageDayService

### Community 7 - "JobRunEntity"
Cohesion: 0.06
Nodes (20): CacheTtl, CALENDAR, CATEGORY, DAY, HABIT, HABIT_TASK, PERCENTAGE_DAY, PERCENTAGE_WEEK (+12 more)

### Community 8 - "uuid"
Cohesion: 0.08
Nodes (6): HabitCompletionRequest, TaskCompletionRequest, CompletionChanged, HabitCreated, HabitPointChanged, HabitTaskCreated

### Community 9 - "PageModel"
Cohesion: 0.11
Nodes (3): TaskCompletionController, PageModel, TaskCompletionResponse

### Community 10 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.15
Nodes (3): CacheNames, DuplicateResourceException, PageableUtils

### Community 11 - "Habit"
Cohesion: 0.07
Nodes (5): HabitDayWeekController, HabitDayWeekMapper, HabitDayWeekResponse, Habit, HabitDayWeek

### Community 13 - "Job"
Cohesion: 0.13
Nodes (4): JobRunStore, Engine, Job, JobRegistry

### Community 14 - "CustomUserDetails"
Cohesion: 0.13
Nodes (4): CustomUserDetails, HabitController, HabitMapper, HabitResponse

### Community 15 - "HabitCompletionService"
Cohesion: 0.09
Nodes (6): CompletionProvisioner, PercentageCalculationService, ProvisioningTrigger, HabitCompletionService, HabitDayWeekService, TaskCompletionService

### Community 16 - "User"
Cohesion: 0.10
Nodes (3): User, UserRepository, UserService

### Community 17 - "org.springframework.stereotype.Component"
Cohesion: 0.18
Nodes (3): TaskRecurrenceMatcher, EngineDates, JobIds

### Community 21 - "Cron"
Cohesion: 0.08
Nodes (10): HabitCompletionCleanupJob, HabitWeekCleanupJob, TaskCompletionCleanupJob, Cron, DAILY, EVERY_MINUTE, HOURLY, MONTHLY (+2 more)

### Community 26 - "org.mapstruct.Mapping"
Cohesion: 0.15
Nodes (7): HabitCalendarMapper, HabitCompletionMapper, HabitWeekMapper, PercentageWeekMapper, TaskCompletionMapper, HabitCalendarRequest, HabitWeekRequest

### Community 28 - "RateLimitService"
Cohesion: 0.15
Nodes (7): PolicyType, ANONYMOUS, API, AUTH, RateLimitProperties, RatePolicy, RateLimitService

### Community 30 - ".map"
Cohesion: 0.23
Nodes (3): PercentageDayController, PercentageDayMapper, PercentageDayResponse

### Community 36 - "UserResponse"
Cohesion: 0.22
Nodes (3): AdminCreateUserRequest, UserRequest, UserResponse

### Community 38 - "AuthService.java"
Cohesion: 0.21
Nodes (5): AuthController, UserMapper, LoginRequest, AuthResponse, AuthService

### Community 48 - "InvalidCredentialsException"
Cohesion: 0.23
Nodes (3): JwtBlocklistService, InvalidCredentialsException, VerificationCodeService

### Community 49 - "org.springframework.context.annotation.Configuration"
Cohesion: 0.29
Nodes (3): AopConfig, JpaAuditingConfig, SchedulerConfig

### Community 63 - "qodana.yaml"
Cohesion: 0.33
Nodes (5): linter, profile, name, projectJDK, version

## Knowledge Gaps
- **31 isolated node(s):** `DAILY`, `EVERY_MINUTE`, `HOURLY`, `MONTHLY`, `WEEKLY` (+26 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 313 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `InvalidRequestException` connect `InvalidRequestException` to `jakarta.servlet.http.HttpServletRequest`, `Role`, `HabitCategory`, `HabitTask`, `HabitWeek`, `TaskPriorityLevel`, `PercentageDay`, `HabitCalendar`, `PageModel`, `org.springframework.transaction.annotation.Transactional`, `Habit`, `AdminUserController`, `HabitCompletionService`, `User`, `HabitCompletion`, `UserController`, `TaskCompletion`, `MailService`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **What connects `DAILY`, `EVERY_MINUTE`, `HOURLY` to the rest of the system?**
  _31 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `TaskPriorityLevel` be split into smaller, more focused modules?**
  _Cohesion score 0.059720869847452125 - nodes in this community are weakly interconnected._
- **Why does `PageModel` connect `PageModel` to `HabitCompletionController`, `PercentageWeekController`, `HabitTask`, `HabitWeek`, `PercentageDay`, `JobRun`, `JobRunEntity`, `org.springframework.transaction.annotation.Transactional`, `HabitCalendarController`, `lombok.RequiredArgsConstructor`, `CustomUserDetails`, `HabitWeekController`, `org.springframework.http.ResponseEntity`, `.map`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **Should `Role` be split into smaller, more focused modules?**
  _Cohesion score 0.0593607305936073 - nodes in this community are weakly interconnected._
- **Why does `CustomUserDetails` connect `CustomUserDetails` to `HabitCompletionController`, `PercentageWeekController`, `RateLimitFilter.java`, `Role`, `InvalidRequestException`, `UserResponse`, `PageModel`, `HabitCalendarController`, `lombok.RequiredArgsConstructor`, `Habit`, `AopLogging.java`, `org.springframework.http.ResponseEntity`, `UserController`, `JwtUtils`, `.map`?**
  _High betweenness centrality (0.031) - this node is a cross-community bridge._
- **Should `HabitCategory` be split into smaller, more focused modules?**
  _Cohesion score 0.06606990622335891 - nodes in this community are weakly interconnected._