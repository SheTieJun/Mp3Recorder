# 录音工具-Mp3Recorder


#### 功能点：

1. 边录边转码,没有额外转码时间,录制音频为MP3保存本地。
2. 录制中添加背景音乐（文件写入，所有支持耳机）， 可以控制写入背景音乐声音的大小
3. 去噪音：MixRecorder使用VOICE_COMMUNICATION ,使用系统自带的AEC
4. 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
6. 可以设置继续录制功能，使用的是Lame,可以直接文件末尾继续录制
7. 音频权限提示，权限获取回调
7. 支持获取声音大小
10. 支持返回已经录制时长和当前声音大小
11. 设置最大录制时间，达到最大时间触发自动完成
10. 录音可以设置声音增强，但是可能会加大噪音~

### 缺点

1. 录制声道数设置，因为合成，所有你需要设置和背景音乐相同的声道数据，背景音乐默认需要是 44k ，双声道，16位

#### 录制（可以选择背景音乐）
  - 录制中可以中断背景音乐，继续录制声音  建议优化这个思路 MixRecorder
  - 如果如果背景音乐的参数我的库中不一样，需要自行修改库中的参数

#### PCM 文件时间计算

音频文件大小的计算公式为: 数据量Byte = 采样频率Hz×（采样位数/8）× 声道数 × 时间s
反之：时间s = 数据量Byte / (采样频率Hz×（采样位数/8）× 声道数)

#### Gradle

Step 1. Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
        }
}
```

Step 2. Add the dependency

```
dependencies {
    implementation 'com.github.SheTieJun:Mp3Recorder:0.0.1'
}
```

#### [demo](https://github.com/SheTieJun/Mp3Recorder/tree/master/app) 中继续录制 
- 继续录制，是通过音频文件合并，因为重录希望上次录制的没有丢掉,所有采用的文件拼接



### 1. 录音方式一：[MixRecorder](/doc/MixRecorder.MD)  带上耳机，可很好的录制背景音乐

### 2. 录音方式二： [MP3Recorder](/doc/Mp3Recorder.MD)

### 3. 播放音乐：[AudioPlayer](/doc/AudioPlayer.MD)

### 4. 播放音乐,可以得到PCM：[PlayBackMusic](/doc/PlayBackMusic.MD)

### 5. 播放PCM文件：[AudioTrackManager](/doc/AudioTrackManager.MD)

