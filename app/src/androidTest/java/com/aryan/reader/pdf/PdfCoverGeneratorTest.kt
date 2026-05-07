// app/src/androidTest/java/com/aryan/reader/pdf/PdfCoverGeneratorTest.kt
package com.aryan.reader.pdf

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PdfCoverGeneratorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var coverGenerator: PdfCoverGenerator
    private var samplePdfUri: Uri? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        coverGenerator = PdfCoverGenerator(context)
        try {
            samplePdfUri = copyAssetToCache(context, "sample.pdf")
        } catch (_: IOException) {
            println("Could not copy sample.pdf from assets. Skipping PdfCoverGenerator tests.")
            samplePdfUri = null
        }
    }

    @After
    fun tearDown() {
        val cacheFile = File(context.cacheDir, "sample.pdf")
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    @Test
    fun generateCover_returnsBitmapForValidPdf() = runTest {
        val uri = samplePdfUri ?: return@runTest

        val targetHeight = 600
        val cover = coverGenerator.generateCover(uri, targetHeight)

        assertThat(cover).isNotNull()
        assertThat(cover!!.height).isEqualTo(targetHeight)
        assertThat(cover.width).isGreaterThan(0)
    }

    @Test
    fun generateCover_returnsNullForInvalidUri() = runTest {
        val invalidUri = Uri.fromFile(File("nonexistent/file.pdf"))
        val cover = coverGenerator.generateCover(invalidUri)
        assertThat(cover).isNull()
    }

    private fun copyAssetToCache(context: Context, @Suppress("SameParameterValue") assetName: String): Uri {
        val file = File(context.cacheDir, assetName)
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
}