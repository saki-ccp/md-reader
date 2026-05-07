// PaginatedReaderViewModelTest.kt
package com.aryan.reader.paginatedreader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aryan.reader.SearchResult
import com.aryan.reader.epub.EpubBook
import com.aryan.reader.epub.EpubChapter
import com.aryan.reader.paginatedreader.data.BookCacheDao
import com.aryan.reader.paginatedreader.data.BookCacheDatabase
import com.aryan.reader.paginatedreader.data.BookProcessingWorker
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class FakePaginator(
    initiallyLoading: Boolean,
    initialPageCount: Int,
    initialGeneration: Int
) : IPaginator {
    override var isLoading by mutableStateOf(initiallyLoading)
    override var totalPageCount by mutableIntStateOf(initialPageCount)
    override var generation by mutableIntStateOf(initialGeneration)
    override val pageShiftRequest: Flow<Int> = emptyFlow()

    var lastNavigatedHref: String? = null
    var lastNavigatedChapter: String? = null

    override fun getPageContent(pageIndex: Int): Page? = null
    override fun getChapterPathForPage(pageIndex: Int): String? = null
    override fun getPlainTextForChapter(chapterIndex: Int): String? = null

    override fun navigateToHref(
        currentChapterAbsPath: String,
        href: String,
        onNavigationComplete: (pageIndex: Int) -> Unit
    ) {
        lastNavigatedChapter = currentChapterAbsPath
        lastNavigatedHref = href
    }

    override fun findPageForSearchResult(
        result: SearchResult,
        onResult: (Int) -> Unit
    ) = Unit

    // Add stubs for the other missing interface members
    override fun findPageForCfi(chapterIndex: Int, cfi: String, onResult: (Int) -> Unit) = Unit
    override fun findPageForCfiAndOffset(
        chapterIndex: Int,
        cfi: String,
        charOffset: Int
    ): Int? {
        return null
    }

    override fun findChapterIndexForPage(pageIndex: Int): Int? = null
    override fun getCfiForPage(pageIndex: Int): String? = null
    override fun onUserScrolledTo(pageIndex: Int) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PaginatedReaderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: PaginatedReaderViewModel
    private lateinit var fakePaginator: FakePaginator

    @Before
    fun setUp() {
        viewModel = PaginatedReaderViewModel()
        fakePaginator = FakePaginator(
            initiallyLoading = true,
            initialPageCount = 0,
            initialGeneration = 0
        )
        viewModel.setPaginatorForTest(fakePaginator)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun uiState_reflectsPaginatorInitialState() = runTest {
        val initialState = viewModel.uiState.value
        assertThat(initialState.isLoading).isTrue()
        assertThat(initialState.totalPageCount).isEqualTo(0)
        assertThat(initialState.generation).isEqualTo(0)
    }

    @Test
    fun uiState_updatesWhenPaginatorIsLoadingChanges() = runTest {
        assertThat(viewModel.uiState.value.isLoading).isTrue()

        fakePaginator.isLoading = false
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun uiState_updatesWhenPaginatorTotalPageCountChanges() = runTest {
        assertThat(viewModel.uiState.value.totalPageCount).isEqualTo(0)

        fakePaginator.totalPageCount = 123
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.totalPageCount).isEqualTo(123)
    }

    @Test
    fun uiState_updatesWhenPaginatorGenerationChanges() = runTest {
        assertThat(viewModel.uiState.value.generation).isEqualTo(0)

        fakePaginator.generation = 5
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.generation).isEqualTo(5)
    }

    @Test
    fun onLinkClick_callsPaginatorNavigateToHrefWithCorrectArguments() {
        val currentChapter = "chapter1.xhtml"
        val href = "#section2"

        viewModel.onLinkClick(currentChapter, href) {}

        assertThat(fakePaginator.lastNavigatedChapter).isEqualTo(currentChapter)
        assertThat(fakePaginator.lastNavigatedHref).isEqualTo(href)
    }
    @Test
    fun initialize_createsARealPaginatorAndUpdateState() = runTest {
        // Arrange
        val viewModel = PaginatedReaderViewModel() // Create a fresh ViewModel
        val context = ApplicationProvider.getApplicationContext<Context>()
        val textMeasurer = mockk<TextMeasurer>(relaxed = true)
        val constraints = Constraints(maxWidth = 1080, maxHeight = 1920)
        val textStyle = TextStyle.Default
        val density = Density(1f)
        val mathMLRenderer = mockk<MathMLRenderer>(relaxed = true)
        val testBook = EpubBook(
            fileName = "test.epub",
            title = "Test Book",
            author = "Test Author",
            language = "en",
            coverImage = null,
            chapters = listOf(
                EpubChapter(
                    chapterId = "ch1",
                    title = "Chapter 1",
                    htmlFilePath = "ch1.html",
                    absPath = "/ops/ch1.html",
                    htmlContent = "<p>Some content</p>",
                    plainTextContent = "Some content"
                )
            ),
            css = mapOf("/ops/style.css" to "p {color: red;}"),
            extractionBasePath = ""
        )

        // Mock dependencies for BookPaginator
        val mockDao = mockk<BookCacheDao>(relaxed = true)
        coEvery { mockDao.getProcessedBook(any()) } returns null // Simulate cache miss

        val mockDb = mockk<BookCacheDatabase>()
        every { mockDb.bookCacheDao() } returns mockDao

        mockkObject(BookCacheDatabase.Companion)
        every { BookCacheDatabase.getDatabase(any()) } returns mockDb

        mockkObject(BookProcessingWorker.Companion)
        every { BookProcessingWorker.enqueue(any(), any(), any(), any(), any(), any()) } returns Unit

        // Pre-condition check
        assertThat(viewModel.uiState.value.isLoading).isTrue()
        assertThat(viewModel.paginator).isNull()

        // Act
        viewModel.initialize(
            book = testBook,
            textMeasurer = textMeasurer,
            textConstraints = constraints,
            textStyle = textStyle,
            density = density,
            isDarkTheme = false,
            context = context,
            initialChapterToPaginate = 0,
            mathMLRenderer = mathMLRenderer
        )
        advanceUntilIdle() // Allow coroutines to complete

        // Assert
        assertThat(viewModel.paginator).isInstanceOf(BookPaginator::class.java)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.totalPageCount).isGreaterThan(0)
    }

    @Test
    fun initialize_isIdempotent() = runTest {
        // Arrange
        val viewModel = PaginatedReaderViewModel()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val textMeasurer = mockk<TextMeasurer>(relaxed = true)
        val constraints = Constraints(maxWidth = 1080, maxHeight = 1920)
        val textStyle = TextStyle.Default
        val density = Density(1f)
        val mathMLRenderer = mockk<MathMLRenderer>(relaxed = true)
        val testBook = EpubBook(
            fileName = "test.epub",
            title = "Test Book",
            author = "Test Author",
            language = "en",
            coverImage = null,
            chapters = listOf(
                EpubChapter(
                    chapterId = "ch1",
                    title = "Chapter 1",
                    htmlFilePath = "ch1.html",
                    absPath = "/ops/ch1.html",
                    htmlContent = "<p>Some content</p>",
                    plainTextContent = "Some content"
                )
            ),
            css = mapOf("/ops/style.css" to "p {color: red;}"),
            extractionBasePath = ""
        )

        // Mock dependencies
        val mockDao = mockk<BookCacheDao>(relaxed = true)
        coEvery { mockDao.getProcessedBook(any()) } returns null
        val mockDb = mockk<BookCacheDatabase>()
        every { mockDb.bookCacheDao() } returns mockDao
        mockkObject(BookCacheDatabase.Companion)
        every { BookCacheDatabase.getDatabase(any()) } returns mockDb
        mockkObject(BookProcessingWorker.Companion)
        every { BookProcessingWorker.enqueue(any(), any(), any(), any(), any(), any()) } returns Unit

        // Act
        viewModel.initialize(
            book = testBook,
            textMeasurer = textMeasurer,
            textConstraints = constraints,
            textStyle = textStyle,
            density = density,
            isDarkTheme = false,
            context = context,
            initialChapterToPaginate = 0,
            mathMLRenderer = mathMLRenderer
        )
        advanceUntilIdle()

        val firstPaginator = viewModel.paginator
        assertThat(firstPaginator).isNotNull()

        // Act again
        viewModel.initialize(
            book = testBook,
            textMeasurer = textMeasurer,
            textConstraints = constraints,
            textStyle = textStyle,
            density = density,
            isDarkTheme = false,
            context = context,
            initialChapterToPaginate = 0,
            mathMLRenderer = mathMLRenderer
        )
        advanceUntilIdle()

        // Assert
        val secondPaginator = viewModel.paginator
        assertThat(secondPaginator).isSameInstanceAs(firstPaginator)
    }
}