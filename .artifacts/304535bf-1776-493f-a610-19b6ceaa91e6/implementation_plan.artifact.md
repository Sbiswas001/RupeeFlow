# Implementation Plan: Merge Recurring into Activity Destinations

This plan refactors the navigation architecture to move "Recurring" payments into the "Activity" destination as a tabbed section. This simplifies the bottom navigation from 5 to 4 items and centralizes all money movement activity.

## User Review Required

> [!IMPORTANT]
> The "Recurring" bottom navigation item will be removed. Users will now access recurring payments via the new "Recurring" tab within the "Activity" screen.

> [!NOTE]
> We will use a `HorizontalPager` with a `PrimaryTabRow` to provide smooth sliding transitions between "Transactions" and "Recurring" as requested.

## Proposed Changes

### Navigation Core

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/MainActivity.kt)
- Update `OneUIBottomNavigation` to remove the `Recurring` item.
- Update `RupeeFlowDrawerContent` to remove/update the `Recurring` item.
- Update `AppContent`'s `entry<Activity>` to use the new `ActivityScreen`.

### Activity Feature

#### [NEW] [ActivityScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/transactions/ActivityScreen.kt)
- Implement a screen with `PrimaryTabRow` containing "Transactions" and "Recurring".
- Use `HorizontalPager` to host `TransactionsListContent` and `RecurringTabContent`.
- Provide a unified `Scaffold` with a context-aware FAB (Adds Transaction in first tab, Recurring in second).

#### [MODIFY] [TransactionsScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/transactions/TransactionsScreen.kt)
- Export `TransactionsListContent` (removing the internal `Scaffold` and headline) to be used by `ActivityScreen`.

### Recurring Feature

#### [MODIFY] [RecurringScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/feature/recurring/RecurringScreen.kt)
- Export `RecurringTabContent` (removing the internal `Scaffold` and headline) to be used by `ActivityScreen`.
- Ensure consistent styling with the "Activity" destination.

## Verification Plan

### Automated Tests
- Run `gradlew :app:assembleDebug` to ensure compilation.

### Manual Verification
- Deploy to device/emulator.
- Verify Bottom Navigation now has only 4 items.
- Navigate to "Activity" and verify the "Transactions" and "Recurring" tabs are present.
- Swipe between tabs and ensure the `PrimaryTabRow` indicator updates correctly.
- Test the FAB in both tabs to ensure they open the correct "Add" screen.
- Verify deep links (if any) still work or redirect appropriately.
