package sayan.apps.rupeeflow.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RupeeFlowMigrations {
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN lastReconciledAt INTEGER")
            db.execSQL("ALTER TABLE accounts ADD COLUMN lastReconciledBalance REAL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN previousBalance REAL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN actualBalance REAL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN reconciliationReason TEXT")
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO accounts (id, name, category, subType, balance, lastUpdated) VALUES (1, 'Cash', 'CASH_WALLETS', 'CASH', 0.0, ${System.currentTimeMillis()})")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `recurring_occurrences` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `recurringItemId` INTEGER NOT NULL, 
                    `scheduledDate` INTEGER NOT NULL, 
                    `status` TEXT NOT NULL, 
                    `transactionId` INTEGER, 
                    `paymentDate` INTEGER, 
                    `accountId` INTEGER, 
                    `notes` TEXT, 
                    FOREIGN KEY(`recurringItemId`) REFERENCES `recurring_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , 
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringItemId` ON `recurring_occurrences` (`recurringItemId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_transactionId` ON `recurring_occurrences` (`transactionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_accountId` ON `recurring_occurrences` (`accountId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringItemId_scheduledDate` ON `recurring_occurrences` (`recurringItemId`, `scheduledDate`)")

            db.execSQL("""
                CREATE TABLE `recurring_items_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `dueDate` INTEGER NOT NULL, 
                    `isAutoPay` INTEGER NOT NULL, 
                    `status` TEXT NOT NULL, 
                    `frequency` TEXT NOT NULL, 
                    `frequencyInterval` INTEGER NOT NULL, 
                    `frequencyUnit` TEXT NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `categoryId` INTEGER, 
                    `subCategory` TEXT, 
                    `iconRes` INTEGER, 
                    `reminderDaysBefore` INTEGER NOT NULL, 
                    `paymentMethod` TEXT, 
                    `defaultAccountId` INTEGER, 
                    `notes` TEXT, 
                    `recurrenceCount` INTEGER, 
                    `totalRecurrence` INTEGER, 
                    `outstandingAmount` REAL, 
                    `minimumDue` REAL, 
                    `lastPaidDate` INTEGER, 
                    `lastGeneratedOccurrenceDate` INTEGER
                )
            """)
            
            db.execSQL("""
                INSERT INTO `recurring_items_new` (id, title, amount, dueDate, isAutoPay, status, frequency, frequencyInterval, frequencyUnit, category, categoryId, subCategory, iconRes, reminderDaysBefore, paymentMethod, defaultAccountId, notes, recurrenceCount, totalRecurrence, outstandingAmount, minimumDue, lastPaidDate, lastGeneratedOccurrenceDate)
                SELECT id, title, amount, dueDate, isAutoPay, 
                    CASE 
                        WHEN status IN ('PENDING', 'PAID', 'OVERDUE') THEN 'ACTIVE'
                        ELSE status 
                    END, 
                    frequency, frequencyInterval, frequencyUnit, category, categoryId, subCategory, iconRes, reminderDaysBefore, paymentMethod, accountId, notes, recurrenceCount, totalRecurrence, outstandingAmount, minimumDue, lastPaidDate, dueDate
                FROM `recurring_items`
            """)
            
            db.execSQL("DROP TABLE `recurring_items`")
            db.execSQL("ALTER TABLE `recurring_items_new` RENAME TO `recurring_items`")

            db.execSQL("""
                INSERT INTO `recurring_occurrences` (recurringItemId, scheduledDate, status)
                SELECT id, dueDate, 'PENDING' FROM `recurring_items`
            """)
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE `transactions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `categoryId` INTEGER, 
                    `accountId` INTEGER, 
                    `timestamp` INTEGER NOT NULL, 
                    `note` TEXT, 
                    `isRecurring` INTEGER NOT NULL, 
                    `upiTransactionId` TEXT, 
                    `upiApp` TEXT, 
                    `upiLinkedBank` TEXT, 
                    `previousBalance` REAL, 
                    `actualBalance` REAL, 
                    `reconciliationReason` TEXT, 
                    FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , 
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                )
            """)
            db.execSQL("""
                INSERT INTO `transactions_new` (id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring, upiTransactionId, upiApp, upiLinkedBank, previousBalance, actualBalance, reconciliationReason)
                SELECT id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring, upiTransactionId, upiApp, upiLinkedBank, previousBalance, actualBalance, reconciliationReason
                FROM `transactions`
            """)
            db.execSQL("DROP TABLE `transactions`")
            db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE `transactions_v20` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `categoryId` INTEGER, 
                    `accountId` INTEGER, 
                    `timestamp` INTEGER NOT NULL, 
                    `note` TEXT, 
                    `isRecurring` INTEGER NOT NULL, 
                    `upiTransactionId` TEXT, 
                    `upiApp` TEXT, 
                    `upiLinkedBank` TEXT, 
                    `previousBalance` REAL, 
                    `actualBalance` REAL, 
                    `reconciliationReason` TEXT, 
                    `accountNameSnapshot` TEXT, 
                    `accountCategorySnapshot` TEXT, 
                    FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , 
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                )
            """)
            db.execSQL("""
                INSERT INTO `transactions_v20` (id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring, upiTransactionId, upiApp, upiLinkedBank, previousBalance, actualBalance, reconciliationReason, accountNameSnapshot, accountCategorySnapshot)
                SELECT id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring, upiTransactionId, upiApp, upiLinkedBank, previousBalance, actualBalance, reconciliationReason, NULL, NULL
                FROM `transactions`
            """)
            db.execSQL("DROP TABLE `transactions`")
            db.execSQL("ALTER TABLE `transactions_v20` RENAME TO `transactions`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")

            db.execSQL("""
                CREATE TABLE `recurring_occurrences_v20` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `recurringItemId` INTEGER, 
                    `scheduledDate` INTEGER NOT NULL, 
                    `status` TEXT NOT NULL, 
                    `transactionId` INTEGER, 
                    `paymentDate` INTEGER, 
                    `accountId` INTEGER, 
                    `notes` TEXT, 
                    `recurringItemNameSnapshot` TEXT, 
                    `accountNameSnapshot` TEXT, 
                    FOREIGN KEY(`recurringItemId`) REFERENCES `recurring_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , 
                    FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , 
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                )
            """)
            db.execSQL("""
                INSERT INTO `recurring_occurrences_v20` (id, recurringItemId, scheduledDate, status, transactionId, paymentDate, accountId, notes, recurringItemNameSnapshot, accountNameSnapshot)
                SELECT id, recurringItemId, scheduledDate, status, transactionId, paymentDate, accountId, notes, NULL, NULL
                FROM `recurring_occurrences`
            """)
            db.execSQL("DROP TABLE `recurring_occurrences`")
            db.execSQL("ALTER TABLE `recurring_occurrences_v20` RENAME TO `recurring_occurrences`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringItemId` ON `recurring_occurrences` (`recurringItemId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_transactionId` ON `recurring_occurrences` (`transactionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_accountId` ON `recurring_occurrences` (`accountId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringItemId_scheduledDate` ON `recurring_occurrences` (`recurringItemId`, `scheduledDate`)")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE accounts ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transferId TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transferAccountId INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transferAccountNameSnapshot TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isIncoming INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transferId` ON `transactions` (`transferId`)")
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val columnsToAdd = listOf(
                "ALTER TABLE accounts ADD COLUMN institutionName TEXT",
                "ALTER TABLE accounts ADD COLUMN accountNumberLast4 TEXT",
                "ALTER TABLE accounts ADD COLUMN creditLimit REAL",
                "ALTER TABLE accounts ADD COLUMN interestRate REAL",
                "ALTER TABLE accounts ADD COLUMN maturityDate INTEGER",
                "ALTER TABLE accounts ADD COLUMN principalAmount REAL",
                "ALTER TABLE accounts ADD COLUMN tenureMonths INTEGER",
                "ALTER TABLE accounts ADD COLUMN upiId TEXT",
                "ALTER TABLE accounts ADD COLUMN colorHex TEXT",
                "ALTER TABLE categories ADD COLUMN budget REAL",
                "ALTER TABLE categories ADD COLUMN parentCategoryId INTEGER",
                "ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE budgets ADD COLUMN notified80Percent INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE budgets ADD COLUMN notified100Percent INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE budgets ADD COLUMN lastNotifiedPeriodStart INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE goals ADD COLUMN notified50Percent INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE goals ADD COLUMN notified90Percent INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE goals ADD COLUMN notified100Percent INTEGER NOT NULL DEFAULT 0"
            )
            for (sql in columnsToAdd) {
                try {
                    db.execSQL(sql)
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val columnsToAdd = listOf(
                "ALTER TABLE recurring_items ADD COLUMN subCategory TEXT",
                "ALTER TABLE recurring_items ADD COLUMN iconRes INTEGER",
                "ALTER TABLE recurring_items ADD COLUMN reminderDaysBefore INTEGER NOT NULL DEFAULT 1",
                "ALTER TABLE recurring_items ADD COLUMN paymentMethod TEXT",
                "ALTER TABLE recurring_items ADD COLUMN defaultAccountId INTEGER",
                "ALTER TABLE recurring_items ADD COLUMN notes TEXT",
                "ALTER TABLE recurring_items ADD COLUMN recurrenceCount INTEGER",
                "ALTER TABLE recurring_items ADD COLUMN totalRecurrence INTEGER",
                "ALTER TABLE recurring_items ADD COLUMN outstandingAmount REAL",
                "ALTER TABLE recurring_items ADD COLUMN minimumDue REAL",
                "ALTER TABLE recurring_items ADD COLUMN lastPaidDate INTEGER",
                "ALTER TABLE recurring_items ADD COLUMN lastGeneratedOccurrenceDate INTEGER",
                "ALTER TABLE recurring_occurrences ADD COLUMN notes TEXT",
                "ALTER TABLE recurring_occurrences ADD COLUMN recurringItemNameSnapshot TEXT",
                "ALTER TABLE recurring_occurrences ADD COLUMN accountNameSnapshot TEXT",
                "ALTER TABLE transactions ADD COLUMN accountNameSnapshot TEXT",
                "ALTER TABLE transactions ADD COLUMN accountCategorySnapshot TEXT"
            )
            for (sql in columnsToAdd) {
                try {
                    db.execSQL(sql)
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `goal_contributions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `goalId` INTEGER NOT NULL,
                    `amount` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `note` TEXT,
                    FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_goalId` ON `goal_contributions` (`goalId`)")
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `debit_cards` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER NOT NULL,
                    `cardName` TEXT NOT NULL,
                    `last4Digits` TEXT NOT NULL,
                    `network` TEXT,
                    `nickname` TEXT,
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_debit_cards_accountId` ON `debit_cards` (`accountId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `account_upi_apps` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `accountId` INTEGER,
                    `appName` TEXT NOT NULL,
                    `appPackage` TEXT,
                    `isCustom` INTEGER NOT NULL DEFAULT 0,
                    `isDefault` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_account_upi_apps_accountId` ON `account_upi_apps` (`accountId`)")

            db.execSQL("INSERT OR IGNORE INTO `account_upi_apps` (appName, isCustom, isDefault) VALUES ('Google Pay', 0, 1)")
            db.execSQL("INSERT OR IGNORE INTO `account_upi_apps` (appName, isCustom, isDefault) VALUES ('PhonePe', 0, 1)")
            db.execSQL("INSERT OR IGNORE INTO `account_upi_apps` (appName, isCustom, isDefault) VALUES ('Paytm', 0, 1)")
            db.execSQL("INSERT OR IGNORE INTO `account_upi_apps` (appName, isCustom, isDefault) VALUES ('Amazon Pay', 0, 1)")
            db.execSQL("INSERT OR IGNORE INTO `account_upi_apps` (appName, isCustom, isDefault) VALUES ('BHIM', 0, 1)")

            val columnsToAdd = listOf(
                "ALTER TABLE `transactions` ADD COLUMN `paymentMethodType` TEXT",
                "ALTER TABLE `transactions` ADD COLUMN `debitCardId` INTEGER",
                "ALTER TABLE `transactions` ADD COLUMN `debitCardNameSnapshot` TEXT",
                "ALTER TABLE `transactions` ADD COLUMN `debitCardLast4Snapshot` TEXT",
                "ALTER TABLE `transactions` ADD COLUMN `upiAppNameSnapshot` TEXT"
            )
            for (sql in columnsToAdd) {
                try {
                    db.execSQL(sql)
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_debitCardId` ON `transactions` (`debitCardId`)")
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_23_24,
        MIGRATION_24_25
    )
}
