package sayan.apps.rupeeflow.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RupeeFlowDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate20To23WithComplexData() {
        helper.createDatabase(TEST_DB, 20).apply {
            // Insert test accounts
            execSQL("INSERT INTO accounts (id, name, category, subType, balance, lastUpdated) VALUES (1, 'Main Savings', 'BANKING', 'SAVINGS', 15000.0, 1000)")
            execSQL("INSERT INTO accounts (id, name, category, subType, balance, lastUpdated) VALUES (2, 'Wallet', 'CASH_WALLETS', 'CASH', 500.0, 1000)")
            
            // Insert categories
            execSQL("INSERT INTO categories (id, name, icon, colorHex, type) VALUES (1, 'Food', '🍔', '#FF0000', 'EXPENSE')")
            execSQL("INSERT INTO categories (id, name, icon, colorHex, type) VALUES (2, 'Salary', '💰', '#00FF00', 'INCOME')")

            // Insert transactions with foreign keys
            execSQL("INSERT INTO transactions (id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring) VALUES (1, 'Lunch', 250.0, 'EXPENSE', 1, 1, 2000, 'Restaurant', 0)")
            execSQL("INSERT INTO transactions (id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring) VALUES (2, 'Paycheck', 50000.0, 'INCOME', 2, 1, 3000, 'Monthly Salary', 0)")

            // Insert budget
            execSQL("INSERT INTO budgets (id, categoryId, limitAmount, period, startDate) VALUES (1, 1, 5000.0, 'MONTHLY', 1000)")

            // Insert recurring item
            execSQL("INSERT INTO recurring_items (id, title, amount, dueDate, isAutoPay, status, frequency, frequencyInterval, frequencyUnit, category, categoryId) VALUES (1, 'Netflix', 649.0, 4000, 1, 'ACTIVE', 'MONTHLY', 1, 'MONTHS', 'Entertainment', 1)")
            execSQL("INSERT INTO recurring_occurrences (id, recurringItemId, scheduledDate, status, accountId) VALUES (1, 1, 4000, 'PENDING', 1)")

            close()
        }

        // Run migrations from 20 to 23
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            23,
            true,
            RupeeFlowMigrations.MIGRATION_20_21,
            RupeeFlowMigrations.MIGRATION_21_22,
            RupeeFlowMigrations.MIGRATION_22_23
        )

        // Verify data integrity post-migration
        migratedDb.query("SELECT name, balance, isClosed FROM accounts WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Main Savings", cursor.getString(0))
            assertEquals(15000.0, cursor.getDouble(1), 0.001)
            assertEquals(0, cursor.getInt(2)) // Default isClosed = 0
        }

        migratedDb.query("SELECT title, amount, transferId, isIncoming FROM transactions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Lunch", cursor.getString(0))
            assertEquals(250.0, cursor.getDouble(1), 0.001)
            assertEquals(0, cursor.getInt(3)) // isIncoming default = 0
        }

        migratedDb.query("SELECT limitAmount FROM budgets WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5000.0, cursor.getDouble(0), 0.001)
        }

        migratedDb.query("SELECT title, reminderDaysBefore FROM recurring_items WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Netflix", cursor.getString(0))
            assertEquals(1, cursor.getInt(1)) // default reminderDaysBefore = 1
        }

        migratedDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun testBackupAndRestoreCompatibility() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = context.getDatabasePath(TEST_DB)
        if (dbFile.exists()) dbFile.delete()

        // 1. Create v23 database with test data
        helper.createDatabase(TEST_DB, 23).apply {
            execSQL("INSERT INTO accounts (id, name, category, subType, balance, lastUpdated) VALUES (1, 'Backup Test Account', 'BANKING', 'SAVINGS', 9999.0, 1000)")
            close()
        }

        // 2. Simulate Backup (copy db file)
        val backupFile = File(context.cacheDir, "test_backup.db")
        dbFile.inputStream().use { input ->
            backupFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 3. Delete current db file to simulate fresh reinstall or restore over empty
        dbFile.delete()
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        // 4. Simulate Restore (copy backup file back to db path)
        backupFile.inputStream().use { input ->
            dbFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        backupFile.delete()

        // 5. Open database using Room / helper and verify data integrity
        val restoredDb = helper.runMigrationsAndValidate(TEST_DB, 23, true)
        restoredDb.query("SELECT name, balance FROM accounts WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Backup Test Account", cursor.getString(0))
            assertEquals(9999.0, cursor.getDouble(1), 0.001)
        }
        restoredDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate24To25WithPaymentInstruments() {
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL("INSERT INTO accounts (id, name, category, subType, balance, lastUpdated) VALUES (1, 'SBI Bank', 'BANKING', 'SAVINGS', 25000.0, 1000)")
            execSQL("INSERT INTO categories (id, name, icon, colorHex, type) VALUES (1, 'Shopping', '🛍', '#FF0000', 'EXPENSE')")
            execSQL("INSERT INTO transactions (id, title, amount, type, categoryId, accountId, timestamp, note, isRecurring) VALUES (1, 'Amazon Order', 1200.0, 'EXPENSE', 1, 1, 2000, 'Shoes', 0)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            25,
            true,
            RupeeFlowMigrations.MIGRATION_24_25
        )

        migratedDb.query("SELECT COUNT(*) FROM account_upi_apps WHERE isDefault = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
        }

        migratedDb.query("SELECT title, paymentMethodType, debitCardNameSnapshot FROM transactions WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Amazon Order", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }

        migratedDb.close()
    }
}
