# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Toolchain

- JDK **21** (set via `jvmToolchain(21)`; `compileOptions`/`kotlinOptions` also pinned to 21)
- Android SDK: compileSdk **36**, minSdk **26**, targetSdk **35**
- NDK + CMake **3.22.1** — required: `app/src/main/cpp/CMakeLists.txt` builds `libmobi`, the PDFium JNI bridge, and the WOFF2 converter every build (the gradle config does not gate native build behind a flag)

There is only one product flavor — `oss` — and only `release` is configured to enable minify. Use the `ossDebug` / `ossRelease` variants.

## Common commands

```bash
# Debug APK (CI default)
./gradlew :app:assembleOssDebug

# Release APK — needs MYAPP_RELEASE_STORE_FILE / _PASSWORD / _KEY_ALIAS / _KEY_PASSWORD in local.properties
./gradlew :app:assembleOssRelease

# JVM unit tests for :app (oss debug variant)
./gradlew :app:testOssDebugUnitTest

# Run a single unit test class / method
./gradlew :app:testOssDebugUnitTest --tests "com.aryan.reader.SomeClass"
./gradlew :app:testOssDebugUnitTest --tests "com.aryan.reader.SomeClass.someMethod"

# Instrumentation tests (need a device/emulator)
./gradlew :app:connectedOssDebugAndroidTest

# Lint
./gradlew :app:lintOssDebug

# Wipe build outputs
./gradlew clean
```

APK output filename is rewritten in `app/build.gradle.kts` to `md-reader-v<versionName>-<buildType>.apk`. Path: `app/build/outputs/apk/oss/<buildType>/`.

Unit tests run with `unitTests.isReturnDefaultValues = true` and `slf4j-android` excluded from `testImplementation` (replaced by `slf4j-nop`); JVM flag `-Xss2m` is set for them. Don't add `slf4j-android` back to `testImplementation`.

## High-level architecture

Single Android app (`:app`) plus a thin Kotlin Multiplatform module (`:shared`). `settings.gradle.kts` only includes those two.

### `:shared`
KMP layout with `commonMain`, `androidMain`, `desktopMain`, and an intermediate `readerJvmMain` (parent of both android and desktop, depends on `commonMain`, brings Jsoup). The `jvm("desktop")` target is declared but **not consumed by anything** in this repo — it's a vestigial holdover from the upstream `desktopApp` that was removed. Keep changes building on the android target; touching `desktopMain` only matters if you're reviving desktop support.

`:app` depends on `:shared` via `implementation(project(":shared"))`. Models that need to cross to shared go through `SharedModelMappers.kt` in `:app`.

### `:app` runtime entry points
- `MyApplication.kt` — `Application` subclass; Timber init, first-launch Chinese locale, and image-loader setup.
- `MainActivity.kt` → `AppNavigation.kt` — single Compose-Navigation activity. Top-level destinations are `MainScreen` (which hosts `HomeScreen` / `LibraryScreen`), reader screens (`PdfViewerScreen`, the paginated reader, etc.), `FontsScreen`, and `AiSettingsScreen`. Purchase, feedback, and support screens have been removed.
- `MainViewModel.kt` + `LibraryStateProjector.kt` — central state; library list is projected from Room via Paging 3.

### Reader engines (one package per format family)
- **PDF** (`pdf/`): primary engine is **Pdfium** via `io.legere:pdfiumandroid` plus a custom JNI bridge in `cpp/pdfium_bridge.cpp` exposed by `NativePdfiumBridge.kt`. `PdfBox-Android` is also used for parts of the export pipeline. Annotations (pen / highlight / eraser / text boxes) are drawn by `PdfDrawer.kt` / `PdfTextBox.kt`, persisted via `pdf/data/`, and burned into a new PDF by `PdfExporter.kt`. Cover thumbnails from `PdfCoverGenerator.kt`.
- **EPUB / MOBI / AZW3 / FB2** (`epub/`): pure-Kotlin EPUB parser (`EpubParser.kt` + `EpubXMLFileParser.kt`); MOBI/AZW3 go through `cpp/libmobi` via `cpp/mobi_jni_bridge.c` (`MobiParser.kt`); FB2 via `Fb2Parser.kt`; ODT/FODT via `OdtParser.kt`. Embedded WOFF2 fonts are decoded by `cpp/Woff2Converter.cpp` so the reader can inline them as TTF.
- **DOCX**: `org.zwobble.mammoth` converts to HTML, rendered in the paginated reader.
- **Markdown/HTML**: Flexmark (with tables, strikethrough, tasklist, autolink extensions).
- **Comics (CBZ/CBR/CB7)**: `me.zhanghai.android.libarchive`.
- **TTS** (`tts/TtsService.kt`): foreground service backed by Media3 session — surfaces playback controls in the system notification / lock screen.

Source-only viewers (Java/Kotlin/Python/C/C++/etc., logs, JSON, XML, CSV/TSV) reuse the markdown/text rendering path; format detection in `FileTypeResolver.kt` and registration in `AndroidManifest.xml`.

### Library / data layer (`data/`)
- Room database (`AppDatabase.kt`) with DAOs in `LibraryDaos.kt` / `RecentFileDao.kt` / `CustomFontDao.kt`; entities in `LibraryEntities.kt` etc.
- `androidx.paging:paging-runtime-ktx` + `room-paging` for the library grid.
- Two `WorkManager` workers do background heavy lifting:
  - `FolderSyncWorker` — incremental scan of user-chosen folders (SAF tree URIs resolved by `AndroidFolderPathResolver.kt`).
  - `MetadataExtractionWorker` — generates covers + metadata via the per-format generators (`PdfCoverGenerator.kt`, EPUB cover extraction, etc.).
- `SmartCollectionEngine.kt` powers dynamic shelves.

### Document groups and legacy OPDS
Manual shelves are presented as document groups. A document can belong to multiple groups without duplicating its file, reading position, or annotations. Deleting a group only removes its membership. Ungrouped documents are determined by active manual groups, independently of tags, series, smart shelves, and imported directories.

The OPDS catalog screens and download view model have been removed. Repository/parser and stream-document support remain for reading existing library records; there is no catalog entry in the current UI.

### Build flags / variants
The `oss` flavor disables the hosted paid/cloud services with these `BuildConfig` fields. User-configured AI keys remain supported:
- `IS_PRO = false`, `IS_OFFLINE = false`
- `AI_WORKER_URL`, `VERIFIER_WORKER_URL`, `FEEDBACK_WORKER_URL`, `TTS_WORKER_URL` — all empty strings

Code that calls a worker URL must tolerate the empty string (treat as "feature disabled"). Don't introduce a new flavor unless you also add the missing buildConfig fields here — defaults are not provided in `defaultConfig` for the URLs.

`debugImplementation` pulls in the TensorFlow Lite stack — those deps must not be referenced from `main/` (only from `debug/`), or release builds will fail.

### Native build notes
- `cppFlags` is intentionally empty; per-target flags live inside `CMakeLists.txt`.
- `packaging.jniLibs` excludes `libbrotlicommon/dec/enc.so` — they ship inside another transitive lib but aren't needed; don't remove these excludes without checking APK size / duplicate-`.so` errors.
- `META-INF/AL2.0` and `META-INF/LGPL2.1` are excluded from packaging (license file collisions).

### i18n
`resourceConfigurations` in `app/build.gradle.kts` filters which `values-*/strings.xml` actually ship in the APK (currently `en, zh-rCN, ar, de, tr, fr, ru`). Adding a new language requires both the `values-xx/` directory **and** an entry in this list.

## Repo provenance

This is a fork of upstream [Aryan-Raj3112/episteme](https://github.com/Aryan-Raj3112/episteme), trimmed to the OSS flavor only. The following upstream pieces have been removed and should not be reintroduced piecemeal — go upstream if you need them: `desktopApp/`, `releaseOffline` build variant, the `pro` source set's AI / cloud-sync / OCR / Bubble-Zoom features, `fastlane/`, `spotless/`, CLA tooling.
