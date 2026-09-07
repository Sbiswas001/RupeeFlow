# RupeeFlow 💸

RupeeFlow is a modern, feature-rich personal finance tracker built natively for Android using Jetpack Compose and Material 3. It helps users effortlessly manage bank accounts, track daily expenses and income, monitor budgets and savings goals, handle recurring subscriptions, and analyze financial health with powerful charts and widgets.

---

## 🌟 Key Features

### 📊 Dashboard & Overview
- Real-time Net Worth calculation across all bank accounts and payment instruments.
- Monthly cash flow summary (Income vs. Expenses).
- Quick access to recent transactions, active budgets, and upcoming bills.

### 💳 Accounts & Payment Instruments
- Multi-account management (Savings, Current, Credit Cards, Cash, Wallets).
- UPI Apps & Debit Card management with deduplication.

### 💸 Transactions & Categories
- Fast and intuitive transaction logging (Income, Expense, Transfer) with custom categories and tags.
- Advanced search and filtering options by date range, payment method, category, and amount.

### 🎯 Budgets & Financial Goals
- Set and monitor monthly category-wise spending budgets with progress bars.
- Savings goals tracker to plan for future milestones.

### 🔄 Recurring Transactions & Subscriptions
- Automatic tracking and reminders for recurring bills, subscriptions, and auto-payments.
- Background processing via WorkManager for timely notifications and payment updates.

### 📈 Analytics & Reports
- Comprehensive spending breakdown by category and merchant.
- Visual data representations using custom Bar Charts and Line Charts.
- Financial health scoring and forecasting.

### 🔒 Security & Privacy
- **App Lock**: Biometric and passcode protection to secure your financial data.
- **Privacy Mode**: Toggle visibility mask for sensitive currency and balance amounts.

### 🧩 Home Screen Widgets (Jetpack Glance)
- **Balance Widget**: Quick glance at total net worth and account balances.
- **Spending Widget**: Visual progress bar of monthly budget usage.
- **Upcoming Widget**: Glance at upcoming bills with direct "Mark Paid" actions.

### ☁️ Backup & Restore
- Google Drive integration for seamless cloud backups and data restoration.
- Local backup support.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/) design system.
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow and Clean Architecture principles.
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/).
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with robust migrations and schema testing.
- **Asynchronous Processing**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html).
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for automated recurring transactions and cloud sync.
- **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/androidx/releases/glance) for modern Compose-based App Widgets.
- **Navigation**: Jetpack Navigation 3 & Hilt Navigation Compose.

---

## 📱 Screenshots / UI Previews

<p align="center">
  <img src="screenshots/dashboard.png" width="300" alt="Dashboard Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="screenshots/insights.png" width="300" alt="Insights Screen" />
</p>

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Sbiswas001/RupeeFlow.git
   ```
2. Open the project in [Android Studio](https://developer.android.com/studio) (Ladybug / Koala or newer recommended).
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device (Min SDK: 29, Target/Compile SDK: 37).

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
