# RupeeFlow 💸

RupeeFlow is a modern, feature-rich personal finance tracker built natively for Android using Jetpack Compose and Material 3. It helps users effortlessly manage bank accounts, track daily expenses and income, monitor budgets and savings goals, handle recurring subscriptions, and analyze financial health with powerful charts and widgets.

---

🚧 **Project Status: Active Development**  
*Features and capabilities are actively evolving to provide an even more seamless personal finance experience.*

---

## ✨ Highlights

RupeeFlow is designed around a unified view of personal finances rather than simply recording expenses. Bank accounts act as the source of truth for balances and linked payment instruments, while dashboards, budgets, recurring transactions, financial health metrics, and widgets provide actionable insight into day-to-day finances.

---

## 📱 Screenshots / UI Previews

<p align="center">
  <img width="260" alt="Dashboard Screen" src="https://github.com/user-attachments/assets/3b8553ac-9bcb-44ad-9934-22b0513dcd89" />
  &nbsp;&nbsp;
  <img width="260" alt="Insights Screen" src="https://github.com/user-attachments/assets/7b28b16c-304f-405e-9dab-3e64c2d5fa00" />
  &nbsp;&nbsp;
  <img width="260" alt="Analytics Screen" src="https://github.com/user-attachments/assets/d7310e4b-a8cc-4da3-a9e0-f3f2a2db7e72" />
</p>

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

### 🧠 Financial Health Scoring
- Four-pillar financial health scoring model (0–100) designed to provide an explainable assessment of financial well-being:
  1. **Spending Control (25%)**: Evaluates budget adherence and spending velocity.
  2. **Cash Flow (25%)**: Measures income versus expenditure balance.
  3. **Savings (30%)**: Tracks savings rate relative to total income.
  4. **Commitment Load (20%)**: Assesses recurring bills and fixed commitments.
- Provides actionable insights and advice pills rather than relying on a single raw spending metric.

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

## 🏛️ Core Architecture

RupeeFlow follows Clean Architecture principles with a reactive Unidirectional Data Flow (UDF):

```
UI (Jetpack Compose / Glance Widgets)
        ↓
ViewModels (StateFlow / UDF)
        ↓
Use Cases / Domain Logic (Financial Calculators & Engines)
        ↓
Repositories (Hilt DI & Data Sources)
        ↓
Room Database / DataStore Preferences
```

---

## 🛠️ Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/) design system.
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow & Clean Architecture.
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/).
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) with robust migrations and schema testing.
- **Asynchronous Processing**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html).
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for automated recurring transactions and cloud sync.
- **Widgets**: [Jetpack Glance](https://developer.android.com/jetpack/androidx/releases/glance) for modern Compose-based App Widgets.
- **Navigation**: Jetpack Navigation 3 & Hilt Navigation Compose.

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
