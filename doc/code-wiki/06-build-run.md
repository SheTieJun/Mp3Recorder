# 06 构建与运行

## 开发环境要求

- Android Studio（推荐最新版稳定版）
- JDK 17（CI 与 JitPack 配置使用 17）
- Android SDK：`compileSdk=34`、`targetSdk=34`（见 [gradle.properties](file:///Users/pc/Documents/Github/Mp3Recorder/gradle.properties#L23-L27)）

## 运行 Demo App（Android Studio）

1. 用 Android Studio 打开项目根目录（多模块工程）
2. 等待 Gradle Sync 完成
3. 选择 Run Configuration：`app`
4. 连接设备或启动模拟器，点击 Run

入口 Activity 在 [app AndroidManifest.xml](file:///Users/pc/Documents/Github/Mp3Recorder/app/src/main/AndroidManifest.xml#L32-L41) 中声明为 `.MainActivity`。

## 命令行构建

首次：

```bash
chmod +x ./gradlew
```

构建 Debug APK：

```bash
./gradlew :app:assembleDebug
```

构建 Release APK（CI 同款）：

```bash
./gradlew :app:assembleRelease
```

安装到设备（可选）：

```bash
./gradlew :app:installDebug
```

## 产物位置

- Debug：`app/build/outputs/apk/debug/`
- Release：`app/build/outputs/apk/release/`

## 常见构建注意点

- `buildSrc/tools/projects.kt` 对 lint 使用 `warningsAsErrors = true`，新增/升级依赖后可能出现 lint 阻断构建（见 [projects.kt](file:///Users/pc/Documents/Github/Mp3Recorder/buildSrc/src/main/kotlin/tools/projects.kt#L106-L109)）。
- NDK so 文件由仓库直接携带（`recorder-core/libs/**`、`recorder-st/libs/**`），不是通过外部 CMake 现场编译。

