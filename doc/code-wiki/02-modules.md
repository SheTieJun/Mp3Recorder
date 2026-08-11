# 02 模块划分与依赖

## 模块列表

工程模块由 [settings.gradle.kts](file:///Users/pc/Documents/Github/Mp3Recorder/settings.gradle.kts#L18-L23) 声明：

- `:recorder-core`：核心库（AudioRecord 基础能力 + 编码线程抽象 + LAME 封装 + 通用工具）
- `:recorder-sim`：SimRecorder（仅录音转码 MP3）
- `:recorder-mix`：MixRecorder（背景音乐混音 + 转码 MP3）
- `:recorder-st`：STRecorder（SoundTouch 变声 + 转码 MP3）
- `:app`：Demo App

## 依赖关系图

### 工程内（当前仓库的 module 依赖）

```
app
 ├─ implementation(recorder-core)
 ├─ implementation(recorder-sim)
 ├─ implementation(recorder-mix)
 └─ implementation(recorder-st)

recorder-sim  ─ compileOnly(recorder-core)
recorder-mix  ─ compileOnly(recorder-core)
recorder-st   ─ compileOnly(recorder-core)
recorder-core ─ (无模块依赖)
```

说明：

- `recorder-sim/mix/st` 通过 `compileOnly(project(":recorder-core"))` 编译期依赖 core，但不会把 core 打进产物；因此在“作为库被依赖”时，需要确保同时依赖 `recorder-core`（根 README 也有说明）。

### 发布坐标（README 推荐）

根 [README.md](file:///Users/pc/Documents/Github/Mp3Recorder/README.md#L27-L38) 的建议是：

- 必选：`recorder-core`
- 任选其一或多个：`recorder-sim` / `recorder-mix` / `recorder-st`

## 模块职责细化

### recorder-core（核心）

核心文件（建议先看）：

- `BaseRecorder`：录音状态机 + AudioRecord 初始化 + 回调分发 + 音量/时长计算 + 组装编码线程
- `BaseEncodeThread`：编码线程抽象，负责 flush/close 与 stop/error 收尾
- `Mp3Option` + `recorder { }`：配置 DSL
- `LameUtils`：NDK LAME 封装（`System.loadLibrary("shetj_mp3lame")`）
- `RecordListener` / `PermissionListener` / `PCMListener`：对外回调协议

### recorder-sim（仅录音转码）

- `SimRecorder`：使用 `AudioRecord.read(ShortArray)` 读取 PCM，直接喂给 `DataEncodeThread` 编码。
- `DataEncodeThread`：实现 `BaseEncodeThread`，完成 PCM->MP3 的队列化编码。

### recorder-mix（背景音乐混音）

- `MixRecorder`：使用 `AudioRecord.read(ByteArray)` 读取 PCM；若有背景音乐，要求 mic buffer 与 bg buffer 对齐后再入队。
- `PlayBackMusic`：背景音乐播放与 PCM 获取（基于 MediaExtractor/解码，详见 `doc/PlayBackMusic.MD`）。
- `MixEncodeThread`：将 mic PCM 与 bg PCM 混合后喂给 LAME。

### recorder-st（SoundTouch 变声）

- `STRecorder`：录音采集后交由 `DataSTEncodeThread`，在编码前通过 `SoundTouchKit` 处理 PCM。
- `ISoundTouchCore`：对外暴露变声控制接口（setTempo/setPitch 等）。

### app（Demo）

- `MainActivity`：入口页，进入录音 Demo。
- `RecordUtils`：演示如何构建/驱动 recorder（start/pause/resume/complete、背景音乐、变声设置）。
- `RecordPage`、`BackgroundMixMusicView`：UI 交互封装。

