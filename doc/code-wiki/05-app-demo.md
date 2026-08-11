# 05 App Demo 代码导读

`app` 模块不是库的必须组成部分，但它提供了“正确使用方式”的参考实现（权限、文件路径、页面交互、背景音乐选择等）。

## 入口与页面

- Launcher Activity：`MainActivity`，见 [MainActivity.kt](file:///Users/pc/Documents/Github/Mp3Recorder/app/src/main/java/me/shetj/mp3recorder/MainActivity.kt)
- 录音页面：`RecordActivity`，见 [RecordActivity.kt](file:///Users/pc/Documents/Github/Mp3Recorder/app/src/main/java/me/shetj/mp3recorder/record/activity/mix/RecordActivity.kt)

## Demo 的“录音驱动器”：RecordUtils

定义见 [RecordUtils.kt](file:///Users/pc/Documents/Github/Mp3Recorder/app/src/main/java/me/shetj/mp3recorder/record/utils/RecordUtils.kt)。

你可以把它理解成“业务层 Controller”，把库的 `BaseRecorder` 适配成 UI 可用的接口：

- 负责创建 recorder（`initRecorder()`）：
  - 通过 `recorder { ... }` 配置 `Mp3Option`
  - 根据 `recorderType` 选择 `buildMix/buildSim/buildST`
  - ST 模式会额外调用 `getSoundTouch()` 设置默认变声参数
- 负责输出文件路径与开始/暂停/继续：
  - `startOrPause(...)` 根据 `RecordState` 调用 `start/pause/resume`
  - 默认保存到 `EnvironmentStorage.getPath(packagePath = "record")/...mp3`
- 负责背景音乐控制（仅 Mix/Sim 有意义）：
  - `setBackGroundUrl(context, uri)`：设置背景音乐并同步声道
  - `startOrPauseBGM()`：播放/暂停/恢复
- 负责容错：
  - `resolveError()`：发生错误时 `complete()` 并删除坏文件

## 一个更轻量的入口：mp3Recorder(...)

Demo 里提供了一个 `mp3Recorder(...)` 工厂方法，封装了 `recorder { } + buildX`：

- 定义见 [Recorder.kt](file:///Users/pc/Documents/Github/Mp3Recorder/app/src/main/java/me/shetj/mp3recorder/Recorder.kt)
- 适合在你自己的项目里做二次封装（把常用参数与默认值收敛到一个方法里）

