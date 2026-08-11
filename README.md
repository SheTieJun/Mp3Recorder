<h1 align="center">Mp3Recorder</h1>

[![](https://jitpack.io/v/SheTieJun/Mp3Recorder.svg)](https://jitpack.io/#SheTieJun/Mp3Recorder) ![buildWorkflow](https://github.com/SheTieJun/Mp3Recorder/actions/workflows/android.yml/badge.svg)

<p align="center">
 <a href="README.en.md">English</a> | <a href="README.md">简体中文</a>
</p>

边录边转 MP3 的 Android 录音库。

- 边录边转 MP3，支持系统自带 AEC、NC、AGC（通过 `enableAudioEffect` 开关，1.9.1 默认关闭，其他都是默认开启）
- 支持暂停/继续，实时回调**录制时长**和**音量大小**
- 支持背景音乐混音（Mix）、纯录音（Sim）、变声（ST）
- 耳机/外放自动切换背景音乐播放方式

### [Demo APK 下载](https://fir.xcxwo.com/ne21)

![](doc/img/recorder.gif)

***

## 接入

### 添加依赖

```gradle
implementation "com.github.SheTieJun.Mp3Recorder:recorder-core:<版本>"  // 必选
// 以下三选一：
implementation "com.github.SheTieJun.Mp3Recorder:recorder-sim:<版本>"   // 纯录音
implementation "com.github.SheTieJun.Mp3Recorder:recorder-mix:<版本>"   // 背景音乐混音
implementation "com.github.SheTieJun.Mp3Recorder:recorder-st:<版本>"    // 变声
```

### 模块选择

| 场景                   | 模块             |
| -------------------- | -------------- |
| 只需录音转 MP3，无需背景音乐混入输出 | `recorder-sim` |
| 需要背景音乐混入输出文件（K 歌、配乐） | `recorder-mix` |
| 需要实时变声（男声/女声等），无背景音乐 | `recorder-st`  |

> 接入文档：[Wiki](https://github.com/SheTieJun/Mp3Recorder/wiki)

***

## 快速开始

```kotlin
val recorder = recorder {
    audioSource = MediaRecorder.AudioSource.MIC
    audioChannel = 1           // 1=单声道，2=双声道
    samplingRate = 48000
    mp3BitRate = 128
    mp3Quality = 5
    mMaxTime = 1800 * 1_000    // 最大录制时长 ms
    enableAudioEffect = false
    isDebug = true

    recordListener = object : RecordListener {
        override fun onStart() {}
        override fun onRecording(time: Long, volume: Int) {}
        override fun onPause() {}
        override fun onResume() {}
        override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {}
        override fun onError(e: Exception) {}
        override fun onRemind(duration: Long) {}
        override fun onReset() {}
        override fun onMaxChange(time: Long) {}
    }

    permissionListener = object : PermissionListener {
        override fun needPermission() { /* 自行处理 RECORD_AUDIO 权限申请 */ }
    }
}.buildMix(context) // 或 buildSim(context) / buildST()

recorder.setOutputFile("/path/to/output.mp3")
recorder.start()
// recorder.pause() / resume() / complete() / destroy()
```

### Mix - 背景音乐混音

```kotlin
val recorder = recorder { /* ... */ }.buildMix(context)

recorder.setBackgroundMusic(context, uri)
recorder.setLoopMusic(true)
recorder.setBGMVolume(0.3f)

recorder.startPlayMusic()   // 开始播放
recorder.pauseMusic()       // 暂停
recorder.resumeMusic()      // 继续
recorder.cleanBackgroundMusic() // 移除
```

> **注意**：声道数需与背景音乐参数一致，否则会导致音乐拉长或变快。

### ST - 变声

```kotlin
val recorder = recorder { /* ... */ }.buildST()
recorder.getSoundTouch().apply {
    changeUse(true)
    setPitchSemiTones(10f)   // 女声 +10，男声 -10
}
```

***

## API 概览

详见 [BaseRecorder](recorder-core/src/main/java/me/shetj/recorder/core/BaseRecorder.kt)，核心方法：

| 方法                                                | 说明                                  |
| ------------------------------------------------- | ----------------------------------- |
| `setOutputFile(path, isContinue)`                 | 设置输出路径，`isContinue` 为 true 时在文件末尾追加 |
| `start()` / `pause()` / `resume()` / `complete()` | 录音生命周期                              |
| `reset()` / `destroy()`                           | 重置 / 释放资源                           |
| `setBGMVolume(0f..1f)`                            | 背景音乐音量                              |
| `setMaxTime(max, remindDiff)`                     | 最大录制时长，`remindDiff` 为提前提醒差值         |
| `setFilter(lowpass, highpass)`                    | 滤波器                                 |
| `muteRecord(true)`                                | 静音录制（录但不写声音，用于拼接场景）                 |
| `updateDataEncode(path, isContinue)`              | 中途切换输出文件                            |
| `getSoundTouch()`                                 | 获取变声控制器（仅 ST）                       |

***

## 音频参数参考

获取手机当前最佳采样率：

```kotlin
val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val rate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
    ?.toIntOrNull()?.takeIf { it > 0 } ?: 44100
```

> 采样率越高声音越接近原始数据；比特率越高音质越好文件越大。

***

## 其他

- [更新日志](https://github.com/SheTieJun/Mp3Recorder/wiki/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97)
- [Code Wiki](doc/code-wiki)
- [License](LICENSE)

