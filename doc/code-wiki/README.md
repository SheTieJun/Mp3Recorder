# Mp3Recorder Code Wiki

本目录是一套面向代码阅读与二次开发的 Wiki 文档，目标是帮助你在不读完全部源码的前提下，快速理解项目结构、核心链路与对外 API。

## 文档导航

- [01-整体架构](01-architecture.md)
- [02-模块划分与依赖](02-modules.md)
- [03-对外 API（如何接入）](03-public-api.md)
- [04-内部实现（录音/编码线程）](04-internals.md)
- [05-App Demo 代码导读](05-app-demo.md)
- [06-构建与运行](06-build-run.md)
- [07-依赖与权限](07-deps-permissions.md)

## 一句话理解这个项目

- 这是一个 Android 多模块工程：用 AudioRecord 实时采集 PCM，通过 NDK LAME（`libshetj_mp3lame.so`）边录边编码输出 MP3；可选支持背景音乐混音（Mix）与变声（SoundTouch）。

