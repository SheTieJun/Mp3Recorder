<h1 align="center">Mp3Recorder</h1>

[![](https://jitpack.io/v/SheTieJun/Mp3Recorder.svg)](https://jitpack.io/#SheTieJun/Mp3Recorder) ![buildWorkflow](https://github.com/SheTieJun/Mp3Recorder/actions/workflows/android.yml/badge.svg)

<p align="center">
 <a href="README.en.md">English</a> | <a href="README.md">简体中文</a>
</p>

An Android library for recording and encoding to MP3 in real time.

- Record and encode to MP3 simultaneously, with system AEC/NC/AGC support (toggled via `enableAudioEffect`, off by default since v1.9.1, on by default in earlier versions)
- Pause / resume support with real-time **duration** and **volume** callbacks
- Background music mixing (Mix), pure recording (Sim), voice changing (ST)
- Auto-detect headset for background music playback mode

### [Demo APK](https://fir.xcxwo.com/ne21)

![](doc/img/recorder.gif)

---

## Setup

### Dependencies

```gradle
implementation "com.github.SheTieJun.Mp3Recorder:recorder-core:<version>"  // required
// Choose one:
implementation "com.github.SheTieJun.Mp3Recorder:recorder-sim:<version>"   // pure recording
implementation "com.github.SheTieJun.Mp3Recorder:recorder-mix:<version>"   // background music mix
implementation "com.github.SheTieJun.Mp3Recorder:recorder-st:<version>"    // voice changing
```

### Module Selection

| Use Case | Module |
|----------|--------|
| Record to MP3 only, no background music in output | `recorder-sim` |
| Mix background music into output (karaoke, etc.) | `recorder-mix` |
| Real-time voice changing (male/female pitch), no BGM | `recorder-st` |

> Setup guide: [Wiki](https://github.com/SheTieJun/Mp3Recorder/wiki)

---

## Quick Start

```kotlin
val recorder = recorder {
    audioSource = MediaRecorder.AudioSource.MIC
    audioChannel = 1           // 1=mono, 2=stereo
    samplingRate = 48000
    mp3BitRate = 128
    mp3Quality = 5
    mMaxTime = 1800 * 1_000    // max duration in ms
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
        override fun needPermission() { /* request RECORD_AUDIO permission */ }
    }
}.buildMix(context) // or buildSim(context) / buildST()

recorder.setOutputFile("/path/to/output.mp3")
recorder.start()
// recorder.pause() / resume() / complete() / destroy()
```

### Mix - Background Music

```kotlin
val recorder = recorder { /* ... */ }.buildMix(context)

recorder.setBackgroundMusic(context, uri)
recorder.setLoopMusic(true)
recorder.setBGMVolume(0.3f)

recorder.startPlayMusic()       // start playback
recorder.pauseMusic()           // pause
recorder.resumeMusic()          // resume
recorder.cleanBackgroundMusic() // remove
```

> **Note**: Channel count must match the background music's parameters, otherwise the music may stretch or speed up.

### ST - Voice Changing

```kotlin
val recorder = recorder { /* ... */ }.buildST()
recorder.getSoundTouch().apply {
    changeUse(true)
    setPitchSemiTones(10f)   // +10 for female, -10 for male
}
```

---

## API Overview

See [BaseRecorder](recorder-core/src/main/java/me/shetj/recorder/core/BaseRecorder.kt) for details. Core methods:

| Method | Description |
|--------|-------------|
| `setOutputFile(path, isContinue)` | Set output path; `isContinue=true` appends to existing file |
| `start()` / `pause()` / `resume()` / `complete()` | Recording lifecycle |
| `reset()` / `destroy()` | Reset / release resources |
| `setBGMVolume(0f..1f)` | Background music volume |
| `setMaxTime(max, remindDiff)` | Max recording duration with early reminder |
| `setFilter(lowpass, highpass)` | Audio filters |
| `muteRecord(true)` | Mute recording (records silence, for video stitching) |
| `updateDataEncode(path, isContinue)` | Switch output file mid-recording |
| `getSoundTouch()` | Voice change controller (ST only) |

---

## Audio Tips

Get the device's optimal sample rate:

```kotlin
val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val rate = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
    ?.toIntOrNull()?.takeIf { it > 0 } ?: 44100
```

> Higher sample rate = closer to source; higher bitrate = better quality, larger file.

---

## More

- [Changelog](https://github.com/SheTieJun/Mp3Recorder/wiki/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97)
- [Code Wiki](doc/code-wiki)
- [License](LICENSE)
