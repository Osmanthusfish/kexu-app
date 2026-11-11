# 第三方声明

课序项目自有代码和文档适用根目录的 LICENSE。第三方组件与素材各自适用其原有许可，本项目的非商业限制不替代或改变第三方自身的许可。

- Gradle Wrapper 8.11.1：Unix 构建脚本 `gradlew` 由 Gradle 8.11.1 生成并保留其原有版权头，遵循 Apache License 2.0。本仓库分发的 `gradle/wrapper/gradle-wrapper.jar` 含有 Apache License 2.0，原文见 [Apache-2.0.txt](third_party_licenses/Apache-2.0.txt)。其 SHA-256 已与 [Gradle 官方 8.11.1 Wrapper 校验值](https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256)比对一致。Gradle 本身及其第三方组成部分另受其各自许可约束。
- AndroidX、Jetpack Compose、Kotlin 标准库及此前核对的运行时依赖：当时核对的 releaseRuntimeClasspath 制品 POM 或父级 POM 声明 Apache 2.0；见 [逐项组件清单](THIRD_PARTY_COMPONENTS.md) 和 [Apache-2.0.txt](third_party_licenses/Apache-2.0.txt)。这些项目归其各自权利人所有。截图识别开发分支新增 Google ML Kit 及其传递依赖，尚需单独核对许可证与分发声明，不能沿用上述结论。
- 截图识别使用 `com.google.mlkit:text-recognition-chinese:16.0.1`，并引入 Google Play services、ML Kit 和 Firebase 等传递组件。其使用条件与数据处理说明见 [ML Kit 服务条款与隐私说明](https://developers.google.com/ml-kit/terms) 和 [Android 数据披露说明](https://developers.google.com/ml-kit/android-data-disclosure)。完整依赖清单和分发所需的许可及声明仍需逐项核实，此条不是完整许可审计。
- 已检查的三个 ML Kit 中文识别相关 AAR 含 `third_party_licenses.json` 和 `third_party_licenses.txt`，其中包含多项第三方声明；当前 Debug APK 内未发现名称含 license 或 notice 的文件。分发前需核对适用组件并提供必要声明，不能认为本文件已涵盖它们。
- AndroidX Emoji2 1.3.0 的 AAR 包还内嵌重定位的 FlatBuffers 代码，位于 `libs/repackaged.jar`（运行包名为 `androidx.emoji2.text.flatbuffer`）。FlatBuffers 由 Google 及贡献者开发，遵循 Apache License 2.0；[上游许可](https://github.com/google/flatbuffers/blob/master/LICENSE)，[AndroidX 的重定位配置示例](https://android.googlesource.com/platform/frameworks/support/+/58618bec2592c538b0a9e469f1492365ef2233e3/emoji2/emoji2/build.gradle)。内嵌版本尚未确认，不将配置示例当作 1.3.0 的版本证明。
- Emoji2 内生成的 MetadataItem 和 MetadataList 对应 Android Open Source Project 的 Noto Emoji 兼容层；其 [Android 上游 13.0.0_r35 源文件](https://android.googlesource.com/platform/external/noto-fonts/+/refs/tags/android-13.0.0_r35/emoji-compat-flatbuffers/src/java/androidx/text/emoji/flatbuffer/MetadataItem.java)标注 Copyright (C) 2017 The Android Open Source Project，遵循 Apache 2.0。该历史标签是来源线索，尚未证明为构建 Emoji2 1.3.0 所用的确切修订。
- JUnit 4.13.2：仅用于 JVM 单元测试，EPL 1.0；[许可原文](third_party_licenses/JUnit-4.13.2-LICENSE.txt)及[上游 NOTICE](third_party_licenses/JUnit-4.13.2-NOTICE.txt)。
- Hamcrest Core 1.3：测试依赖，BSD 许可；[许可原文](third_party_licenses/Hamcrest-1.3-LICENSE.txt)。
- JSON-java 20240303：JVM 测试依赖，其上游指定版本标注 Public Domain；[上游原文](third_party_licenses/JSON-java-20240303-LICENSE.txt)。Android 运行时使用系统提供的 org.json。
- Gradle 插件、测试及构建工具的其他传递依赖见[组件清单](THIRD_PARTY_COMPONENTS.md)。各自许可及版权声明继续适用。

应用图标由 AI 辅助生成并由项目作者选定。公开候选移除了原图中的内容来源凭证元数据以减少可能关联的信息，原图由作者私下保存；这一处理不会改变图像像素。图标、第三方素材及原始代码的具体权利范围应由相关权利人确认。本清单记录已发现的来源，不代表对所有素材、软件包内嵌代码或分发形式完成法律审核。分发 APK 前应另核对需要随分发提供的第三方声明是否完整。

分发 APK 时，应随分发提供项目许可证及适用的第三方许可和声明；具体分发义务仍需逐项核对。
