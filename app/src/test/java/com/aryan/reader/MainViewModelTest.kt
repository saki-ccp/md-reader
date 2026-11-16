package com.aryan.reader

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import androidx.work.WorkManager
import androidx.lifecycle.viewModelScope
import com.aryan.reader.paginatedreader.data.BookCacheDatabase
import com.aryan.reader.tts.TtsController
import com.aryan.reader.tts.TtsPlaybackManager
import com.aryan.reader.data.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MainViewModel
    private lateinit var mockApplication: Application
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private val billingStateFlow = MutableStateFlow(ProUpgradeState())
    private val customFontsFlow = MutableStateFlow<List<CustomFontEntity>>(emptyList())

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        Dispatchers.setMain(testDispatcher)

        mockApplication = mockk()
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        val mockResources = mockk<Resources>(relaxed = true)

        every { mockApplication.applicationContext } returns mockApplication
        every { mockApplication.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockApplication.resources } returns mockResources
        every { mockApplication.filesDir } returns temporaryFolder.newFolder("files")
        every { mockApplication.cacheDir } returns temporaryFolder.newFolder("cache")
        every { mockApplication.getString(any()) } answers { "resource-${firstArg<Int>()}" }
        every { mockPrefs.edit() } returns mockEditor

        every { mockPrefs.getString(any(), any()) } answers { secondArg() as String? }
        every { mockPrefs.getBoolean(any(), any()) } answers { secondArg() as Boolean }
        every { mockPrefs.getInt(any(), any()) } answers { secondArg() as Int }
        every { mockPrefs.getFloat(any(), any()) } answers { secondArg() as Float }

        mockkObject(AppDatabase.Companion)
        val mockDb = mockk<AppDatabase>(relaxed = true)
        every { AppDatabase.getDatabase(any()) } returns mockDb
        mockkObject(BookCacheDatabase.Companion)
        every { BookCacheDatabase.getDatabase(any()) } returns mockk(relaxed = true)

        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)
        mockkStatic(PDFBoxResourceLoader::class)
        every { PDFBoxResourceLoader.init(any()) } just Runs

        mockkConstructor(AuthRepository::class)
        mockkConstructor(RecentFilesRepository::class)
        mockkConstructor(BillingClientWrapper::class)
        mockkConstructor(RemoteConfigRepository::class)
        mockkConstructor(FirestoreRepository::class)
        mockkConstructor(FeedbackRepository::class)
        mockkConstructor(FontsRepository::class)
        mockkConstructor(TtsController::class)
        every { anyConstructed<TtsController>().ttsState } returns MutableStateFlow(TtsPlaybackManager.TtsState())
        every { anyConstructed<TtsController>().connect() } just Runs

        every { anyConstructed<BillingClientWrapper>().proUpgradeState } returns billingStateFlow
        every { anyConstructed<AuthRepository>().getSignedInUser() } returns null
        every { anyConstructed<AuthRepository>().observeAuthState() } returns flowOf(null)
        every { anyConstructed<RecentFilesRepository>().getRecentFilesFlow() } returns flowOf(emptyList())
        every { anyConstructed<RecentFilesRepository>().activeShelvesFlow } returns flowOf(emptyList())
        every { anyConstructed<RecentFilesRepository>().shelfCrossRefsFlow } returns flowOf(emptyList())
        every { anyConstructed<RecentFilesRepository>().tagsFlow } returns flowOf(emptyList())
        every { anyConstructed<RecentFilesRepository>().tagCrossRefsFlow } returns flowOf(emptyList())

        coEvery { anyConstructed<RecentFilesRepository>().migrateLegacyShelvesToRoom() } just Runs
        coEvery { anyConstructed<RecentFilesRepository>().seedTagsIfEmpty(any()) } just Runs
        coEvery { anyConstructed<RecentFilesRepository>().getAllFilesForSync() } returns emptyList()

        every { anyConstructed<FontsRepository>().getAllFonts() } returns customFontsFlow

        viewModel = MainViewModel(mockApplication)
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `search query updates uiState when search is active`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setSearchActive(true)
        viewModel.onSearchQueryChange("Moby Dick")
        val state = viewModel.uiState.first { it.searchQuery == "Moby Dick" }
        assertEquals("Moby Dick", state.searchQuery)
        assertTrue(state.isSearchActive)
    }

    @Test
    fun `setSearchActive false clears the search query`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setSearchActive(true)
        viewModel.onSearchQueryChange("Android")
        viewModel.uiState.first { it.searchQuery == "Android" }
        viewModel.setSearchActive(false)
        val state = viewModel.uiState.first { !it.isSearchActive }
        assertEquals("", state.searchQuery)
        assertFalse(state.isSearchActive)
    }

    @Test
    fun `switching theme updates internal state and preferences`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setAppThemeMode(AppThemeMode.DARK)
        val state = viewModel.uiState.first { it.appThemeMode == AppThemeMode.DARK }
        assertEquals(AppThemeMode.DARK, state.appThemeMode)
        verify { mockEditor.putString("app_theme_mode", AppThemeMode.DARK.name) }
    }

    @Test
    fun `setTabsEnabled persists to shared preferences`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setTabsEnabled(true)
        val state = viewModel.uiState.first { it.isTabsEnabled }
        assertTrue(state.isTabsEnabled)
        verify { mockEditor.putBoolean("tabs_enabled", true) }
    }

    @Test
    fun `banner message logic works correctly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.showBanner("Test Message", isError = true)
        val currentBanner = viewModel.uiState.first { it.bannerMessage != null }.bannerMessage
        assertEquals("Test Message", currentBanner?.message)
        assertTrue(currentBanner?.isError == true)

        viewModel.bannerMessageShown()
        assertEquals(null, viewModel.uiState.first { it.bannerMessage == null }.bannerMessage)
    }
}
