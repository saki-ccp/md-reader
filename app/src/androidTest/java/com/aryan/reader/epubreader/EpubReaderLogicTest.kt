// app/src/androidTest/java/com/aryan/reader/epubreader/EpubReaderLogicTest.kt
package com.aryan.reader.epubreader

import android.content.Context
import timber.log.Timber
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aryan.reader.SearchResult
import com.aryan.reader.epub.EpubBook
import com.aryan.reader.epub.EpubChapter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class EpubReaderLogicTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var mockEpubBook: EpubBook

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "test_epub").apply { mkdirs() }

        // Create dummy chapter files
        val chapter1File = File(testDir, "chapter1.html")
        chapter1File.writeText("<html><body><p>A simple Test case.</p></body></html>")

        val chapter2File = File(testDir, "chapter2.html")
        chapter2File.writeText("<html><body><p>Another test case here.</p><p>The word Test appears twice.</p></body></html>")

        mockEpubBook = EpubBook(
            fileName = "test.epub",
            title = "Test Book",
            author = "Tester",
            language = "en",
            coverImage = null,
            extractionBasePath = testDir.absolutePath,
            chapters = listOf(
                EpubChapter(
                    chapterId = "ch1",
                    absPath = chapter1File.absolutePath,
                    title = "Chapter 1",
                    htmlFilePath = "chapter1.html",
                    plainTextContent = "",
                    htmlContent = ""
                ),
                EpubChapter(
                    chapterId = "ch2",
                    absPath = chapter2File.absolutePath,
                    title = "Chapter 2",
                    htmlFilePath = "chapter2.html",
                    plainTextContent = "",
                    htmlContent = ""
                )
            )
        )
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    private suspend fun searchEpub(book: EpubBook, query: String): List<SearchResult> {
        val TAG = "EpubReaderLogicTest"
        Timber.d("Starting search for query: '$query'")
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            book.chapters.forEachIndexed { chapterIndex, chapter ->
                try {
                    val fullPath = "${book.extractionBasePath}/${chapter.htmlFilePath}"
                    Timber.d("Chapter ${chapterIndex + 1}: Checking path '$fullPath'")
                    val htmlFile = File(fullPath)
                    if (!htmlFile.exists()) {
                        Timber.e("File does not exist: $fullPath")
                        return@forEachIndexed
                    }

                    val doc = Jsoup.parse(htmlFile, "UTF-8")
                    val bodyChildren = doc.body().children().toList()
                    val chunks = bodyChildren.chunked(20)

                    chunks.forEachIndexed { chunkIndex, chunkOfElements ->
                        val chunkHtml = chunkOfElements.joinToString(separator = "\n") { it.outerHtml() }
                        val content = Jsoup.parse(chunkHtml).text()
                        var lastIndex = -1

                        while (true) {
                            lastIndex = content.indexOf(query, startIndex = lastIndex + 1, ignoreCase = true)
                            if (lastIndex == -1) break

                            Timber.d("Found potential match for '$query' at index $lastIndex.")
                            val isWordStart = lastIndex == 0 || !content[lastIndex - 1].isLetterOrDigit()
                            Timber.d("Is it a word start? -> $isWordStart")
                            if (isWordStart) {
                                Timber.d("Match is a word start. Adding to results.")
                                val snippetStart = max(0, lastIndex - 35)
                                val snippetEnd = min(content.length, lastIndex + query.length + 35)
                                val rawSnippet = content.substring(snippetStart, snippetEnd)
                                val annotatedSnippet = buildAnnotatedString {
                                    append(rawSnippet)
                                    val highlightStart = content.indexOf(query, lastIndex, ignoreCase = true) - snippetStart
                                    val highlightEnd = highlightStart + query.length
                                    addStyle(
                                        style = SpanStyle(fontWeight = FontWeight.Bold),
                                        start = highlightStart,
                                        end = highlightEnd
                                    )
                                }
                                results.add(
                                    SearchResult(
                                        locationInSource = chapterIndex,
                                        locationTitle = chapter.title,
                                        snippet = annotatedSnippet,
                                        query = query,
                                        occurrenceIndexInLocation = results.count { it.locationInSource == chapterIndex },
                                        chunkIndex = chunkIndex
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e("Error during search in chapter ${chapter.title}", e)
                    throw e
                }
            }
            Timber.d("Search finished. Total results found: ${results.size}")
            results
        }
    }

    @Test
    fun search_findsCorrectResults() = runBlocking {
        val results = searchEpub(mockEpubBook, "case")
        assertThat(results).hasSize(2)
        assertThat(results.count { it.locationTitle == "Chapter 1" }).isEqualTo(1)
        assertThat(results.count { it.locationTitle == "Chapter 2" }).isEqualTo(1)
    }

    @Test
    fun search_isCaseInsensitive() = runBlocking {
        val results = searchEpub(mockEpubBook, "test")
        assertThat(results).hasSize(3)
        assertThat(results[0].locationTitle).isEqualTo("Chapter 1")
        assertThat(results[1].locationTitle).isEqualTo("Chapter 2")
        assertThat(results[2].locationTitle).isEqualTo("Chapter 2")
    }

    @Test
    fun search_noResultsFound() = runBlocking {
        val results = searchEpub(mockEpubBook, "nonexistent")
        assertThat(results).isEmpty()
    }

    @Test
    fun search_createsCorrectSnippetHighlight() = runBlocking {
        val query = "Test"
        mockEpubBook.chapters.first()
        val content = "A simple Test case."
        val annotatedString = buildAnnotatedStringWithHighlight(content, query)

        val spanStyles = annotatedString.spanStyles
        assertThat(spanStyles).hasSize(1)

        val style = spanStyles.first().item
        assertThat(style.fontWeight).isEqualTo(FontWeight.Bold)

        val start = spanStyles.first().start
        val end = spanStyles.first().end
        assertThat(annotatedString.substring(start, end)).isEqualTo(query)
    }

    @Suppress("SameParameterValue")
    private fun buildAnnotatedStringWithHighlight(content: String, query: String): AnnotatedString {
        return buildAnnotatedString {
            append(content)
            val highlightStart = content.indexOf(query, ignoreCase = true)
            if (highlightStart != -1) {
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = highlightStart,
                    end = highlightStart + query.length
                )
            }
        }
    }
}