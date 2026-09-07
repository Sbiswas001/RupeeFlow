# Implement Tags System

This plan covers the implementation of a Many-to-Many Tagging system for transactions. This includes the database layer, domain models, repositories, and the UI for managing tags.

## User Review Required

> [!IMPORTANT]
> Transactions can have multiple tags. Deleting a tag will remove it from all associated transactions but will not delete the transactions themselves.

## Proposed Changes

### Data Layer

#### [MODIFY] [RupeeFlowDatabase.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/RupeeFlowDatabase.kt)
Add `TagEntity` and `TransactionTagCrossRef` to the database and define `TagDao`.

#### [NEW] [TagDao.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/dao/TagDao.kt)
Define DAO for tag operations, including many-to-many relationship queries.

#### [NEW] [TagRepositoryImpl.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/repository/TagRepositoryImpl.kt)
Implement `TagRepository`.

---

### Domain Layer

#### [NEW] [Tag.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/domain/model/Tag.kt)
Define domain model for Tag.

#### [NEW] [TagRepository.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/domain/repository/TagRepository.kt)
Define the repository interface.

---

### Feature Layer

#### [NEW] [TagsViewModel.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/tags/TagsViewModel.kt)
ViewModel for managing tags.

#### [NEW] [TagsScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/tags/TagsScreen.kt)
UI for managing tags (List, Add, Delete).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/MainActivity.kt)
Replace `PlaceholderScreen("Tags")` with `TagsScreen()`.

## Verification Plan

### Automated Tests
- Verify build success.
- (Optional) Verify Room schema migration if applicable (though we are in active dev).

### Manual Verification
- Navigate to "Tags" from the sidebar.
- Create a few tags with different colors.
- Delete a tag.
- (Future task) Verify tags appear in the transaction details/add transaction screens.
