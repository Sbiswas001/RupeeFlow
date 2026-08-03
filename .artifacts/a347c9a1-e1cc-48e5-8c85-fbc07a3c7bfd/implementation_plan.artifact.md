# Categories Screen Redesign

Redesign the Categories management screen to be more functional, visually appealing, and personalized with icons and colors.

## User Review Required

> [!IMPORTANT]
> The default categories will be updated to include Indian-friendly options as requested. Existing categories will be preserved but might need manual icon/color updates.

## Proposed Changes

### Core Models

#### [MODIFY] [CategoryEntity.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/entity/CategoryEntity.kt)
- Add `type: TransactionType`.
- Add `budget: Double?`.
- Add `isArchived: Boolean`.
- Change `iconRes: Int` to `icon: String` (to support emojis or icon names).

#### [MODIFY] [Category.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/domain/model/Category.kt)
- Update to match `CategoryEntity`.

#### [MODIFY] [CategoryMapper.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/mapper/CategoryMapper.kt)
- Update mapping logic.

### Data Layer

#### [MODIFY] [CategoryDao.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/dao/CategoryDao.kt)
- Add methods for archiving/unarchiving.
- Update search and retrieval queries.

#### [MODIFY] [CategoryRepository.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/domain/repository/CategoryRepository.kt)
- Add CRUD methods matching the new vision.

#### [MODIFY] [CategoryRepositoryImpl.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/core/database/repository/CategoryRepositoryImpl.kt)
- Implement updated repository methods.

### Feature: Categories

#### [NEW] [CategoriesViewModel.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/categories/CategoriesViewModel.kt)
- Manage category list, search, filters, and totals.
- Calculate stats per category (transactions count, total spent/earned).

#### [NEW] [CategoriesScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/categories/CategoriesScreen.kt)
- Main screen with Search, Overview Cards, Grouped List, and FAB.

#### [NEW] [CategoryDetailScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/categories/CategoryDetailScreen.kt)
- Detailed view of a category's stats and recent transactions.

#### [NEW] [AddEditCategoryBottomSheet.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/categories/AddEditCategoryBottomSheet.kt)
- Bottom sheet for creating/editing categories.

### Application Logic

#### [MODIFY] [RupeeFlowApplication.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/RupeeFlowApplication.kt)
- Update default categories to the Indian-friendly list.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/MainActivity.kt)
- Wire up the new screens in the `NavDisplay`.

## Verification Plan

### Automated Tests
- Unit tests for `CategoriesViewModel` to verify filtering and stat calculations.
- DAO tests for new category operations.

### Manual Verification
- Verify category creation with icon and color selection.
- Verify grouping and filtering on the Categories screen.
- Verify long-press actions (archive, delete).
- Verify the search functionality.
