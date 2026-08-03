# RupeeFlow Implementation Plan - Stages 1 & 2

This plan covers the initial architecture setup and the comprehensive database layer for RupeeFlow, following the provided engineering roadmap.

## User Review Required

> [!IMPORTANT]
> I will be restructuring the current package `sayan.apps.rupeeflow` into a modular layout: `core`, `feature`, `domain`, and `ui`.
> I will also add **Hilt** for Dependency Injection, which is a significant change to the project's backbone.

## Open Questions

- Should I keep the existing `sayan.apps.rupeeflow` package name, or do you want to switch to `com.rupeeflow.app` as mentioned in the plan? Switching package names involves updating many files and AndroidManifest.
- For the `Category` entity, the plan mentions `IconRes` and `ColorHex`. The current `Category` model uses `androidx.compose.ui.graphics.Color`. I will move to `ColorHex` (String) for database persistence.

## Proposed Changes

### [Foundation & DI]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/gradle/libs.versions.toml)
- Add Hilt versions, libraries, and plugins.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/build.gradle.kts)
- Apply Hilt plugin and add dependencies.

#### [NEW] [RupeeFlowApplication.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/RupeeFlowApplication.kt)
- Base application class with `@HiltAndroidApp`.

### [Architecture Reorganization]

#### [MOVE] Existing files into new package structure:
- `data/` -> `core/database/` and `domain/model/`
- `ui/` -> `feature/` and `core/designsystem/` (as applicable)
- `utils/` -> `core/util/`

### [Stage 2: Database Layer]

#### [NEW] Entities in `core/database/entity/`:
- `AccountEntity`
- `TransactionEntity`
- `BudgetEntity`
- `CategoryEntity`
- `MerchantEntity`
- `ReminderEntity`
- `GoalEntity`
- `AttachmentEntity`

#### [NEW] DAOs in `core/database/dao/`:
- `TransactionDao`
- `AccountDao`
- `CategoryDao`
- `BudgetDao`
- etc.

#### [MODIFY] [TransactionDatabase.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/RupeeFlowDatabase.kt)
- Define the Room database with all entities and DAOs.

#### [NEW] [DatabaseModule.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/di/DatabaseModule.kt)
- Hilt module to provide Database and DAO instances.

## Verification Plan

### Automated Tests
- Run `app:assembleDebug` to verify the build and Hilt code generation.
- (Future) Add Room unit tests for the new entities and relationships.

### Manual Verification
- Verify the app still launches and shows the dashboard (after wiring up the new ViewModel injection).
