package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun favoritesPersistAcrossRepositoryInstancesUsingDataStore() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val dataStoreFile = File(tempFolder.root, "lankatv_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope.backgroundScope,
            produceFile = { dataStoreFile },
        )

        val repo = PreferencesRepository(dataStore)
        val initial = repo.userPreferences.first()
        assertTrue("rupavahini" in initial.favoriteChannelIds)

        // Add Sirasa TV and remove TV Derana
        repo.toggleFavorite("sirasa_tv")
        repo.toggleFavorite("tv_derana")

        // Read back through a fresh repository backed by the same DataStore
        val repoReloaded = PreferencesRepository(dataStore)
        val updated = repoReloaded.userPreferences.first()
        assertTrue("sirasa_tv" in updated.favoriteChannelIds)
        assertTrue("rupavahini" in updated.favoriteChannelIds)
        assertFalse("tv_derana" in updated.favoriteChannelIds)

        // Clear all favorites and verify empty set persists
        repoReloaded.clearFavorites()
        val cleared = repoReloaded.userPreferences.first()
        assertEquals(emptySet<String>(), cleared.favoriteChannelIds)
    }
}
