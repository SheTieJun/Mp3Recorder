# 04 内部实现（录音/编码线程）

## 录音初始化：BaseRecorder.initAudioRecorder

入口：`BaseRecorder.initAudioRecorder()`（见 [BaseRecorder.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/BaseRecorder.kt#L435-L471)）

关键步骤：

1. 计算 `AudioRecord.getMinBufferSize(...)` 并对齐到 `FRAME_COUNT`（160）
2. 构造 `AudioRecord(audioSource, samplingRate, channelConfig, format, bufferSize)`
3. 计算 `bytesPerSecond`（用于时长计算）
4. 初始化系统 AudioEffect（NoiseSuppressor/AEC/AGC，可开关）
5. 初始化 LAME 参数：`initLameOption()` -> `LameUtils.init(...)`
6. 创建并启动编码线程：`mEncodeThread = createEncodeThread(); mEncodeThread.start()`
7. `AudioRecord.setRecordPositionUpdateListener(encodeThread, encodeHandler)` + 设置 `positionNotificationPeriod`

## 录音循环（各 Recorder 的 start）

三种 Recorder 的共同点：

- `start()` 内创建录音线程（普通 `Thread`），循环 `AudioRecord.read(...)`
- 每次读到 PCM：
  - 更新时长（基于 `bytesPerSecond`）
  - 计算音量（`calculateRealVolume`）
  - 调用 `mEncodeThread.addTask(...)` 把 PCM 推给编码线程

差异点：

- `SimRecorder`：读取 `ShortArray`（16-bit PCM），直接入队编码
- `MixRecorder`：读取 `ByteArray`，同时读取背景音乐 PCM，入队时带上 bg 数据与音量参数
- `STRecorder`：读取 `ShortArray`，在编码线程中做 SoundTouch 处理

## 编码线程抽象：BaseEncodeThread

定义见 [BaseEncodeThread.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/recorder/core/BaseEncodeThread.kt)。

核心职责：

- 管理输出 `FileOutputStream(file, isContinue)`
- 维护 MP3 输出 buffer：`mMp3Buffer`
- 定期从内部队列取 PCM 并编码写入文件：`processData()`
- stop/error 收尾：
  - stop：处理完队列 -> `flushAndRelease()` -> quit looper
  - error：处理完队列 -> `flushAndRelease()` -> 删除坏文件

### flushAndRelease 做了什么

`flushAndRelease()`：

- `lameUtils.flush(mMp3Buffer)` 写 MP3 尾帧
- `FileOutputStream.write(...)` 落盘
- `FileOutputStream.close()`
- `isEnableVBR=true` 时写 VBR Header（代码中多处提示暂不建议启用）
- `lameUtils.close()` 释放 native 资源

## LAME 封装：LameUtils

定义见 [LameUtils.kt](file:///Users/pc/Documents/Github/Mp3Recorder/recorder-core/src/main/java/me/shetj/ndk/lame/LameUtils.kt)。

关键点：

- `System.loadLibrary("shetj_mp3lame")`，对应 `recorder-core/libs/**/libshetj_mp3lame.so`
- 对外提供 `init/encode/flush/close`，以及可选的 `writeVBRHeader`
- `BaseRecorder.initLameOption()` 会把采样率/声道/比特率/滤波器/VBR 开关等传入 native

## 时长与音量计算

### 时长

- 通过 `bytesPerSecond` 将累计读取字节数转换为毫秒：
  - `duration = 1000 * totalBytes / bytesPerSecond`
- Mix/Sim/ST 的计算方式略有差异（读到的是 byte 或 short），但核心是“累计音频数据量 / 单位时间字节数”。

### 音量

`BaseRecorder.calculateRealVolume(...)` 对 PCM 做 RMS + dB 近似：

- `sqrt(sum(buffer[i]^2) / N)` 取均方根
- `log10(rms) * 20` 转 dB，结果裁剪到 0..100
- 低于阈值会把 buffer 置 0，降低噪音影响

