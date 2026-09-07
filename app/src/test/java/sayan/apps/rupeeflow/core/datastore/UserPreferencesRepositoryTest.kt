package sayan.apps.rupeeflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepositoryImpl

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { temporaryFolder.newFile("user_prefs_${System.nanoTime()}.preferences_pb") }
        )
        repository = UserPreferencesRepositoryImpl(dataStore)
    }

    @Test
    fun `amoled black default should be false`() = runTest(testDispatcher) {
        val prefs = repository.userPreferences.first()
        assertFalse(prefs.amoledBlack)
    }

    @Test
    fun `dynamic color default should be false`() = runTest(testDispatcher) {
        val prefs = repository.userPreferences.first()
        assertFalse(prefs.dynamicColor)
    }

    @Test
    fun `updating amoled black persists correctly`() = runTest(testDispatcher) {
        repository.updateAmoledBlack(true)
        val prefs = repository.userPreferences.first()
        assertEquals(true, prefs.amoledBlack)
        
        repository.updateAmoledBlack(false)
        val prefs2 = repository.userPreferences.first()
        assertEquals(false, prefs2.amoledBlack)
    }

    @Test
    fun `updating dynamic color persists correctly`() = runTest(testDispatcher) {
        repository.updateDynamicColor(true)
        val prefs = repository.userPreferences.first()
        assertEquals(true, prefs.dynamicColor)
        
        repository.updateDynamicColor(false)
        val prefs2 = repository.userPreferences.first()
        assertEquals(false, prefs2.dynamicColor)
    }
}
