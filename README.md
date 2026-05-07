<div align="center">

# md-reader

一个面向 Android 平台的本地文档与电子书阅读器，适用于 PDF、Markdown、EPUB 及常见文本类文件的浏览与阅读。

</div>

## 项目说明

本项目基于上游开源项目 [Aryan-Raj3112/episteme](https://github.com/Aryan-Raj3112/episteme) 进行二次开发与定向精简。

在保留上游项目核心阅读能力的基础上，本仓库围绕“本地阅读体验”进行了针对性调整，主要包括：

- 增加中文界面支持，并将默认语言设为简体中文

- 增加自定义背景功能，可自行选择图片并调整透明度

- 删除一部分不符合当前使用场景的冗余功能，保留更纯粹的本地阅读能力

- 对整体 UI 做了重新整理，视觉上更柔和，也更偏向轻量、二次元风格的阅读器体验

  感谢上游项目提供的基础能力与开源实现。

## 适合谁

本项目主要适用于以下场景：

- 喜欢在手机上阅读本地文档
- 经常查看 Markdown、PDF、EPUB、TXT 等文件
- 想要一个界面更顺眼、干扰更少的阅读器
- 希望把 App 作为系统默认打开器使用

## 当前特性

### 1. 中文优先

- 支持中文界面
- 默认语言为简体中文
- 更适合中文用户直接安装使用

### 2. 本地阅读体验

- 本地文件导入
- 最近阅读 / 书库管理
- 适合作为日常文档查看器与轻量阅读器使用

### 3. 自定义背景

- 可选择本地图片作为应用背景
- 可调节背景透明度
- 让阅读界面更符合个人喜好

### 4. 支持多种格式

目前可用于阅读或查看的格式包括：

- PDF
- EPUB / MOBI / AZW3 / FB2
- Markdown / TXT / HTML
- DOCX / ODT / FODT
- CBZ / CBR / CB7
- CSV / TSV
- JSON / XML
- 常见源码与日志文本

## 构建

### 环境要求

- JDK 21
- Android SDK
- 建议使用最新版 Android Studio 稳定版

### 调试构建

```bash
./gradlew :app:assembleOssDebug
```

构建完成后，APK 位于：

```text
app/build/outputs/apk/oss/debug/md-reader-v<version>-debug.apk
```

## 许可证

本项目基于上游开源项目进行二次开发，相关许可证遵循原项目约定。

具体请查看仓库中的 [LICENSE](LICENSE) 文件。
