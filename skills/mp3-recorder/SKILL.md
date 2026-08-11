---
name: "mp3-recorder"
description: "在 Android 项目中接入 Mp3Recorder 实现边录边转 MP3。Invoke when user wants to add audio recording with real-time MP3 encoding, background music mixing (Mix), or voice changing (ST/SoundTouch) to an Android project."
---

# Mp3Recorder 集成指南

## 模块选型

| 模块 | 用途 | 何时使用 |
|------|------|----------|
| `recorder-core` | 基础库，所有模块的共同依赖 | 必须依赖 |
| `recorder-sim` | 纯录音 → MP3 转码 | 只需录音转 MP3，不需要背景音乐混入 |
| `recorder-mix` | 录音 + 背景音乐混入输出 | 需要输出文件中包含背景音乐（如 K 歌、配乐录音） |
| `recorder-st` | 变声录音（SoundTouch） | 需要实时变声（男声/女声/机器人等），不支持背景音乐 |

## 1. 添加依赖

### 远程依赖（推荐）

```gradle
// 必须：核心库
implementation "com.github.SheTieJun.Mp3Recorder:recorder-core:<version>"
// 按需选一个：
implementation "com.github.SheTieJun.Mp3Recorder:recorder-sim:<version>"
implementation "com.github.SheTieJun.Mp3Recorder:recorder-mix:<version>"
implementation "com.github.SheTieJun.Mp3Recorder:recorder-st:<version>"
```

### 本地模块依赖（二次开发时）

```gradle
implementation(project(":recorder-core"))
implementation(project(":recorder-mix")) // 或 :recorder-sim / :recorder-st
```

## 2. 权限

- 必需权限：`android.permission.RECORD_AUDIO`
- 写入外部存储需额外处理 `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`（Android 版本相关）
- 库侧通过 `PermissionListener.needPermission()` 回调提示缺权限，**运行时申请逻辑需自行实现**

## 3. 通用用法（Mix / Sim / ST 通用）

```kotlin
import me.shetj.recorder.core.*

val recorder = recorder {
    audioSource = MediaRecorder.AudioSource.MIC  // 音频源，默认 MIC
    audioChannel = 1                              // 1=单声道，2=双声道
    samplingRate = 48000                          // 采样率
    mp3BitRate = 128                              // 比特率，64/96/128
    mp3Quality = 5                                // Lame 质量 1-7，数值越大质量越好越慢
    mMaxTime = 1800 * 1_000                       // 最大录制时长 ms
    isDebug = true                                // 输出调试日志
    enableAudioEffect = true                      // 启用噪声抑制/回声消除/自动增益

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
        override fun onMuteRecordChange(isMute: Boolean) {}
    }

    permissionListener = object : PermissionListener {
        override fun needPermission() {
            // 触发运行时权限申请（RECORD_AUDIO）
        }
    }
}.buildMix(context) // 或 buildSim(context) / buildST()

// 设置输出文件后开始录音
recorder.setOutputFile(outputFilePath, isContinue = false)
recorder.start()
```

### 录音生命周期

```kotlin
recorder.start()       // 开始录音
recorder.pause()       // 暂停
recorder.resume()      // 继续
recorder.complete()    // 主动完成（触发 onSuccess）
recorder.destroy()     // 释放资源
recorder.reset()       // 重置状态（不释放资源，可重新 setOutputFile + start）
```

## 4. Mix：背景音乐混音

`buildMix(context)` 需要传入 `context`（用于监听系统音量和耳机插拔）。

```kotlin
val recorder = recorder { /* ... */ }.buildMix(context)

// 设置背景音乐（建议本地 uri）
recorder.setBackgroundMusic(context, uri, header = null)
// 或通过路径：
recorder.setBackgroundMusic("/sdcard/music/bgm.mp3")

recorder.setLoopMusic(true)                     // 是否循环，默认 true
recorder.setBGMVolume(0.3f)                     // 背景音乐音量 0.0~1.0
recorder.setBackgroundMusicListener(object : PlayerListener {
    override fun onStart(duration: Int) {}
    override fun onPause() {}
    override fun onResume() {}
    override fun onStop() {}
    override fun onCompletion() {}
    override fun onError(throwable: Exception?) {}
    override fun onProgress(current: Int, duration: Int) {}
})

// 背景音乐播放控制
recorder.startPlayMusic()
recorder.pauseMusic()
recorder.resumeMusic()
recorder.cleanBackgroundMusic()                 // 移除背景音乐
```

注意：Mix 会把背景音乐混入输出文件。如果只想播放背景音乐但不混入，用 Sim。

## 5. Sim：纯录音转码

```kotlin
val recorder = recorder { /* ... */ }.buildSim(context)
```

Sim 也提供背景音乐播放方法（`setBackgroundMusic` 等），但**不会把背景音乐混入输出文件**。

## 6. ST：变声录音

```kotlin
val recorder = recorder { /* ... */ }.buildST()

val st = recorder.getSoundTouch()
st.changeUse(true)                    // 启用变声
st.setPitchSemiTones(10f)            // 偏女声 +10，偏男声 -10，范围 [-12, +12]
st.setRate(1.0f)                     // 播放速率，默认 1.0
st.setTempo(1.0f)                    // 节拍，默认 1.0
st.setRateChange(0f)                 // 速率增量百分比 (-50..+100)
st.setTempoChange(0f)                // 节拍增量百分比 (-50..+100)

// 中途切换是否变声
st.changeUse(false)

// 也支持离线处理 wav 文件
st.processFile(inputWavPath, outputWavPath)
```

ST **不支持背景音乐**相关方法。

## 7. 关键 API 补充

### Mp3Option 配置项（`recorder { }` 内可用）

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `audioSource` | `MIC` | 音频源：MIC、VOICE_COMMUNICATION、CAMCORDER、DEFAULT、VOICE_RECOGNITION 等 |
| `audioChannel` | `1` | 声道数：1 单声道，2 双声道 |
| `samplingRate` | `48000` | 采样率 Hz |
| `mp3BitRate` | `64` | MP3 比特率 kbps（16 ~ 320，推荐 96/128） |
| `mp3Quality` | `3` | Lame 质量 1~7，1 最快 |
| `mMaxTime` | `1800 * 1000` | 最大录制时长 ms |
| `isDebug` | `false` | 开启调试日志 |
| `enableAudioEffect` | `false` | 开启系统噪声抑制/回声消除/自动增益 |

### 中途切换输出文件

```kotlin
recorder.updateDataEncode(newFilePath, isContinue = false)
```

### 静音录制（录制但不写入声音，用于拼接场景）

```kotlin
recorder.muteRecord(true)  // 静音
recorder.muteRecord(false) // 恢复
```

### 滤波器

```kotlin
recorder.setFilter(lowpassFreq = 3000, highpassFreq = 200)
```

### 接续录制

```kotlin
recorder.setOutputFile(existFilePath, isContinue = true) // 在已有文件末尾追加
recorder.setCurDuration(existingDuration)                // 同步已录制时长
recorder.start()
```

### 查询状态

```kotlin
recorder.state      // RecordState: RECORDING / PAUSED / STOPPED
recorder.isActive   // 录音是否活跃（暂停时仍为 true，完成后为 false）
recorder.duration   // 已录制时长 ms
recorder.realVolume // 当前音量 0~100
```

## 8. 常见问题

- **录音开始后修改参数无效**：`setSamplingRate`、`setMp3BitRate`、`setMp3Quality`、`setFilter` 等方法必须在 `start()` 之前调用，否则会打印 error 日志并忽略。
- **ST 不支持背景音乐**：调用 `setBackgroundMusic` 等方法会无效果或抛异常。
- **`getSoundTouch()` 仅在 ST 下可用**：Sim/Mix 调用会抛 `NullPointerException`。
- **权限**：库只回调 `needPermission()`，不会自动申请权限，需在回调中自行调用 `ActivityCompat.requestPermissions`。
