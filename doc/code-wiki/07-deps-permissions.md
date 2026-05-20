# 07 依赖与权限

## 关键外部依赖

### 1）NDK 动态库

- LAME MP3 编码库：`recorder-core/libs/**/libshetj_mp3lame.so`
  - 对应加载点：`LameUtils` 的 `System.loadLibrary("shetj_mp3lame")`
- SoundTouch：`recorder-st/libs/**/libsoundTouch.so`
  - 用于 STRecorder 变声链路

### 2）App 模块依赖

`app` 额外依赖了一个基础库：

- `com.github.SheTieJun:BaseKit:fcc505b8b7`（见 [app/build.gradle.kts](file:///Users/pc/Documents/Github/Mp3Recorder/app/build.gradle.kts#L45-L56)）

其余 `gradle/libs.versions.toml` 中列出了大量 AndroidX/Compose/Retrofit/Room 等坐标，但本工程当前并未全面使用（属于版本集中管理/历史遗留）。

## Android 权限

`app` 的权限声明见 [app AndroidManifest.xml](file:///Users/pc/Documents/Github/Mp3Recorder/app/src/main/AndroidManifest.xml#L6-L23)。

录音最关键的是：

- `android.permission.RECORD_AUDIO`

与文件/媒体读取相关（Android 13+ 分权限）：

- `READ_MEDIA_AUDIO` / `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`
- `READ_MEDIA_VISUAL_USER_SELECTED`（Android 14+ 重新选择场景）
- Android 12 及以下兼容：`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`（maxSdkVersion=32）

与音频路由/播放相关：

- `MODIFY_AUDIO_SETTINGS`
- `BLUETOOTH`

## 依赖与权限的“合并行为”

- `recorder-*` 子模块也会声明 `RECORD_AUDIO`（库 Manifest 合并进 app）
- 运行时权限申请由 app 自己实现（库侧仅通过 `PermissionListener.needPermission()` 提示“可能缺少权限”）

## 需要你关注的代码风险点（仅记录，不在本次改动中修改）

### 1）app/build.gradle.kts 含明文签名信息

- [app/build.gradle.kts](file:///Users/pc/Documents/Github/Mp3Recorder/app/build.gradle.kts#L18-L25) 中的 `signingConfigs.release` 写死了 `keyPassword/storePassword`，并且仓库里存在 `app/test.jks`。
- 建议：把签名信息迁移到 `local.properties` 或 CI Secret，并避免把 keystore 与密码提交到仓库。

### 2）STRecorder 的 recorderType 可能设置错误

- `STRecorder` 中 `override val recorderType: RecorderType = RecorderType.SIM`（见 [STRecorder.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-st/src/main/java/me/shetj/recorder/soundtouch/STRecorder.kt#L24-L28)）
- 从语义看更合理的值应为 `RecorderType.ST`，否则可能影响外部判断逻辑。

### 3）STRecorderExt 的声道映射疑似不一致

- [STRecorderExt.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-st/src/main/java/me/shetj/recorder/soundtouch/STRecorderExt.kt#L24-L27) 使用了 `AudioFormat.CHANNEL_IN_STEREO == audioChannel` 判断声道。
- 但 `Mp3Option.audioChannel` 在 core 中定义为 `1/2`，这段判断可能永远为 false（导致总是按单声道设置）。

