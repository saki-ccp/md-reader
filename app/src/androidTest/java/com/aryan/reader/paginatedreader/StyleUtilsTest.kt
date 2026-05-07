// StyleUtilsTest.kt
package com.aryan.reader.paginatedreader

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StyleUtilsTest {

    private val baseFontSizeSp = 16f
    private val density = 2.0f
    private val containerWidthPx = 1000

    @Test
    fun parseCssSizeToDp_handlesPxValues() {
        assertThat(parseCssSizeToDp("100px", baseFontSizeSp, density, containerWidthPx)).isEqualTo(50.dp)
    }

    @Test
    fun parseCssSizeToDp_handlesEmValues() {
        assertThat(parseCssSizeToDp("1.5em", baseFontSizeSp, density, containerWidthPx)).isEqualTo(24.dp)
    }

    @Test
    fun parseCssSizeToDp_handlesRemValues() {
        assertThat(parseCssSizeToDp("2rem", baseFontSizeSp, density, containerWidthPx)).isEqualTo(32.dp)
    }

    @Test
    fun parseCssSizeToDp_handlesPtValues() {
        assertThat(parseCssSizeToDp("12pt", baseFontSizeSp, density, containerWidthPx).value).isWithin(0.01f).of(8.0f)
    }

    @Test
    fun parseCssSizeToDp_handlesPercentageValues() {
        // 50% of 1000px = 500px. 500px / 2.0 density = 250dp
        assertThat(parseCssSizeToDp("50%", baseFontSizeSp, density, containerWidthPx)).isEqualTo(250.dp)
    }

    @Test
    fun parseCssSizeToDp_returns0ForInvalidInput() {
        assertThat(parseCssSizeToDp("invalid", baseFontSizeSp, density, containerWidthPx)).isEqualTo(0.dp)
    }

    @Test
    fun parseCssSizeToDp_handlesZeroDensity() {
        assertThat(parseCssSizeToDp("100px", baseFontSizeSp, 0f, containerWidthPx)).isEqualTo(0.dp)
    }

    @Test
    fun parseCssSizeToDp_handlesZeroContainerWidthForPercentage() {
        assertThat(parseCssSizeToDp("50%", baseFontSizeSp, density, 0)).isEqualTo(0.dp)
    }

    @Test
    fun parseCssSizeToDp_handlesValuesWithWhitespace() {
        assertThat(parseCssSizeToDp("  1.5em  ", baseFontSizeSp, density, containerWidthPx)).isEqualTo(24.dp)
    }


    @Test
    fun parseCssDimensionToTextUnit_handlesPxValues() {
        val result = parseCssDimensionToTextUnit("100px", containerWidthPx, density)
        assertThat(result.isSp).isTrue()
        assertThat(result.value).isWithin(0.01f).of(50f)
    }

    @Test
    fun parseCssDimensionToTextUnit_handlesEmValues() {
        val result = parseCssDimensionToTextUnit("1.5em", containerWidthPx, density)
        assertThat(result.isEm).isTrue()
        assertThat(result.value).isEqualTo(1.5f)
    }

    @Test
    fun parseCssDimensionToTextUnit_handlesRemValues() {
        // rem is treated as em
        val result = parseCssDimensionToTextUnit("2rem", containerWidthPx, density)
        assertThat(result.isEm).isTrue()
        assertThat(result.value).isEqualTo(2f)
    }

    @Test
    fun parseCssDimensionToTextUnit_handlesPtValues() {
        val result = parseCssDimensionToTextUnit("12pt", containerWidthPx, density)
        assertThat(result.isSp).isTrue()
        assertThat(result.value).isWithin(0.01f).of(8.0f)
    }

    @Test
    fun parseCssDimensionToTextUnit_handlesPercentageValues() {
        val result = parseCssDimensionToTextUnit("50%", containerWidthPx, density)
        assertThat(result.isSp).isTrue()
        assertThat(result.value).isWithin(0.01f).of(250f)
    }

    @Test
    fun parseCssDimensionToTextUnit_returnsUnspecifiedForInvalidInput() {
        assertThat(parseCssDimensionToTextUnit("invalid", containerWidthPx, density).isUnspecified).isTrue()
    }

    @Test
    fun parseCssDimensionToTextUnit_handlesZeroDensity() {
        assertThat(parseCssDimensionToTextUnit("100px", containerWidthPx, 0f).isUnspecified).isTrue()
    }

    @Test
    fun parseCssDimensionToTextUnit_handlesZeroContainerWidthForPercentage() {
        assertThat(parseCssDimensionToTextUnit("50%", 0, density).isUnspecified).isTrue()
    }
}