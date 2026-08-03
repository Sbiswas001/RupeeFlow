# Implementation Plan - Task 3: Transactions List & Add Form

This plan covers the development of the Transactions list screen and the Add Transaction form with ViewModel state management.

## Proposed Changes

### 1. Data Layer & Repository
- Update `TransactionRepository` to provide dummy transactions and allow adding new ones.

### 2. Navigation
- Add `AddTransaction` route to `NavRoutes.kt`.
- Update `MainActivity.kt` to include the `AddTransaction` screen in the navigation graph.

### 3. ViewModels
- Implement `TransactionsViewModel`:
    - Expose a `StateFlow<List<Transaction>>` of transactions.
- Implement `AddTransactionViewModel`:
    - Manage form state: `amount`, `isIncome`, `category`.
    - Handle submission logic.

### 4. UI Components (Jetpack Compose)
- **TransactionsScreen**:
    - Scrollable list of transactions.
    - Transaction items styled with Emerald Green for income and Vibrant Red for expenses.
    - Floating Action Button (FAB) to navigate to the Add Transaction screen.
- **AddTransactionScreen**:
    - Large text field for amount.
    - Segmented toggle for Income/Expense.
    - Dropdown menu for category selection.
    - "Save" button to add the transaction and navigate back.

## File Modifications

#### [MODIFY] [NavRoutes.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/navigation/NavRoutes.kt)
- Add `AddTransaction` route.

#### [MODIFY] [TransactionRepository.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/data/repository/TransactionRepository.kt)
- Implement dummy data and `addTransaction` method.

#### [MODIFY] [TransactionViewModel.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/viewmodel/TransactionViewModel.kt)
- Rename to `TransactionsViewModel` (plural) or keep and implement logic. I'll use `TransactionsViewModel` for the list and create a new `AddTransactionViewModel`.

#### [NEW] [AddTransactionViewModel.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/viewmodel/AddTransactionViewModel.kt)
- New ViewModel for the form.

#### [MODIFY] [TransactionsScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/screens/TransactionsScreen.kt)
- Implement the list UI and FAB.

#### [NEW] [AddTransactionScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/screens/AddTransactionScreen.kt)
- New screen for adding transactions.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/MainActivity.kt)
- Register `AddTransaction` screen in `NavDisplay`.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.

### Manual Verification
- Navigate to the Transactions screen and verify the list is displayed.
- Click the FAB to open the Add Transaction screen.
- Fill out the form and click "Save".
- Verify the new transaction is added to the list (locally for now).
- Check color consistency (Emerald Green for Income, Vibrant Red for Expense).
- Verify One UI dark theme consistency.
