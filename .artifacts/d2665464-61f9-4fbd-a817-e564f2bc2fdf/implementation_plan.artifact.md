# Implementation Plan: Finalizing RupeeFlow Features

This plan covers the implementation of all remaining functional modules and UI refinements to bring the app to a "Feature Complete" state.

## 1. Net Worth History (Visual Insights)
- **Goal**: Add a historical line chart to the Net Worth screen showing assets vs liabilities over time.
- **Method**: Back-calculate net worth using transaction history from current account balances.

## 2. Archived System
- **Goal**: Replace the "Archived" placeholder with a screen to manage hidden categories and accounts.
- **Functionality**: View archived items, restore them, or permanently delete them.

## 3. Data Portability (Backup/Restore & Import/Export)
- **Goal**: Implement JSON-based backup/restore and CSV transaction export.
- **Backup**: Full database dump to a JSON file.
- **Export**: Generate a CSV of transactions for the selected period.

## 4. Notifications UI
- **Goal**: Implement a functional settings screen for managing financial reminders.

## Proposed Changes

### Data Layer
- **[MODIFY] [TransactionDao.kt]**: Add queries for range-based calculations.
- **[MODIFY] [AccountRepository.kt]**: Add methods for fetching historical balances.

### UI Layer
- **[NEW] [ArchivedScreen.kt]**: Screen for managing archived entities.
- **[NEW] [BackupRestoreScreen.kt]**: UI for local data management.
- **[NEW] [ImportExportScreen.kt]**: UI for CSV/JSON data movement.
- **[NEW] [NotificationsScreen.kt]**: UI for notification preferences.
- **[MODIFY] [NetWorthScreen.kt]**: Add the `LineChart` component for history.

## Verification Plan
- Build and run the app.
- Verify each newly implemented screen is accessible and functional.
- Verify CSV export generates a valid file.
- Verify Net Worth chart accurately reflects historical transaction trends.
