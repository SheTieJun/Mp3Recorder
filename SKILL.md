# SKILL：如何使用 Mp3Recorder

## 目标

- 在 Android 项目中接入 Mp3Recorder，实现“边录边转 MP3”的录音能力
- 根据需求选择：Sim（纯录音）、Mix（带背景音乐混音）、ST（变声）

## 你需要做的事（最短路径）

1. 添加依赖（`recorder-core` + 任选一个录音实现）
2. 申请运行时权限（至少 `RECORD_AUDIO`）
3. 构建 `BaseRecorder`（`recorder { } -> buildMix/buildSim/buildST`）
4. `setOutputFile(...)` 后调用 `start/pause/resume/complete`
5. 通过 `RecordListener` 接收进度、成功与错误回调

## 1）添加依赖

### A. 依赖远程产物（README 推荐）

```gradle
implementation "com.github.SheTieJun.Mp3Recorder:recorder-core:<version>"
implementation "com.github.SheTieJun.Mp3Recorder:recorder-mix:<version>" // 或 recorder-sim / recorder-st
```

### B. 依赖本仓库模块（用于二次开发）

```gradle
implementation(project(":recorder-core"))
implementation(project(":recorder-mix")) // 或 sim / st
```

## 2）权限与注意事项

- 最少权限：`android.permission.RECORD_AUDIO`
- 如果你要把文件写到外部存储/媒体库，还需要按 Android 版本处理相应读写权限（本仓库 `app` 已给出一套 Manifest 示例）。

库侧只会通过 `PermissionListener.needPermission()` 提示“可能缺权限”，运行时申请逻辑需要你自己实现。

## 3）通用用法（适用于 Mix/Sim/ST）

```kotlin
val recorder = recorder {
    audioSource = MediaRecorder.AudioSource.MIC
    audioChannel = 1
    samplingRate = 48000
    mp3BitRate = 128
    mp3Quality = 5
    enableAudioEffect = true
    isDebug = true

    recordListener = object : RecordListener {
        override fun onStart() {}
        override fun onResume() {}
        override fun onReset() {}
        override fun onRecording(time: Long, volume: Int) {}
        override fun onPause() {}
        override fun onRemind(duration: Long) {}
        override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {}
        override fun onMaxChange(time: Long) {}
        override fun onError(e: Exception) {}
    }

    permissionListener = object : PermissionListener {
        override fun needPermission() {
            // 这里触发你的权限申请（RECORD_AUDIO 等）
        }
    }
}.buildMix(context) // 或 buildSim(context) / buildST()

recorder.setOutputFile(outputFilePath, isContinue = false)
recorder.start()
```

暂停/继续/完成：

```kotlin
recorder.pause()
recorder.resume()
recorder.complete()
recorder.destroy()
```

## 4）Mix：背景音乐混音（推荐）

构建时传入 `context`，用于监听系统音量与耳机变化（见 `buildMix(context)`）。

### 设置背景音乐（建议本地 uri）

```kotlin
recorder.setBackgroundMusic(context, uri, header = null)
recorder.setLoopMusic(true)
recorder.setBGMVolume(0.3f)
recorder.setBackgroundMusicListener(object : PlayerListener { })
```

播放控制：

```kotlin
recorder.startPlayMusic()
recorder.pauseMusic()
recorder.resumeMusic()
recorder.cleanBackgroundMusic()
```

## 5）Sim：纯录音转码

```kotlin
val recorder = recorder { /* 同上 */ }.buildSim(context)
```

Sim 也提供了背景音乐“播放”相关方法，但不会把背景音乐混入输出；如果你的目标是“输出文件里有背景音乐”，优先使用 Mix。

## 6）ST：变声录音（SoundTouch）

```kotlin
val recorder = recorder { /* 同上 */ }.buildST()
val st = recorder.getSoundTouch()
st.changeUse(true)
st.setPitchSemiTones(10f) // 示例：偏女声
```

ST 不支持背景音乐相关能力。

## 7）如何运行本仓库 Demo

- Android Studio 打开根目录，运行 `app` 模块即可（入口 `MainActivity`）。
- 命令行构建：

```bash
./gradlew :app:assembleDebug
```

