# 03 对外 API（如何接入）

本节从“第三方接入库”的角度描述对外 API（不依赖 app 模块）。

## 依赖接入（库使用方）

根据 [README.md](file:///Users/pc/Documents/Github/Mp3Recorder/README.md#L27-L38)：

```gradle
implementation "com.github.SheTieJun.Mp3Recorder:recorder-core:<version>"
implementation "com.github.SheTieJun.Mp3Recorder:recorder-sim:<version>" // 或 recorder-mix / recorder-st
```

工程内直接 module 依赖则使用：

```gradle
implementation(project(":recorder-core"))
implementation(project(":recorder-mix")) // 或 sim / st
```

## 最小使用流程

对外使用的核心对象是 `BaseRecorder`（`recorder-core`）。

最小流程：

1. 通过 `recorder { }` 配置得到 `Mp3Option`
2. 通过 `Mp3Option.buildMix/buildSim/buildST` 构建具体 `BaseRecorder`
3. `setOutputFile(...)`
4. `start/pause/resume/complete`

示例（Mix）：

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
        override fun needPermission() {}
    }
}.buildMix(context)

recorder.setOutputFile(outputFilePath, isContinue = false)
recorder.start()
```

## 配置对象：Mp3Option

定义见 [Mp3Option.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/Mp3Option.kt)。

关键字段：

- `audioSource`：默认 `MIC`，也可用 `VOICE_COMMUNICATION`（系统可能优化录音但音量变小）
- `audioChannel`：1 单声道 / 2 双声道
- `samplingRate`：默认 48000
- `mp3BitRate`：输出 MP3 比特率（如 64/96/128）
- `mp3Quality`：0-9（越小质量越高、速度越慢）
- `mMaxTime`：最大录制时长（毫秒）
- `enableAudioEffect`：系统 NoiseSuppressor/AEC/AGC 开关
- `recordListener` / `permissionListener` / `pcmListener`

## 统一抽象：BaseRecorder

定义见 [BaseRecorder.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/BaseRecorder.kt)。

常用方法（按调用顺序）：

- `setOutputFile(pathOrFile, isContinue)`：设置输出文件；`isContinue=true` 表示在文件末尾继续写（实现“继续录制”的一种方式）
- `setRecordListener(...)` / `setPermissionListener(...)` / `setPCMListener(...)`
- `setMaxTime(maxTime, remindDiffTime)`：最大时长与提醒阈值（默认 max-10s）
- `setSamplingRate(...)` / `setMp3BitRate(...)` / `setMp3Quality(...)` / `setAudioChannel(...)` / `setAudioSource(...)`
- `start()` / `pause()` / `resume()` / `complete()`
- `reset()`：重置状态（不等同 complete）
- `destroy()`：释放资源（AudioEffect、VolumeConfig、LAME 等）

背景音乐相关（仅 Mix/Sim 有意义，ST 不支持）：

- `setBackgroundMusic(url)` 或 `setBackgroundMusic(context, uri, header)`
- `setBackgroundMusicListener(PlayerListener)`
- `setBGMVolume(0.0..1.0)`
- `startPlayMusic()` / `pauseMusic()` / `resumeMusic()` / `cleanBackgroundMusic()`

变声相关（仅 ST 有意义）：

- `getSoundTouch(): ISoundTouchCore`

## 三种构建入口（扩展函数）

- Mix：`Mp3Option.buildMix(context)`，见 [MixRecoderExt.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-mix/src/main/java/me/shetj/recorder/mixRecorder/MixRecoderExt.kt)
- Sim：`Mp3Option.buildSim(context)`，见 [SimRecorderExt.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-sim/src/main/java/me/shetj/recorder/simRecorder/SimRecorderExt.kt)
- ST：`Mp3Option.buildST()`，见 [STRecorderExt.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-st/src/main/java/me/shetj/recorder/soundtouch/STRecorderExt.kt)

## 回调协议

- `RecordListener`：录音生命周期与进度回调，见 [RecordListener.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/RecordListener.kt)
- `PermissionListener`：录音失败时提示“可能缺少权限”，见 [PermissionListener.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/PermissionListener.kt)
- `PCMListener`：编码前对 PCM 做轻量处理（子线程回调），见 [PCMListener.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/PCMListener.kt)

