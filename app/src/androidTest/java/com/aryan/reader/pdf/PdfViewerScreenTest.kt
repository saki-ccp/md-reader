package com.aryan.reader.pdf

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.aryan.reader.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PdfViewerScreenTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @org.junit.Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("epub_reader_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun createPdfViewIntent(context: Context, uri: Uri): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private var currentPdfFile: File? = null

    private val samplePdfUri: Uri by lazy { copyAssetToCache(context, "sample.pdf") }

    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(createPdfViewIntent(context, samplePdfUri))

    @After
    fun tearDown() {
        currentPdfFile?.let {
            if (it.exists()) it.delete()
        }
    }

    private fun waitForDocumentLoad(pageText: String = "Page 1 of 4") {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule
                .onAllNodesWithText(pageText)
                .fetchSemanticsNodes().size == 1
        }
    }

    private fun ensurePaginationMode() {
        composeTestRule.onNodeWithContentDescription("More Options").performClick()
        composeTestRule.onNodeWithText("Reading Mode: Paginated").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun documentLoadsAndDisplaysCorrectPageCount() {
        waitForDocumentLoad()
        composeTestRule.onNodeWithTag("PageNumberIndicator")
            .assertIsDisplayed()
    }

    @Test
    fun tableOfContents_displaysEmptyState() {
        waitForDocumentLoad()

        composeTestRule.onNodeWithTag("TocButton").performClick()

        composeTestRule.onNodeWithText("Chapters are not available for this book.").assertIsDisplayed()
    }

    @Suppress("SameParameterValue")
    private fun copyAssetToCache(context: Context, assetName: String): Uri {
        val uniqueName = "${UUID.randomUUID()}_$assetName"
        val file = File(context.cacheDir, uniqueName)

        currentPdfFile = file

        if (file.exists()) file.delete()
        context.assets.open(assetName).use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    @Test
    fun bookmarkFunctionality_addNavigateAndDelete() {
        waitForDocumentLoad()

        ensurePaginationMode()

        composeTestRule.onNodeWithText("Page 1 of 4").assertIsDisplayed()

        try {
            composeTestRule.onRoot().performTouchInput { swipe(start = this.centerRight, end = this.centerLeft, durationMillis = 300) }
            composeTestRule.onRoot().performClick()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithText("Page 2 of 4").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Page 2 of 4").assertIsDisplayed()
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithContentDescription("More Options").performClick()
            composeTestRule.onNodeWithText("Bookmark this page").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onRoot().performTouchInput { swipe(start = this.centerRight, end = this.centerLeft, durationMillis = 300) }
            composeTestRule.onRoot().performClick()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithText("Page 3 of 4").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Page 3 of 4").assertIsDisplayed()
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithTag("TocButton").performClick()
            composeTestRule.onNodeWithTag("BookmarksTab").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("BookmarkItem_1").assertIsDisplayed()
                .assert(hasText("Page 2", substring = true))
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithTag("BookmarkItem_1").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodes(hasTestTag("PageNumberIndicator").and(hasText("Page 2 of 4"))).fetchSemanticsNodes().size == 1
            }
            composeTestRule.onNode(hasTestTag("PageNumberIndicator").and(hasText("Page 2 of 4"))).assertIsDisplayed()

        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithTag("TocButton").performClick()
            composeTestRule.onNodeWithTag("BookmarksTab").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithContentDescription("More options for bookmark").performClick()
            composeTestRule.onNodeWithText("Delete").performClick()
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithText("Delete", useUnmergedTree = true).performClick()
        } catch (e: Exception) {
            throw e
        }

        try {
            composeTestRule.onNodeWithTag("BookmarkItem_1").assertDoesNotExist()
            composeTestRule.onNodeWithText("You haven't added any bookmarks yet.").assertIsDisplayed()
        } catch (e: Exception) {
            throw e
        }
    }

    @Test
    fun sliderNavigation_opensAndDisplaysCorrectly() {
        waitForDocumentLoad()

        composeTestRule.onNodeWithContentDescription("Navigate with slider").performClick()
        composeTestRule.onNodeWithContentDescription("Exit slider navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 4").assertIsDisplayed()
    }

    @Test
    fun displayMode_switchesToVerticalScroll() {
        waitForDocumentLoad()

        // Ensure we are in Pagination mode first to test the switch
        ensurePaginationMode()

        // Verify Vertical Scroll component is NOT displayed initially
        composeTestRule.onNodeWithTag("PdfVerticalScroll").assertDoesNotExist()

        // Switch to Vertical Scroll
        composeTestRule.onNodeWithContentDescription("More Options").performClick()
        composeTestRule.onNodeWithText("Reading Mode: Vertical scroll").performClick()

        composeTestRule.waitForIdle()

        // Verify Vertical Scroll component IS displayed
        composeTestRule.onNodeWithTag("PdfVerticalScroll").assertIsDisplayed()

        // Switch back to Pagination
        ensurePaginationMode()

        // Verify Vertical Scroll component is gone
        composeTestRule.onNodeWithTag("PdfVerticalScroll").assertDoesNotExist()
    }

    @Test
    fun search_uiOpensAndAcceptsQuery() {
        waitForDocumentLoad()

        // Click search button
        composeTestRule.onNodeWithTag("SearchButton").performClick()

        composeTestRule.onNodeWithText("English, Spanish, French, etc.").performClick()

        // Verify text field appears
        composeTestRule.onNodeWithTag("SearchTextField").assertIsDisplayed()

        // Enter text
        composeTestRule.onNodeWithTag("SearchTextField").performTextInput("test query")

        // Verify text exists in the field
        composeTestRule.onNodeWithTag("SearchTextField").assertTextContains("test query")

        // Close search
        composeTestRule.onNodeWithContentDescription("Close Search").performClick()

        // Verify text field is gone
        composeTestRule.onNodeWithTag("SearchTextField").assertDoesNotExist()
    }

    @Test
    fun fullScreen_togglesVisibility() {
        waitForDocumentLoad()

        // Click enter full screen button
        composeTestRule.onNodeWithContentDescription("Enter Full Screen").performClick()

        // Verify exit full screen button appears
        composeTestRule.onNodeWithContentDescription("Exit Full Screen").assertIsDisplayed()

        // Click exit full screen
        composeTestRule.onNodeWithContentDescription("Exit Full Screen").performClick()

        // Verify exit button is gone and enter button returns
        composeTestRule.onNodeWithContentDescription("Exit Full Screen").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Enter Full Screen").assertIsDisplayed()
    }

    @Test
    fun darkMode_togglesState() {
        waitForDocumentLoad()

        // Initial state: Light mode (default from cleared prefs), so button says "Enable Dark Mode"
        composeTestRule.onNodeWithContentDescription("Enable Dark Mode").assertIsDisplayed()

        // Toggle On
        composeTestRule.onNodeWithContentDescription("Enable Dark Mode").performClick()

        // State changed: Now button says "Disable Dark Mode"
        composeTestRule.onNodeWithContentDescription("Disable Dark Mode").assertIsDisplayed()

        // Toggle Off
        composeTestRule.onNodeWithContentDescription("Disable Dark Mode").performClick()

        // State changed back
        composeTestRule.onNodeWithContentDescription("Enable Dark Mode").assertIsDisplayed()
    }
}