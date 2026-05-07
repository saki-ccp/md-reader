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
package com.aryan.reader

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.aryan.reader.paginatedreader.SvgStringFetcher
import timber.log.Timber

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        // 首次启动时把默认 locale 设为简体中文。
        // 用户后续可以在系统"应用语言"或应用内设置中切换 —— 我们只在第一次设默认。
        applyDefaultLocaleOnFirstLaunch()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun applyDefaultLocaleOnFirstLaunch() {
        val prefs = getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DEFAULT_LOCALE_APPLIED, false)) return

        // 仅当当前应用还没有显式设置过语言时才覆盖（避免覆盖用户后续修改）。
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("zh-CN")
            )
        }
        prefs.edit().putBoolean(KEY_DEFAULT_LOCALE_APPLIED, true).apply()
    }

    override fun newImageLoader(): ImageLoader {
        Timber.d("MyApplication: Creating custom ImageLoader with SvgStringFetcher.")
        return ImageLoader.Builder(this)
            .components {
                add(SvgStringFetcher.Factory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

    private companion object {
        const val LOCALE_PREFS = "locale_prefs"
        const val KEY_DEFAULT_LOCALE_APPLIED = "default_locale_applied_v1"
    }
}
