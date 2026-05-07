/*
 * Episteme Reader - A native Android document reader.
 * Copyright (C) 2026 Episteme
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * mail: epistemereader@gmail.com
 */
package com.aryan.reader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 和风 / Galgame 风字体方案。
 *
 * 默认使用 [FontFamily.Serif] —— Android 系统会用 Noto Serif CJK 渲染中文字符，
 * 在大部分现代设备（Android 8+）上呈现出与思源宋体几乎一致的"和风宋体"观感，
 * 而无需把 ~10MB 的 TTF 打进 APK。
 *
 * 若要强制内嵌字体以保证所有设备完全一致：
 *   1. 下载 NotoSerifSC-Regular.ttf / NotoSerifSC-Bold.ttf 放入 res/font/
 *   2. 把下面 [serifFamily] 改为：
 *      FontFamily(Font(R.font.noto_serif_sc_regular, FontWeight.Normal),
 *                 Font(R.font.noto_serif_sc_bold,    FontWeight.Bold))
 */
private val serifFamily: FontFamily = FontFamily.Serif

private val baseTypography = Typography()

/** 把 [base] 中所有 TextStyle 的 fontFamily 替换为 [family]。 */
private fun Typography.withFamily(family: FontFamily): Typography = Typography(
    displayLarge      = displayLarge.copy(fontFamily = family),
    displayMedium     = displayMedium.copy(fontFamily = family),
    displaySmall      = displaySmall.copy(fontFamily = family),
    headlineLarge     = headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    headlineMedium    = headlineMedium.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    headlineSmall     = headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    titleLarge        = titleLarge.copy(fontFamily = family, fontWeight = FontWeight.SemiBold),
    titleMedium       = titleMedium.copy(fontFamily = family, fontWeight = FontWeight.Medium),
    titleSmall        = titleSmall.copy(fontFamily = family, fontWeight = FontWeight.Medium),
    bodyLarge         = bodyLarge.copy(fontFamily = family),
    bodyMedium        = bodyMedium.copy(fontFamily = family),
    bodySmall         = bodySmall.copy(fontFamily = family),
    labelLarge        = labelLarge.copy(fontFamily = family),
    labelMedium       = labelMedium.copy(fontFamily = family),
    labelSmall        = labelSmall.copy(fontFamily = family),
)

val AppTypography: Typography = baseTypography.withFamily(serifFamily)
