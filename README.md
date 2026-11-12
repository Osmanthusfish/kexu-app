# 课序

课序是一款基于 Kotlin 和 Jetpack Compose 的 Android 课表应用，支持多学期课表、单次调课与补课、提醒与作业、主题外观、数据备份，以及今日和整周桌面小组件。

[English](#english) · [功能](#功能) · [下载](#下载) · [参与测试](#一起把课序做得更好) · [源码构建](#构建和测试) · [许可](#许可)

## 功能

- **课程管理**：按学期管理课程，查看当天和整周安排。
- **灵活调整**：处理单次调课、停课与补课，保留原有周期课程。
- **桌面小组件**：在桌面查看今天或整周课程，快捷打开相关页面。
- **提醒与待办**：管理课程提醒、作业、待办和倒数日。
- **截图转待办**：在“待办 / 备忘”中选择截图，在设备上识别后编辑草稿并确认日期和时间；也可自行配置 DeepSeek 密钥，按次确认后整理识别文字。
- **外观与数据**：调整主题和小组件外观，导入或导出备份。

## 下载

支持 Android 8.0（API 26）及以上。[点击下载课序 APK](https://github.com/Osmanthusfish/kexu-app/releases/download/v1.49/kexu-1.49.apk)；源码 ZIP 不能直接安装。

安装更新前建议导出并妥善保存数据。若提示签名不兼容，请先备份数据，避免直接卸载原应用。

## 一起把课序做得更好

课序还在不断打磨中。如果你愿意花一点时间试用，看看课表、提醒和桌面小组件在你的手机上表现如何，我们会很感谢。无论是发现了问题、觉得哪里用起来别扭，还是有新的想法，都欢迎在项目公开后通过 GitHub Issues 告诉我们。

反馈问题时，如果方便，可以说明手机型号、Android 版本、操作步骤和预期结果；截图或日志也很有帮助。**提交前请遮住姓名、课程表、教室、联系方式等个人信息**，不要在公开页面上传真实备份文件或密钥。

## 使用与测试提示

- 项目仍在完善，可能存在尚未发现的问题，不同手机系统和桌面的小组件、通知或后台提醒表现也可能不同。
- 测试前建议先导出并妥善保管已有数据；重要课程和提醒请同时留有其他记录，不要仅依赖本应用。
- 此软件按现状提供，不对所有设备上的兼容性和提醒的及时性作保证。感谢你的理解和反馈，这些建议会帮助我们继续改进。

## 构建和测试

- Android Studio、JDK 17 和 Android SDK（compileSdk 36）；最低支持 Android 8.0（API 26）。
- 首次构建需要联网下载 Gradle 和项目声明的依赖。
- 在项目根目录执行 `sh ./gradlew :app:assembleDebug :app:testDebugUnitTest`；Windows 使用 `gradlew.bat :app:assembleDebug :app:testDebugUnitTest`。
- 隔离测试版本可使用 `-PisolatedReview=true`，其包名为 `com.schedule.app.review`；正式应用包名为 `com.schedule.app`。
- 设备回归包含会修改测试应用数据的操作，仅可在专用模拟器或明确授权的测试设备上运行。

## 数据与权限

首次启动使用空数据。课表、提醒及外观等保存在设备上，用户可导入或导出备份。系统备份行为取决于 Android 和设备设置。应用按功能声明通知、精确闹钟、开机恢复、全屏提醒、悬浮窗和前台服务等权限；具体可用性取决于系统和用户设置。

截图转待办使用 Google ML Kit 在设备上识别图片；根据 Google 的说明，图片和识别结果不会由 ML Kit 发送给 Google，但组件可能联网收集设备、应用、安装标识及性能等诊断信息。应用声明网络权限以支持可选的在线 AI 整理；仅在用户确认后将识别文字发送至 DeepSeek，图片不会发送。API 密钥加密保存在设备上且不包含在应用备份中。

导入引导提供了一段可复制的提示词，用户也可以自行选择外部 AI 工具整理课表；应用不会替用户将课表发送给 AI。如果选择向第三方 AI 上传课表、截图或文件，该服务会收到你提交的内容，其处理方式取决于服务提供方。提交前请检查并移除不愿分享的姓名、学号、联系方式等信息，并留意课程时间和地点也可能涉及隐私。备份文件同样可能包含个人数据，请妥善保管和分享。

## 许可

本项目自有代码和文档按 [课序非商业使用许可证](LICENSE) 提供：允许非商业使用、修改和免费分享，禁止将本项目及包含本项目受版权保护内容的衍生作品用于商业目的。具体权利和条件以许可证全文为准。

第三方依赖及其他素材各自的权利和许可证仍然适用，详见 [第三方声明](THIRD_PARTY_NOTICES.md)和[组件清单](THIRD_PARTY_COMPONENTS.md)。由于拟限制商业使用，项目公开源码后也不属于开源促进会定义的开源软件。

---

<a id="english"></a>

# Kexu

Kexu is an Android timetable app built with Kotlin and Jetpack Compose. It supports multiple semesters, one-time class changes and make-up classes, reminders and assignments, themes, data backup, and home-screen widgets for today's schedule and the full week.

[中文](#课序) · [Features](#features) · [Download](#download) · [Testing](#help-improve-kexu) · [Build](#build-and-test) · [License](#license)

## Features

- **Timetables:** manage courses across semesters and view daily or weekly schedules.
- **One-time changes:** reschedule, cancel, or add a make-up class without changing the recurring course.
- **Widgets:** see today's or this week's courses on your home screen and open related pages quickly.
- **Reminders and tasks:** manage class reminders, assignments, tasks, and countdowns.
- **Screenshot to task:** choose an image in Tasks, review the on-device recognition result, and confirm the date and time. Optionally configure a DeepSeek key and approve each request to organize the recognized text.
- **Appearance and data:** customize themes and widgets, and import or export backups.

## Download

Requires Android 8.0 (API 26) or later. [Download the Kexu APK](https://github.com/Osmanthusfish/kexu-app/releases/download/v1.49/kexu-1.49.apk). The source ZIP is not an installable APK.

Export and safely store your data before installing an update. If signatures are incompatible, back up your data before considering any uninstall.

## Help improve Kexu

Kexu is still being refined. If you have time to try it, we'd be grateful to hear how the timetable, reminders, and widgets work on your phone. Once the project is public, you are welcome to report bugs, awkward interactions, or ideas through GitHub Issues.

When reporting a problem, please include your phone model, Android version, steps to reproduce, and expected result if possible. Screenshots and logs can help too. **Remove personal information such as names, timetables, classrooms, and contact details before posting.** Do not upload real backup files or keys to public pages.

## Usage and testing

- The project is still in development and may have undiscovered issues. Widgets, notifications, and background reminders can behave differently across devices and launchers.
- Export and safely store a backup before testing. Keep a separate record of important classes and reminders rather than relying solely on the app.
- The software is provided as is, without a guarantee of compatibility with every device or timely delivery of reminders. Thank you for testing and sharing your feedback.

## Build and test

- Android Studio, JDK 17, and the Android SDK (compileSdk 36) are required. The minimum supported version is Android 8.0 (API 26).
- The first build requires an internet connection to download Gradle and the project's declared dependencies.
- From the project root, run `sh ./gradlew :app:assembleDebug :app:testDebugUnitTest`. On Windows, use `gradlew.bat :app:assembleDebug :app:testDebugUnitTest`.
- Use `-PisolatedReview=true` for an isolated test build with package name `com.schedule.app.review`; the regular app uses `com.schedule.app`.
- Device regression tests modify test-app data. Run them only on a dedicated emulator or an explicitly authorized test device.

## Data and permissions

The app starts with empty data. Timetables, reminders, and appearance settings are stored on the device; users can import and export backups. System backup behavior depends on Android and device settings. Depending on the feature, the app declares permissions for notifications, exact alarms, boot recovery, full-screen reminders, overlays, and foreground services. Availability depends on the system and user settings.

Screenshot-to-task uses Google ML Kit to recognize images on the device. According to Google, ML Kit does not send the images or recognition results to Google, but the SDK may collect device and application information, installation identifiers, and performance diagnostics over the network. The app requests internet access for optional online AI processing; recognized text is sent to DeepSeek only after user confirmation, and images are not sent. The API key is encrypted on the device and excluded from app backups.

The import guide offers a prompt that you can copy. You may choose to use an external AI tool to organize a timetable; the app does not send your timetable to an AI service for you. If you upload a timetable, screenshot, or file to a third-party AI service, that service receives what you submit, and its handling of the data depends on the provider. Before submitting, remove information you do not wish to share, such as names, student IDs, and contact details. Class times and locations can also be sensitive. Backup files may contain personal data, so store and share them carefully.

## License

The project's own code and documentation are provided under the [Kexu Noncommercial Use License](LICENSE). It permits noncommercial use, modification, and free sharing, and prohibits commercial use of the project and derivative works containing its protected content. The full license text governs the rights and conditions.

Third-party dependencies and other materials remain subject to their own rights and licenses; see the [third-party notices](THIRD_PARTY_NOTICES.md) and [component inventory](THIRD_PARTY_COMPONENTS.md). Because commercial use is restricted, the publicly available source is not open-source software as defined by the Open Source Initiative.
