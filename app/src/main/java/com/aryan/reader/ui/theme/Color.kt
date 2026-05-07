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

import androidx.compose.ui.graphics.Color

// === 和风 / Galgame 配色方案 ===
// 主色：朱红 (#C5283D)  —— 鸟居红、印章
// 辅色：藏青 (#1B3358)  —— 墨色、深夜
// 背景：米白 (#FAF6E9)  —— 和纸
// 点缀：樱花粉 (#F4C2C2) / 抹茶绿 (#A8B89B)

// ---------- Light（浅色 / 日间和纸）----------
val primaryLight = Color(0xFFC5283D)              // 朱红
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFFFD9DD)     // 樱花淡粉
val onPrimaryContainerLight = Color(0xFF410008)

val secondaryLight = Color(0xFF1B3358)            // 藏青
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFD6E2F8)
val onSecondaryContainerLight = Color(0xFF001D36)

val tertiaryLight = Color(0xFFA8784E)             // 朽叶色（茶褐）
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFFFDCBE)
val onTertiaryContainerLight = Color(0xFF2C1600)

val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)

val backgroundLight = Color(0xFFFAF6E9)           // 和纸米白
val onBackgroundLight = Color(0xFF1F1B16)
val surfaceLight = Color(0xFFFAF6E9)
val onSurfaceLight = Color(0xFF1F1B16)
val surfaceVariantLight = Color(0xFFEEE6D5)
val onSurfaceVariantLight = Color(0xFF4F4639)

val outlineLight = Color(0xFF817567)
val outlineVariantLight = Color(0xFFD3C4B4)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF35302A)
val inverseOnSurfaceLight = Color(0xFFF9EFE1)
val inversePrimaryLight = Color(0xFFFFB3B6)

val surfaceDimLight = Color(0xFFE2DAC8)
val surfaceBrightLight = Color(0xFFFAF6E9)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF5EFDF)
val surfaceContainerLight = Color(0xFFF0E9D7)
val surfaceContainerHighLight = Color(0xFFEAE2CF)
val surfaceContainerHighestLight = Color(0xFFE4DCC8)

// ---------- Dark（深色 / 夜墨）----------
val primaryDark = Color(0xFFFFB3B6)               // 浅朱（夜间柔和）
val onPrimaryDark = Color(0xFF680014)
val primaryContainerDark = Color(0xFF8B0024)
val onPrimaryContainerDark = Color(0xFFFFD9DD)

val secondaryDark = Color(0xFFA9C7FF)             // 浅藏青
val onSecondaryDark = Color(0xFF002F65)
val secondaryContainerDark = Color(0xFF00468F)
val onSecondaryContainerDark = Color(0xFFD6E2F8)

val tertiaryDark = Color(0xFFFFB87C)
val onTertiaryDark = Color(0xFF4A2800)
val tertiaryContainerDark = Color(0xFF693D00)
val onTertiaryContainerDark = Color(0xFFFFDCBE)

val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

val backgroundDark = Color(0xFF17130E)            // 浓墨夜
val onBackgroundDark = Color(0xFFEDE0CC)
val surfaceDark = Color(0xFF17130E)
val onSurfaceDark = Color(0xFFEDE0CC)
val surfaceVariantDark = Color(0xFF4F4639)
val onSurfaceVariantDark = Color(0xFFD3C4B4)

val outlineDark = Color(0xFF9C8E7F)
val outlineVariantDark = Color(0xFF4F4639)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFEDE0CC)
val inverseOnSurfaceDark = Color(0xFF35302A)
val inversePrimaryDark = Color(0xFFC5283D)

val surfaceDimDark = Color(0xFF17130E)
val surfaceBrightDark = Color(0xFF3E382F)
val surfaceContainerLowestDark = Color(0xFF110D08)
val surfaceContainerLowDark = Color(0xFF1F1B16)
val surfaceContainerDark = Color(0xFF231F1A)
val surfaceContainerHighDark = Color(0xFF2E2924)
val surfaceContainerHighestDark = Color(0xFF39342E)