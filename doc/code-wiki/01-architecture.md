# 01 整体架构

## 目标与能力边界

- 目标：在录音进行的同时把 PCM 实时编码为 MP3，支持暂停/继续/完成，支持进度（时长）与音量回调。
- 可选能力：
  - Mix：录音过程中可播放/切换背景音乐，并把背景音乐混入输出文件。
  - ST：录音过程中对 PCM 做 SoundTouch 处理（变调/变速等），再编码输出 MP3。

## 总体模块视角

项目采用“核心录音抽象 + 三种录音策略”的方式组织：

- `recorder-core`：核心抽象（状态机、AudioRecord 初始化、回调分发、编码线程基类、LAME 封装）。
- `recorder-sim`：仅录音 + MP3 编码（不混音、不变声）。
- `recorder-mix`：录音 + 背景音乐播放/读取 + 混音 + MP3 编码。
- `recorder-st`：录音 + SoundTouch 处理 + MP3 编码。
- `app`：Demo 应用，演示三种 Recorder 的使用方式与 UI 交互。

## 关键数据流（核心链路）

以 Sim 为例（Mix/ST 只是在“PCM 入队前”增加了混音或变声步骤）：

```
AudioRecord.read(PCM)  --(录音线程)-->
  BaseEncodeThread.addTask(PCM)  --(HandlerThread)-->
    LameUtils.encode*(PCM -> MP3 bytes)  --(文件输出流)-->
      output.mp3
```

关键点：

- 录音线程负责“采集 + 计时 + 音量计算 + 把 PCM 推给编码线程”。
- 编码线程（`BaseEncodeThread`）负责“队列化处理 + 调用 LAME 编码 + 写文件 + stop 时 flush/close”。
- 回调（`RecordListener`）在主线程触发（通过 `Handler(Looper.getMainLooper())` 分发）。

## 录音状态机（对外表现）

状态类型：`RecordState`（`recorder-core/src/.../RecordState.kt`）

- `STOPPED`：未录音或已完成
- `RECORDING`：录音中
- `PAUSED`：暂停中

状态变更入口（`BaseRecorder` 抽象定义，具体类实现）：

- `start()`：STOPPED -> RECORDING
- `pause()`：RECORDING -> PAUSED
- `resume()`：PAUSED -> RECORDING
- `complete()`：RECORDING/PAUSED -> STOPPED（触发成功回调 + 文件落盘完成）

## 线程模型

- 录音线程：由各 Recorder `start()` 内部创建（普通 `Thread`），设置 `THREAD_PRIORITY_URGENT_AUDIO`。
- 编码线程：`BaseEncodeThread` 继承 `HandlerThread`，通过 `AudioRecord.setRecordPositionUpdateListener` 定期触发 `processData()`。
- 主线程：状态/进度/错误回调统一切回主线程触发。

