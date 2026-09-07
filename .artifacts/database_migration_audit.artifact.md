# RupeeFlow Database Migration Safety Audit & Review Report

## 1. Database Version & Chain Overview
- **Current Database Version**: `23`
- **Minimum Supported Database Version**: `15` (Earlier versions < 15 are obsolete / unsupported; upgrading from < 15 without a migration would require fallback, but version 15+ represents all known released schema states).
- **Complete Migration Chain**:
  `15 → 16 → 17 → 18 → 19 → 20 → 21 → 22 → 23`

---

## 2. Migration Chain & Schema Verification

### Migration 15 → 16 (`MIGRATION_15_16`)
- **Changes**: Added reconciliation columns to `accounts` (`lastReconciledAt`, `lastReconciledBalance`) and `transactions` (`previousBalance`, `actualBalance`, `reconciliationReason`).
- **Safety**: Uses non-destructive `ALTER TABLE ... ADD COLUMN`. Existing rows and IDs are preserved.

### Migration 16 → 17 (`MIGRATION_16_17`)
- **Changes**: Inserts default Cash wallet into `accounts` (`id = 1`) if not already present.
- **Safety**: Uses `INSERT OR IGNORE`. Existing user data and accounts are fully preserved.

### Migration 17 → 18 (`MIGRATION_17_18`)
- **Changes**: Created `recurring_occurrences` table and restructured `recurring_items` table.
- **Safety**: Creates new table, copies existing records with safe column mapping and default status (`ACTIVE`), drops old table, and renames new table. Foreign keys and indexes are correctly re-created.

### Migration 18 → 19 (`MIGRATION_18_19`)
- **Changes**: Restructured `transactions` table to make `accountId` nullable with `ON DELETE SET NULL`.
- **Safety**: Safe table recreation pattern (`transactions_new` → copy data → drop old → rename). Foreign keys and indexes preserved.

### Migration 19 → 20 (`MIGRATION_19_20`)
- **Changes**: Added snapshot columns (`accountNameSnapshot`, `accountCategorySnapshot`) to `transactions` and `recurring_occurrences`, and made `recurringItemId` nullable in `recurring_occurrences`.
- **Safety**: Safe table recreation pattern preserving existing records and setting new snapshot columns to `NULL`.

### Migration 20 → 21 (`MIGRATION_20_21`)
- **Changes**: Added `isClosed` to `accounts` and transfer fields (`transferId`, `transferAccountId`, `transferAccountNameSnapshot`, `isIncoming`) to `transactions`.
- **Safety**: Non-destructive `ALTER TABLE` statements with explicit defaults.

### Migration 21 → 22 (`MIGRATION_21_22`)
- **Changes**: Added account details (`institutionName`, `accountNumberLast4`, `creditLimit`, `interestRate`, `maturityDate`, `principalAmount`, `tenureMonths`, `upiId`, `colorHex`), category extensions (`budget`, `parentCategoryId`, `isDeleted`), budget notification flags (`notified80Percent`, `notified100Percent`, `lastNotifiedPeriodStart`), and goal notification flags (`notified50Percent`, `notified90Percent`, `notified100Percent`).
- **Safety**: Safe `ALTER TABLE` statements with try/catch robustness and appropriate defaults (`isDeleted`, notification flags default to `0`). Existing rows, IDs, relationships, and custom categories remain 100% intact.

### Migration 22 → 23 (`MIGRATION_22_23`)
- **Changes**: Added recurring item metadata (`subCategory`, `iconRes`, `reminderDaysBefore`, `paymentMethod`, `defaultAccountId`, `notes`, `recurrenceCount`, `totalRecurrence`, `outstandingAmount`, `minimumDue`, `lastPaidDate`, `lastGeneratedOccurrenceDate`), recurring occurrence notes/snapshots (`notes`, `recurringItemNameSnapshot`, `accountNameSnapshot`), and transaction snapshots (`accountNameSnapshot`, `accountCategorySnapshot`).
- **Safety**: Safe `ALTER TABLE` statements with defaults (`reminderDaysBefore` defaults to `1`). Existing records and relationships remain valid.

---

## 3. Migration Risks Addressed
- **Destructive Fallback Removed**: `.fallbackToDestructiveMigration()` was completely removed from `DatabaseModule.kt`. Room will now fail safely rather than silently dropping tables if a migration is missing.
- **Hidden Destructive Recovery**: Codebase audit confirmed zero instances of silent database deletion, reset, or recreation on error.

---

## 4. Migration Tests & Verification
- **Test Coverage**: Implemented `MigrationTest.kt` using Room's `MigrationTestHelper`.
- **Data Preservation Testing**: Tests insert representative real-world data (multiple accounts, transactions with foreign keys, custom categories, recurring items, budgets) at version 20, execute migrations up to version 23, and assert exact data preservation post-migration.
- **Status**: All migration tests compile and pass successfully.

---

## 5. Backup & Restore Compatibility
- **Implementation**: RupeeFlow backs up and restores the raw SQLite database file (`rupeeflow_backup.db`).
- **Testing**: A dedicated test (`testBackupAndRestoreCompatibility`) in `MigrationTest.kt` simulates backing up a version 23 database, clearing storage, restoring the raw SQLite file, opening it with Room, and validating data integrity.
- **Status**: Tested and verified working.

---

## 6. Ways User Data Could Still Be Lost (Outside Room Migrations)
1. **Manual App Uninstall**: Uninstalling the Android application deletes the app's private sandbox storage (including SQLite databases and DataStore). This is standard Android OS sandbox behavior and cannot be prevented by Room.
2. **Manual "Clear Data"**: Going to Android Device Settings → App Info → Storage → "Clear Data" wipes app storage.
3. **Signing Key Mismatch / Reinstallation**: Installing an APK signed with a different keystore (e.g., switching between debug and release certificates or different developer keys) forces Android to treat it as a new installation.
