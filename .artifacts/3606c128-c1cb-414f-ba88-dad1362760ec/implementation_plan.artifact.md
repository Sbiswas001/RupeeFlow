# Implementation Plan - Task 4: Adaptive & Verify

Implement adaptive layouts for different form factors (phone vs tablet/foldable) and perform a final UI consistency check for RupeeFlow.

## Proposed Changes

### [Component] Adaptive Layouts

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/screens/DashboardScreen.kt)
- Update `DashboardScreen` to use `WindowWidthSizeClass`.
- On expanded screens, use a grid layout for "Recent Transactions" or side-by-side components.

#### [MODIFY] [TransactionsScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/screens/TransactionsScreen.kt)
- Integrate `ListDetailPaneScaffold` to support dual-pane view on tablets/foldables.
- Show the transaction list and details (or "Add Transaction" form) side-by-side on large screens.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/MainActivity.kt)
- Pass window size class or use `currentWindowAdaptiveInfo()` to provide adaptive context to screens.

### [Component] UI Polish & Consistency

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/screens/DashboardScreen.kt) & [TransactionsScreen.kt](file:///C:/Users/Sayan Biswas/AndroidStudioProjects/RupeeFlow/app/src/main/java/sayan/apps/rupeeflow/ui/screens/TransactionsScreen.kt)
- Ensure all headers are large and bold (One UI style).
- Verify pure black background and elevated surface cards.

## Verification Plan

### Automated Tests
- Build the app: `./gradlew :app:assembleDebug`

### Manual Verification
- Verify layout behavior on different emulators (Phone, Tablet, Foldable).
- Check color consistency (pure black background).
