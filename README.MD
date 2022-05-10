# 录音工具-Mp3Recorder 

 [![](https://jitpack.io/v/SheTieJun/Mp3Recorder.svg)](https://jitpack.io/#SheTieJun/Mp3Recorder) ![buildWorkflow](https://github.com/SheTieJun/Mp3Recorder/actions/workflows/android.yml/badge.svg)

- 边录边转码MP3,支持暂停，实时返回已录制时长和当前声音大小。
- 可添加背景音乐,可以设置背景音乐声音的大小
- 支持[**变音**](#变音录音推荐)（变音的录音工具，暂时不支持背景音乐），可边录边变音边转码MP3
- 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
- 可以使用默认耳机配置方式：如果没有连接耳机会只用外放的背景音乐，如果连接上了耳机，会使用写入合成背景音乐的方式
- 其他..


### Demo1 [APP下载](http://d.zqapps.com/ne21)
<img src="https://github.com/SheTieJun/Mp3Recorder/blob/master/doc/img/recorder.gif" width="35%" height="35%" />

### Demo2 [多进程录音代码示例](https://github.com/SheTieJun/Beloved/blob/master/M-recorder/src/main/java/me/shetj/recorder/func/service/RecordService.kt)

## 一、[接入配置文档](https://github.com/SheTieJun/Mp3Recorder/wiki/%E6%8E%A5%E5%85%A5%E6%96%87%E6%A1%A3)

### 1. 如何选择

```
implementation "com.github.SheTieJun.Mp3Recorder:recorder-core:$sdk_version"//必选
implementation "com.github.SheTieJun.Mp3Recorder:recorder-sim:$sdk_version"//可选
implementation "com.github.SheTieJun.Mp3Recorder:recorder-mix:$sdk_version"//可选
implementation "com.github.SheTieJun.Mp3Recorder:recorder-st:$sdk_version" //可选，已初步测试（变音参数需要自己调试）
```

### 请选择合适的Recorder进行编写功能
- 如果只是录音，对背景音乐没有要求，带耳机后没有背景音乐：[SimRecorder](recorder-sim)
- 如果录音中，需要随时添加背景音乐，而且要求支持带耳机后也有背景音乐：[MixRecorder](recorder-mix)
- 如果录音中，没有背景音乐，但是支持随时进行变音：[STRecorder](recorder-st)


### 2. [《背景音乐》录音推荐](recorder-sim)：

```
implementation 'com.github.SheTieJun.Mp3Recorder:recorder-mix:版本号'
implementation 'com.github.SheTieJun.Mp3Recorder:recorder-core:版本号'
```

#### 1) . 背景音乐录制相关

- 录制中可以随时中断、播放、替换背景音乐
- 如果背景音乐的参数我的库中不一样，需要自行设置参数，如果不一样会让背景音乐拉长或者变快

#### 2).  缺点

- 录制声道数设置，因为合成，所有你**需要设置和背景音乐相同的参数**
- 如果设置单声道，播放的背景是双声道，（MIX）会让音乐拉长；反之双声音合成，背景音乐是单声音，节奏会变快
- tip: 背景音乐支持网络链接，但是网络差的时候可能导致卡顿,所有请尽量不要使用网络链接作为背景音乐应用

### 3. [《变音》录音推荐](recorder-st)(初步完成，未完善):

```
implementation 'com.github.SheTieJun.Mp3Recorder:recorder-core:版本号'
implementation 'com.github.SheTieJun.Mp3Recorder:recorder-st:版本号'
```

#### 1). 变音录制相关(SoundTouch)

- 录制中可以随时中断、重新继续进行变音、变速等等功能
```kotlin
    //指定播放速率
    fun setRate(speed: Float)
    //一般用来设置倍速，我们变音，默认 1.0就好
    fun setTempo(tempo: Float)

    //在原速1.0基础上，按百分比做增量，取值(-50 .. +100 %)
    fun setRateChange(@FloatRange(from = -50.0, to = 100.0) rateChange: Float)

    //在原速1.0基础上 源tempo=1.0，小于1则变慢；大于1变快 tempo (-50 .. +100 %)
    fun setTempoChange(@FloatRange(from = -50.0, to = 100.0) tempoChange: Float)

    //在源pitch的基础上，使用半音(Semitones)设置新的pitch [-12.0,12.0]
    //男声:-
    //女声:+
    fun setPitchSemiTones(@FloatRange(from = -12.0, to = 12.0) pitch: Float)

```

## 二、[使用说明文档](https://github.com/SheTieJun/Mp3Recorder/wiki/%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)

```
abstract class BaseRecorder {

    // 录音Recorder 是否在活动，暂停的时候isActive 还是true,只有录音结束了才会为false
    var isActive = false

    //当前状态
    var state = RecordState.STOPPED

    //录制时间
    var duration = 0L

    //设置是否使用耳机配置方式
    abstract fun setContextToPlugConfig(context: Context): BaseRecorder

    //设置声音配置，设置后，修改设置声音大小会修改系统播放声音的大小
    abstract fun setContextToVolumeConfig(context: Context): BaseRecorder

    //设置录音输出文件,
    abstract fun setOutputFile(outputFile: String, isContinue: Boolean = false): BaseRecorder

    /**
     * 设置录音输出文件
     * @param outputFile 设置输出路径
     * @param isContinue 表示是否拼接在文件末尾，实现继续录制的一种方式
     */
     abstract fun setOutputFile(outputFile: File, isContinue: Boolean = false): BaseRecorder

    //设置录音监听
    abstract fun setRecordListener(recordListener: RecordListener?): BaseRecorder

    //设置权限监听
    abstract fun setPermissionListener(permissionListener: PermissionListener?): BaseRecorder

    //设计背景音乐的url,本地的(网络的可能造成卡死)
    abstract fun setBackgroundMusic(url: String): BaseRecorder

    //是否循环播放，默认true
    abstract fun setLoopMusic(isLoop: Boolean): BaseRecorder

    //背景音乐的url,兼容Android Q
    abstract fun setBackgroundMusic(context: Context,uri: Uri,header: MutableMap<String, String>?): BaseRecorder

    //设置背景音乐的监听
    abstract fun setBackgroundMusicListener(listener: PlayerListener): BaseRecorder


    //初始Lame录音输出质量
    abstract fun setMp3Quality(@IntRange(from = 0 ,to = 9)mp3Quality: Int): BaseRecorder

    //设置比特率，关系声音的质量
    abstract fun setMp3BitRate(@IntRange(from = 16) mp3BitRate: Int): BaseRecorder

    //设置采样率，默认44100
    abstract fun setSamplingRate(@IntRange(from = 8000) rate: Int): BaseRecorder

    //设置音频声道数量，每次录音前可以设置修改，开始录音后无法修改
    abstract fun setAudioChannel(@IntRange(from = 1,to = 2)channel: Int = 1):Boolean

    //设置音频来源，每次录音前可以设置修改，开始录音后无法修改
    abstract fun setAudioSource(@Source audioSource: Int = MediaRecorder.AudioSource.MIC):Boolean


    //初始最大录制时间 和提醒时间 remind = maxTime - remindDiffTime
    abstract fun setMaxTime(maxTime: Int, remindDiffTime: Int? = null): BaseRecorder


    //设置增强系数(不建议修改，因为会产生噪音~,细小的噪音放大就是噪音)
    abstract fun setWax(wax: Float): BaseRecorder

    //设置背景声音大小
    abstract fun setBGMVolume(volume: Float): BaseRecorder

    //移除背景音乐
    abstract fun cleanBackgroundMusic()

    //开始录音
    abstract fun start()

    //完成录音
    abstract fun complete()

    //重新开始录音
    abstract fun resume()

    //替换输出文件
    abstract fun updateDataEncode(outputFilePath: String)

    //暂停录音
    abstract fun pause()

    //是否设置了并且开始播放了背景音乐
    abstract fun isPlayMusic(): Boolean

    //开始播放音乐
    abstract fun startPlayMusic()

    //是否在播放音乐
    abstract fun isPauseMusic(): Boolean

    //暂停背景音乐
    abstract fun pauseMusic()

    //重新播放背景音乐
    abstract fun resumeMusic()

    //重置
    abstract fun reset()

    //结束释放
    abstract fun destroy()

    //2021年11月10号，新增变音：STRecorder(SoundTouchRecorder)，其他抛出异常
    open fun getSoundTouch():ISoundTouchCore 
}
```
## 其他

### 尝试支持 Compose
[Mp3Recorder-Compose](https://github.com/SheTieJun/Mp3Recorder-Compose)

### 1. [Old version](https://github.com/SheTieJun/Mp3Recorder/tree/master_copy)

### 2. [Update_log](https://github.com/SheTieJun/Mp3Recorder/wiki/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97)

### 3. [License](https://github.com/SheTieJun/Mp3Recorder/blob/master/LICENSE)

