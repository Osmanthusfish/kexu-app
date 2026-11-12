# 第三方组件清单

> 本清单为新增截图识别依赖前的历史记录，不代表当前版本的完整运行时组件。当前发布候选实际解析的运行时制品见[新清单](third_party_licenses/RUNTIME-DEPENDENCIES.md)，其附带的第三方声明见[声明汇总](third_party_licenses/RUNTIME-THIRD-PARTY-NOTICES.txt)。

此清单来自离线构建实际解析的 Gradle 制品，包含发布版、调试版、单元测试与构建工具。许可证列转录 POM 或其父级声明；多项许可证的选择关系以原始文本为准。构建工具和单元测试依赖不因列在此处就被声称包含在 APK 中；构建工具的插件及其传递依赖也不随本源码候选一并分发。此表不是对每个组件所有源文件及 NOTICE 的完整法律审计。

范围：R = releaseRuntimeClasspath；D = debugRuntimeClasspath；T = debugUnitTestRuntimeClasspath；B = 构建插件 classpath。版本或构建配置变化时应重新生成。

| 组件 | 范围 | 元数据中声明的许可 | 原始依据 |
| --- | --- | --- | --- |
| `androidx.activity:activity:1.9.3` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.9.3/activity-1.9.3.pom) |
| `androidx.activity:activity-compose:1.9.3` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/1.9.3/activity-compose-1.9.3.pom) |
| `androidx.activity:activity-ktx:1.9.3` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/activity/activity-ktx/1.9.3/activity-ktx-1.9.3.pom) |
| `androidx.annotation:annotation-experimental:1.4.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-experimental/1.4.1/annotation-experimental-1.4.1.pom) |
| `androidx.annotation:annotation-jvm:1.8.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-jvm/1.8.1/annotation-jvm-1.8.1.pom) |
| `androidx.arch.core:core-common:2.2.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-common/2.2.0/core-common-2.2.0.pom) |
| `androidx.arch.core:core-runtime:2.2.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-runtime/2.2.0/core-runtime-2.2.0.pom) |
| `androidx.autofill:autofill:1.0.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/autofill/autofill/1.0.0/autofill-1.0.0.pom) |
| `androidx.collection:collection-jvm:1.4.4` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/collection/collection-jvm/1.4.4/collection-jvm-1.4.4.pom) |
| `androidx.collection:collection-ktx:1.4.4` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/collection/collection-ktx/1.4.4/collection-ktx-1.4.4.pom) |
| `androidx.compose.animation:animation-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-android/1.7.5/animation-android-1.7.5.pom) |
| `androidx.compose.animation:animation-core-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/animation/animation-core-android/1.7.5/animation-core-android-1.7.5.pom) |
| `androidx.compose.foundation:foundation-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-android/1.7.5/foundation-android-1.7.5.pom) |
| `androidx.compose.foundation:foundation-layout-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/foundation/foundation-layout-android/1.7.5/foundation-layout-android-1.7.5.pom) |
| `androidx.compose.material:material-android:1.7.5` | DT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-android/1.7.5/material-android-1.7.5.pom) |
| `androidx.compose.material:material-icons-core-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-core-android/1.7.5/material-icons-core-android-1.7.5.pom) |
| `androidx.compose.material:material-icons-extended-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-extended-android/1.7.5/material-icons-extended-android-1.7.5.pom) |
| `androidx.compose.material:material-ripple-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-ripple-android/1.7.5/material-ripple-android-1.7.5.pom) |
| `androidx.compose.material3:material3-android:1.3.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.3.1/material3-android-1.3.1.pom) |
| `androidx.compose.runtime:runtime-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-android/1.7.5/runtime-android-1.7.5.pom) |
| `androidx.compose.runtime:runtime-saveable-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/runtime/runtime-saveable-android/1.7.5/runtime-saveable-android-1.7.5.pom) |
| `androidx.compose.ui:ui-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-android/1.7.5/ui-android-1.7.5.pom) |
| `androidx.compose.ui:ui-geometry-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-geometry-android/1.7.5/ui-geometry-android-1.7.5.pom) |
| `androidx.compose.ui:ui-graphics-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-graphics-android/1.7.5/ui-graphics-android-1.7.5.pom) |
| `androidx.compose.ui:ui-text-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-text-android/1.7.5/ui-text-android-1.7.5.pom) |
| `androidx.compose.ui:ui-tooling-android:1.7.5` | DT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-tooling-android/1.7.5/ui-tooling-android-1.7.5.pom) |
| `androidx.compose.ui:ui-tooling-data-android:1.7.5` | DT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-tooling-data-android/1.7.5/ui-tooling-data-android-1.7.5.pom) |
| `androidx.compose.ui:ui-tooling-preview-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-tooling-preview-android/1.7.5/ui-tooling-preview-android-1.7.5.pom) |
| `androidx.compose.ui:ui-unit-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-unit-android/1.7.5/ui-unit-android-1.7.5.pom) |
| `androidx.compose.ui:ui-util-android:1.7.5` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/compose/ui/ui-util-android/1.7.5/ui-util-android-1.7.5.pom) |
| `androidx.concurrent:concurrent-futures:1.1.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/concurrent/concurrent-futures/1.1.0/concurrent-futures-1.1.0.pom) |
| `androidx.core:core:1.13.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/core/core/1.13.1/core-1.13.1.pom) |
| `androidx.core:core-ktx:1.13.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/1.13.1/core-ktx-1.13.1.pom) |
| `androidx.customview:customview-poolingcontainer:1.0.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/customview/customview-poolingcontainer/1.0.0/customview-poolingcontainer-1.0.0.pom) |
| `androidx.databinding:databinding-common:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/databinding/databinding-common/8.9.1/databinding-common-8.9.1.pom) |
| `androidx.databinding:databinding-compiler-common:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/databinding/databinding-compiler-common/8.9.1/databinding-compiler-common-8.9.1.pom) |
| `androidx.emoji2:emoji2:1.3.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/emoji2/emoji2/1.3.0/emoji2-1.3.0.pom) |
| `androidx.graphics:graphics-path:1.0.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/graphics/graphics-path/1.0.1/graphics-path-1.0.1.pom) |
| `androidx.interpolator:interpolator:1.0.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/interpolator/interpolator/1.0.0/interpolator-1.0.0.pom) |
| `androidx.lifecycle:lifecycle-common-java8:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common-java8/2.8.7/lifecycle-common-java8-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-common-jvm:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common-jvm/2.8.7/lifecycle-common-jvm-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-livedata-core:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata-core/2.8.7/lifecycle-livedata-core-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-process:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-process/2.8.7/lifecycle-process-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-runtime-android:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-android/2.8.7/lifecycle-runtime-android-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-runtime-compose-android:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-compose-android/2.8.7/lifecycle-runtime-compose-android-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-runtime-ktx-android:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx-android/2.8.7/lifecycle-runtime-ktx-android-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-viewmodel-android:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-android/2.8.7/lifecycle-viewmodel-android-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-viewmodel-compose-android:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-compose-android/2.8.7/lifecycle-viewmodel-compose-android-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-ktx/2.8.7/lifecycle-viewmodel-ktx-2.8.7.pom) |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate/2.8.7/lifecycle-viewmodel-savedstate-2.8.7.pom) |
| `androidx.profileinstaller:profileinstaller:1.3.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/profileinstaller/profileinstaller/1.3.1/profileinstaller-1.3.1.pom) |
| `androidx.savedstate:savedstate:1.2.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate/1.2.1/savedstate-1.2.1.pom) |
| `androidx.savedstate:savedstate-ktx:1.2.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate-ktx/1.2.1/savedstate-ktx-1.2.1.pom) |
| `androidx.startup:startup-runtime:1.1.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/startup/startup-runtime/1.1.1/startup-runtime-1.1.1.pom) |
| `androidx.tracing:tracing:1.0.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/tracing/tracing/1.0.0/tracing-1.0.0.pom) |
| `androidx.versionedparcelable:versionedparcelable:1.1.1` | RDT | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/androidx/versionedparcelable/versionedparcelable/1.1.1/versionedparcelable-1.1.1.pom) |
| `com.android:signflinger:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/signflinger/8.9.1/signflinger-8.9.1.pom) |
| `com.android:zipflinger:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/zipflinger/8.9.1/zipflinger-8.9.1.pom) |
| `com.android.databinding:baseLibrary:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/databinding/baseLibrary/8.9.1/baseLibrary-8.9.1.pom) |
| `com.android.tools:annotations:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/annotations/31.9.1/annotations-31.9.1.pom) |
| `com.android.tools:common:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/common/31.9.1/common-31.9.1.pom) |
| `com.android.tools:dvlib:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/dvlib/31.9.1/dvlib-31.9.1.pom) |
| `com.android.tools:repository:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/repository/31.9.1/repository-31.9.1.pom) |
| `com.android.tools:sdk-common:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/sdk-common/31.9.1/sdk-common-31.9.1.pom) |
| `com.android.tools:sdklib:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/sdklib/31.9.1/sdklib-31.9.1.pom) |
| `com.android.tools.analytics-library:crash:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/analytics-library/crash/31.9.1/crash-31.9.1.pom) |
| `com.android.tools.analytics-library:protos:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/analytics-library/protos/31.9.1/protos-31.9.1.pom) |
| `com.android.tools.analytics-library:shared:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/analytics-library/shared/31.9.1/shared-31.9.1.pom) |
| `com.android.tools.analytics-library:tracker:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/analytics-library/tracker/31.9.1/tracker-31.9.1.pom) |
| `com.android.tools.build:aapt2-proto:8.9.1-12782657` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2-proto/8.9.1-12782657/aapt2-proto-8.9.1-12782657.pom) |
| `com.android.tools.build:aaptcompiler:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/aaptcompiler/8.9.1/aaptcompiler-8.9.1.pom) |
| `com.android.tools.build:apksig:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/apksig/8.9.1/apksig-8.9.1.pom) |
| `com.android.tools.build:apkzlib:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/apkzlib/8.9.1/apkzlib-8.9.1.pom) |
| `com.android.tools.build:builder:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/builder/8.9.1/builder-8.9.1.pom) |
| `com.android.tools.build:builder-model:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/builder-model/8.9.1/builder-model-8.9.1.pom) |
| `com.android.tools.build:builder-test-api:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/builder-test-api/8.9.1/builder-test-api-8.9.1.pom) |
| `com.android.tools.build:bundletool:1.17.2` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/bundletool/1.17.2/bundletool-1.17.2.pom) |
| `com.android.tools.build:gradle:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/8.9.1/gradle-8.9.1.pom) |
| `com.android.tools.build:gradle-api:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle-api/8.9.1/gradle-api-8.9.1.pom) |
| `com.android.tools.build:gradle-settings-api:8.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle-settings-api/8.9.1/gradle-settings-api-8.9.1.pom) |
| `com.android.tools.build:manifest-merger:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/manifest-merger/31.9.1/manifest-merger-31.9.1.pom) |
| `com.android.tools.build:transform-api:2.0.0-deprecated-use-gradle-api` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/transform-api/2.0.0-deprecated-use-gradle-api/transform-api-2.0.0-deprecated-use-gradle-api.pom) |
| `com.android.tools.build.jetifier:jetifier-core:1.0.0-beta10` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/jetifier/jetifier-core/1.0.0-beta10/jetifier-core-1.0.0-beta10.pom) |
| `com.android.tools.build.jetifier:jetifier-processor:1.0.0-beta10` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/build/jetifier/jetifier-processor/1.0.0-beta10/jetifier-processor-1.0.0-beta10.pom) |
| `com.android.tools.ddms:ddmlib:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/ddms/ddmlib/31.9.1/ddmlib-31.9.1.pom) |
| `com.android.tools.layoutlib:layoutlib-api:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/layoutlib/layoutlib-api/31.9.1/layoutlib-api-31.9.1.pom) |
| `com.android.tools.lint:lint-model:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/lint/lint-model/31.9.1/lint-model-31.9.1.pom) |
| `com.android.tools.lint:lint-typedef-remover:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/lint/lint-typedef-remover/31.9.1/lint-typedef-remover-31.9.1.pom) |
| `com.android.tools.utp:android-device-provider-ddmlib-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-device-provider-ddmlib-proto/31.9.1/android-device-provider-ddmlib-proto-31.9.1.pom) |
| `com.android.tools.utp:android-device-provider-gradle-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-device-provider-gradle-proto/31.9.1/android-device-provider-gradle-proto-31.9.1.pom) |
| `com.android.tools.utp:android-device-provider-profile-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-device-provider-profile-proto/31.9.1/android-device-provider-profile-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-host-additional-test-output-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-host-additional-test-output-proto/31.9.1/android-test-plugin-host-additional-test-output-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-host-apk-installer-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-host-apk-installer-proto/31.9.1/android-test-plugin-host-apk-installer-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-host-coverage-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-host-coverage-proto/31.9.1/android-test-plugin-host-coverage-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-host-emulator-control-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-host-emulator-control-proto/31.9.1/android-test-plugin-host-emulator-control-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-host-logcat-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-host-logcat-proto/31.9.1/android-test-plugin-host-logcat-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-host-retention-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-host-retention-proto/31.9.1/android-test-plugin-host-retention-proto-31.9.1.pom) |
| `com.android.tools.utp:android-test-plugin-result-listener-gradle-proto:31.9.1` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/android/tools/utp/android-test-plugin-result-listener-gradle-proto/31.9.1/android-test-plugin-result-listener-gradle-proto-31.9.1.pom) |
| `com.google.android:annotations:4.1.1.4` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/android/annotations/4.1.1.4/annotations-4.1.1.4.pom) |
| `com.google.api.grpc:proto-google-common-protos:2.17.0` | B | Apache-2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/api/grpc/proto-google-common-protos/2.17.0/proto-google-common-protos-2.17.0.pom) |
| `com.google.auto.value:auto-value-annotations:1.6.2` | B | Apache 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/auto/auto-parent/6/auto-parent-6.pom) |
| `com.google.code.findbugs:jsr305:3.0.2` | B | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.pom) |
| `com.google.code.gson:gson:2.10.1` | B | Apache-2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.pom) |
| `com.google.crypto.tink:tink:1.7.0` | B | Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/crypto/tink/tink/1.7.0/tink-1.7.0.pom) |
| `com.google.dagger:dagger:2.28.3` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/dagger/dagger/2.28.3/dagger-2.28.3.pom) |
| `com.google.errorprone:error_prone_annotations:2.18.0` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.18.0/error_prone_annotations-2.18.0.pom) |
| `com.google.flatbuffers:flatbuffers-java:1.12.0` | B | Apache License V2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/flatbuffers/flatbuffers-java/1.12.0/flatbuffers-java-1.12.0.pom) |
| `com.google.guava:failureaccess:1.0.1` | B | The Apache Software License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom) |
| `com.google.guava:guava:32.0.1-jre` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/32.0.1-jre/guava-parent-32.0.1-jre.pom) |
| `com.google.guava:listenablefuture:1.0` | RDT | The Apache Software License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom) |
| `com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava` | B | The Apache Software License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom) |
| `com.google.j2objc:j2objc-annotations:2.8` | B | Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/2.8/j2objc-annotations-2.8.pom) |
| `com.google.jimfs:jimfs:1.1` | B | The Apache Software License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/jimfs/jimfs-parent/1.1/jimfs-parent-1.1.pom) |
| `com.google.protobuf:protobuf-java:3.24.4` | B | BSD-3-Clause（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/protobuf/protobuf-parent/3.24.4/protobuf-parent-3.24.4.pom) |
| `com.google.protobuf:protobuf-java-util:3.22.3` | B | BSD-3-Clause（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/google/protobuf/protobuf-parent/3.22.3/protobuf-parent-3.22.3.pom) |
| `com.google.testing.platform:core-proto:0.0.9-alpha03` | B | The Apache Software License, Version 2.0 | [来源](https://dl.google.com/dl/android/maven2/com/google/testing/platform/core-proto/0.0.9-alpha03/core-proto-0.0.9-alpha03.pom) |
| `com.googlecode.juniversalchardet:juniversalchardet:1.0.3` | B | Mozilla Public License 1.1 (MPL 1.1) | [来源](https://repo.maven.apache.org/maven2/com/googlecode/juniversalchardet/juniversalchardet/1.0.3/juniversalchardet-1.0.3.pom) |
| `com.squareup:javapoet:1.10.0` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/com/squareup/javapoet/1.10.0/javapoet-1.10.0.pom) |
| `com.squareup:javawriter:2.5.0` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/com/squareup/javawriter/2.5.0/javawriter-2.5.0.pom) |
| `com.sun.activation:javax.activation:1.2.0` | B | CDDL/GPLv2+CE（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/sun/activation/all/1.2.0/all-1.2.0.pom) |
| `com.sun.istack:istack-commons-runtime:3.0.8` | B | Eclipse Distribution License - v 1.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/sun/istack/istack-commons/3.0.8/istack-commons-3.0.8.pom) |
| `com.sun.xml.fastinfoset:FastInfoset:1.2.16` | B | Apache License, Version 2.0 / Eclipse Distribution License - v 1.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/sun/xml/fastinfoset/fastinfoset-project/1.2.16/fastinfoset-project-1.2.16.pom) |
| `commons-codec:commons-codec:1.11` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/apache/18/apache-18.pom) |
| `commons-io:commons-io:2.16.1` | B | Apache-2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/apache/31/apache-31.pom) |
| `commons-logging:commons-logging:1.2` | B | The Apache Software License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/apache/13/apache-13.pom) |
| `io.grpc:grpc-api:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-api/1.57.2/grpc-api-1.57.2.pom) |
| `io.grpc:grpc-context:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-context/1.57.2/grpc-context-1.57.2.pom) |
| `io.grpc:grpc-core:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-core/1.57.2/grpc-core-1.57.2.pom) |
| `io.grpc:grpc-netty:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-netty/1.57.2/grpc-netty-1.57.2.pom) |
| `io.grpc:grpc-protobuf:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-protobuf/1.57.2/grpc-protobuf-1.57.2.pom) |
| `io.grpc:grpc-protobuf-lite:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-protobuf-lite/1.57.2/grpc-protobuf-lite-1.57.2.pom) |
| `io.grpc:grpc-stub:1.57.2` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/grpc/grpc-stub/1.57.2/grpc-stub-1.57.2.pom) |
| `io.netty:netty-buffer:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-codec:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-codec-http:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-codec-http2:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-codec-socks:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-common:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-handler:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-handler-proxy:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-resolver:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-transport:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.netty:netty-transport-native-unix-common:4.1.93.Final` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/io/netty/netty-parent/4.1.93.Final/netty-parent-4.1.93.Final.pom) |
| `io.perfmark:perfmark-api:0.26.0` | B | Apache 2.0 | [来源](https://repo.maven.apache.org/maven2/io/perfmark/perfmark-api/0.26.0/perfmark-api-0.26.0.pom) |
| `jakarta.activation:jakarta.activation-api:1.2.1` | B | EDL 1.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/sun/activation/all/1.2.1/all-1.2.1.pom) |
| `jakarta.xml.bind:jakarta.xml.bind-api:2.3.2` | B | Eclipse Distribution License - v 1.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api-parent/2.3.2/jakarta.xml.bind-api-parent-2.3.2.pom) |
| `javax.annotation:javax.annotation-api:1.3.2` | B | CDDL + GPLv2 with classpath exception | [来源](https://repo.maven.apache.org/maven2/javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.pom) |
| `javax.inject:javax.inject:1` | B | Apache-2.0（上游项目声明；POM 未声明） | [来源](https://github.com/javax-inject/javax-inject) |
| `junit:junit:4.13.2` | T | Eclipse Public License 1.0 | [来源](https://repo.maven.apache.org/maven2/junit/junit/4.13.2/junit-4.13.2.pom) |
| `net.java.dev.jna:jna:5.6.0` | B | LGPL, version 2.1 / Apache License v2.0 | [来源](https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.6.0/jna-5.6.0.pom) |
| `net.java.dev.jna:jna-platform:5.6.0` | B | LGPL, version 2.1 / Apache License v2.0 | [来源](https://repo.maven.apache.org/maven2/net/java/dev/jna/jna-platform/5.6.0/jna-platform-5.6.0.pom) |
| `net.sf.jopt-simple:jopt-simple:4.9` | B | The MIT License | [来源](https://repo.maven.apache.org/maven2/net/sf/jopt-simple/jopt-simple/4.9/jopt-simple-4.9.pom) |
| `net.sf.kxml:kxml2:2.3.0` | B | BSD-style 与 Public Domain（上游资料；具体文件范围待确认） | [来源](https://android.googlesource.com/platform/prebuilts/r8/+/9b54bbb2416417c236a7b82f41e7adb5883ca65d/r8.spdx.json) |
| `org.apache.commons:commons-compress:1.21` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/apache/23/apache-23.pom) |
| `org.apache.httpcomponents:httpclient:4.5.14` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-client/4.5.14/httpcomponents-client-4.5.14.pom) |
| `org.apache.httpcomponents:httpcore:4.4.16` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-core/4.4.16/httpcomponents-core-4.4.16.pom) |
| `org.apache.httpcomponents:httpmime:4.5.6` | B | Apache License, Version 2.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/apache/httpcomponents/httpcomponents-client/4.5.6/httpcomponents-client-4.5.6.pom) |
| `org.bitbucket.b_c:jose4j:0.9.5` | B | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/bitbucket/b_c/jose4j/0.9.5/jose4j-0.9.5.pom) |
| `org.bouncycastle:bcpkix-jdk18on:1.79` | B | Bouncy Castle 1.79 上游许可；POM 未声明 | [来源](https://github.com/bcgit/bc-java/blob/r1rv79/LICENSE.md) |
| `org.bouncycastle:bcprov-jdk18on:1.79` | B | Bouncy Castle 1.79 上游许可；POM 未声明 | [来源](https://github.com/bcgit/bc-java/blob/r1rv79/LICENSE.md) |
| `org.bouncycastle:bcutil-jdk18on:1.79` | B | Bouncy Castle 1.79 上游许可；POM 未声明 | [来源](https://github.com/bcgit/bc-java/blob/r1rv79/LICENSE.md) |
| `org.checkerframework:checker-qual:3.33.0` | B | The MIT License | [来源](https://repo.maven.apache.org/maven2/org/checkerframework/checker-qual/3.33.0/checker-qual-3.33.0.pom) |
| `org.codehaus.mojo:animal-sniffer-annotations:1.23` | B | MIT license（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/codehaus/mojo/animal-sniffer-parent/1.23/animal-sniffer-parent-1.23.pom) |
| `org.glassfish.jaxb:jaxb-runtime:2.3.2` | B | Eclipse Distribution License - v 1.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/sun/xml/bind/mvn/jaxb-parent/2.3.2/jaxb-parent-2.3.2.pom) |
| `org.glassfish.jaxb:txw2:2.3.2` | B | Eclipse Distribution License - v 1.0（父级声明） | [来源](https://repo.maven.apache.org/maven2/com/sun/xml/bind/mvn/jaxb-parent/2.3.2/jaxb-parent-2.3.2.pom) |
| `org.hamcrest:hamcrest-core:1.3` | T | New BSD License（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest-parent/1.3/hamcrest-parent-1.3.pom) |
| `org.jdom:jdom2:2.0.6` | B | JDOM 许可；制品 META-INF/LICENSE.txt（POM 未声明） | [来源](https://github.com/hunterhacker/jdom/tree/JDOM-2.0.6) |
| `org.jetbrains:annotations:13.0` | B | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.pom) |
| `org.jetbrains:annotations:23.0.0` | RDT | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/23.0.0/annotations-23.0.0.pom) |
| `org.jetbrains.intellij.deps:trove4j:1.0.20200330` | B | GNU LESSER GENERAL PUBLIC LICENSE 2.1 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.pom) |
| `org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/compose-compiler-gradle-plugin/2.0.21/compose-compiler-gradle-plugin-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-build-statistics:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-build-statistics/2.0.21/kotlin-build-statistics-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-build-tools-api:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-build-tools-api/2.0.21/kotlin-build-tools-api-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-compiler-embeddable/2.0.21/kotlin-compiler-embeddable-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-compiler-runner:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-compiler-runner/2.0.21/kotlin-compiler-runner-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-daemon-client:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-daemon-client/2.0.21/kotlin-daemon-client-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-daemon-embeddable:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-daemon-embeddable/2.0.21/kotlin-daemon-embeddable-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/2.0.21/kotlin-gradle-plugin-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-annotations:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin-annotations/2.0.21/kotlin-gradle-plugin-annotations-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin-api/2.0.21/kotlin-gradle-plugin-api-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-idea:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin-idea/2.0.21/kotlin-gradle-plugin-idea-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-idea-proto:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin-idea-proto/2.0.21/kotlin-gradle-plugin-idea-proto-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-gradle-plugin-model:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin-model/2.0.21/kotlin-gradle-plugin-model-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-klib-commonizer-api:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-klib-commonizer-api/2.0.21/kotlin-klib-commonizer-api-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-native-utils:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-native-utils/2.0.21/kotlin-native-utils-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-reflect:2.0.20` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-reflect/2.0.20/kotlin-reflect-2.0.20.pom) |
| `org.jetbrains.kotlin:kotlin-stdlib:2.0.20` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.0.20/kotlin-stdlib-2.0.20.pom) |
| `org.jetbrains.kotlin:kotlin-stdlib:2.0.21` | RDT | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.0.21/kotlin-stdlib-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.20` | RDT | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk7/1.8.20/kotlin-stdlib-jdk7-1.8.20.pom) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.0` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk7/2.1.0/kotlin-stdlib-jdk7-2.1.0.pom) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.20` | RDT | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/1.8.20/kotlin-stdlib-jdk8-1.8.20.pom) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/2.1.0/kotlin-stdlib-jdk8-2.1.0.pom) |
| `org.jetbrains.kotlin:kotlin-tooling-core:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-tooling-core/2.0.21/kotlin-tooling-core-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-util-io:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-util-io/2.0.21/kotlin-util-io-2.0.21.pom) |
| `org.jetbrains.kotlin:kotlin-util-klib:2.0.21` | B | The Apache License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-util-klib/2.0.21/kotlin-util-klib-2.0.21.pom) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` | RDT | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.7.3/kotlinx-coroutines-android-1.7.3.pom) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4` | B | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.6.4/kotlinx-coroutines-core-jvm-1.6.4.pom) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3` | RDT | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.7.3/kotlinx-coroutines-core-jvm-1.7.3.pom) |
| `org.json:json:20240303` | T | Public Domain | [来源](https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.pom) |
| `org.jvnet.staxex:stax-ex:1.8.1` | B | Eclipse Distribution License - v 1.0 | [来源](https://repo.maven.apache.org/maven2/org/jvnet/staxex/stax-ex/1.8.1/stax-ex-1.8.1.pom) |
| `org.ow2.asm:asm:9.7` | B | BSD-3-Clause | [来源](https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.pom) |
| `org.ow2.asm:asm-analysis:9.7` | B | BSD-3-Clause | [来源](https://repo.maven.apache.org/maven2/org/ow2/asm/asm-analysis/9.7/asm-analysis-9.7.pom) |
| `org.ow2.asm:asm-commons:9.7` | B | BSD-3-Clause | [来源](https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/9.7/asm-commons-9.7.pom) |
| `org.ow2.asm:asm-tree:9.7` | B | BSD-3-Clause | [来源](https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.7/asm-tree-9.7.pom) |
| `org.ow2.asm:asm-util:9.7` | B | BSD-3-Clause | [来源](https://repo.maven.apache.org/maven2/org/ow2/asm/asm-util/9.7/asm-util-9.7.pom) |
| `org.slf4j:slf4j-api:1.7.30` | B | MIT License（父级声明） | [来源](https://repo.maven.apache.org/maven2/org/slf4j/slf4j-parent/1.7.30/slf4j-parent-1.7.30.pom) |
| `org.tensorflow:tensorflow-lite-metadata:0.2.0` | B | The Apache Software License, Version 2.0 | [来源](https://repo.maven.apache.org/maven2/org/tensorflow/tensorflow-lite-metadata/0.2.0/tensorflow-lite-metadata-0.2.0.pom) |

本表记录各模块元数据，并不自动证明二进制分发时所需的全部归属和 NOTICE 已覆盖。

## 运行依赖中的内嵌代码

- `androidx.emoji2:emoji2:1.3.0` 中的 `libs/repackaged.jar` 含 `androidx.emoji2.text.flatbuffer` 包。AndroidX 官方构建脚本记录了从 `com.google.flatbuffers` 到该包名的重定位，见[代码来源线索](https://android.googlesource.com/platform/frameworks/support/+/58618bec2592c538b0a9e469f1492365ef2233e3/emoji2/emoji2/build.gradle)；FlatBuffers 上游使用 [Apache License 2.0](https://github.com/google/flatbuffers/blob/master/LICENSE)。该构建脚本并非经证实的 1.3.0 对应修订，具体内嵌版本和完整源码来源尚待确认。

- [Android 13.0.0_r35 上游构建配置](https://android.googlesource.com/platform/external/noto-fonts/+/refs/tags/android-13.0.0_r35/emoji-compat-flatbuffers/build.gradle)引用 FlatBuffers Java 1.12.0；该标签的 MetadataItem/MetadataList 生成文件保留 Android Open Source Project 2017 版权与 Apache 2.0 声明。本机 1.12.0 制品的 45 个类与内嵌类在包名重定位后的字段及方法签名一致；使用 JDK javap -p -c 比较时，规范化包名、常量池编号、ldc 与 ldc_w、指令位置和跳转目标后反汇编输出一致，原始类文件不逐字节一致。这些证据支持来源关联，但不能单独证明确切版本或对应修订。

## 构建工具中 Trove 的补充线索

- `org.jetbrains.intellij.deps:trove4j:1.0.20200330` 是构建工具依赖，不在所解析的 releaseRuntimeClasspath 中。其 POM 标注 LGPL 2.1；[Gradle 8.11.1 发行版许可](https://github.com/gradle/gradle/blob/v8.11.1/LICENSE)在 Kotlin 编译器使用的 Trove 部分还单独记录 `HashFunctions`、`PrimeFinder` 的 CERN 版权与许可。本机该 Trove4j 制品中存在同名类，但尚未证明这些类与 Gradle 发行版所述版本逐字节一致；准确来源和重新分发时的相应声明仍需核对。项目仓库不直接分发该构建依赖。
