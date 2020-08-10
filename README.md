# 录音工具-Mp3Recorder

- 边录边转码MP3,支持暂停，实时返回已录制时长和当前声音大小。
- 可添加背景音乐,可以设置背景音乐声音的大小
- 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
- 可设置最大录制时间
- 录音中途可以替换输出文件，比如每60秒替换一个输出文件
- 其他...

#### 背景音乐相关
  - 录制中可以随时中断、播放、替换背景音乐
  - 如果背景音乐的参数我的库中不一样，需要自行设置参数，如果不一样会让背景音乐拉长或者变快


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

#### [![](https://jitpack.io/v/SheTieJun/Mp3Recorder.svg)](https://jitpack.io/#SheTieJun/Mp3Recorder)
```
dependencies {
    implementation 'com.github.SheTieJun:Mp3Recorder:+'
}
```

#### [demo](https://github.com/SheTieJun/Mp3Recorder/tree/master/app) 中继续录制
- ~~继续录制，是通过音频文件合并，因为【继续录制-重录】，希望回到上次录制,所有采用的文件拼接~~ x相关commit已删除,拼接自行参考 [util](/app/src/main/java/me/shetj/mp3recorder/record/utils/Util)
- demo已修改成只要文件存在，自动拼接在末尾最后，所以【继续录制-重录】已无法重置到老的录音，不过可以自行通过copy一份进行上述功能，就是很麻烦
- [MixRecordUtils](https://github.com/SheTieJun/Mp3Recorder/blob/master/app/src/main/java/me/shetj/mp3recorder/record/utils/MixRecordUtils.kt)
- [RecordUtils](https://github.com/SheTieJun/Mp3Recorder/blob/master/app/src/main/java/me/shetj/mp3recorder/record/utils/RecordUtils.kt)
- [MixRecordActivity](https://github.com/SheTieJun/Mp3Recorder/blob/master/app/src/main/java/me/shetj/mp3recorder/record/MixRecordActivity.kt)

<img src="https://github.com/SheTieJun/Mp3Recorder/blob/master/doc/img/recorder.gif" width="35%" height="35%" />


### 缺点

1. 录制声道数设置，因为合成，所有你**需要设置和背景音乐相同的参数**
2. 如果设置单声道，播放的背景是双声道，（MIX）会让音乐拉长；反之双声音合成，背景音乐是单声音，节奏会变快


#### PCM与时间的计算

音频文件大小的计算公式为: 数据量Byte = 采样频率Hz×（采样位数/8）× 声道数 × 时间s

反之：时间s = 数据量Byte / (采样频率Hz×（采样位数/8）× 声道数)

### 初始化
```kotlin
         if (mRecorder == null) {
//          mRecorder = simpleRecorderBuilder(BaseRecorder.RecorderType.MIX,BaseRecorder.AudioSource.VOICE_COMMUNICATION)
            mRecorder = simpleRecorderBuilder(BaseRecorder.RecorderType.MIX,
                BaseRecorder.AudioSource.MIC,
                channel = BaseRecorder.AudioChannel.STEREO)
                mRecorder.setBackgroundMusic(musicUrl!!)//设置默认的背景音乐
                .setRecordListener(onRecording = { time, volume ->
                    //当前已录制时长 和 当前声音大小
                    Timber.i("time = $time  ,volume = $volume")
                },onSuccess = { file, _ ->
                    //录制成功
                    Timber.i("file= %s", file)
                })
                .setPlayListener(onProgress = {current: Int, duration: Int ->
                    //背景音乐播放
                    Timber.i("current = $current  ,duration = $duration")
                })
                .setWax(1f) //超过1f 就是加大声音，但是同时会加大噪音
                .setMaxTime(1800 * 1000) //设置最大时间
        }
```
#### 1.录音控制
``` kotlin
      when {
            mRecorder?.state == RecordState.STOPPED -> {
                if (EmptyUtils.isEmpty(file)) {
                    val mRecordFile = SDCardUtils.getPath("record") + "/" + System.currentTimeMillis() + ".mp3"
                    this.saveFile = mRecordFile
                }else{
                    this.saveFile = file
                }
                mRecorder?.setOutputFile(saveFile,isContinue)
                mRecorder?.start()
            }
            mRecorder?.state == RecordState.PAUSED->{
                mRecorder?.onResume()
            }
            mRecorder?.state == RecordState.RECORDING ->{
                mRecorder?.onPause()
            }
        }  
```

#### 2. 开始录音

```kotlin
  mRecorder!!.start()
```

#### 3. 暂停、重新开始录音

```kotlin
 mRecorder?.onPause() //暂停
 mRecorder?.onResume() //重新开始
 mRecorder?.state     //获取当前录音的状态 3个状态，停止，录音中，暂停
```

#### 4. 背景音乐相关

```kotlin
 mRecorder?.setBackgroundMusic(musicUrl)//设置背景音乐
 mRecorder?.setVolume(volume)//设置背景音乐大小0-1	
 mRecorder?.startPlayMusic() //开始播放背景音乐
 mRecorder?.pauseMusic() //暂停背景音乐
 mRecorder?.isPauseMusic()// 背景音乐是否暂停
 mRecorder?.resumeMusic() //重新开始播放
 mRecorder?.setContextToPlugConfigs(context) //设置次方法后，会使用耳机配置方式,只有MixRecorder 有效
```

> 如果使用耳机配置方式：如果没有连接耳机会只用外放的背景音乐，如果连接上了耳机，会使用写入合成背景音乐的方式

> 如果没有使用耳机，会同时使用外放和写入背景音乐 2 种方法，可能会存在叠音，目前有细微优化，但是不保证兼容所有机型

#### 5. 停止录音

```kotlin
 mRecorder?.stop()  //完成录音
```

#### 6.新增录音参数修改，必须在start()之前调用才有效
```
    //初始Lame录音输出质量
    mRecorder?.setMp3Quality(mp3Quality)
    //设置比特率，关系声音的质量
    mRecorder?.setMp3BitRate(mp3BitRate)
    //设置采样率
    mRecorder?.setSamplingRate(rate)
```



### 1. 录音方式一：[MixRecorder](/doc/MixRecorder.MD)
### 2. 录音方式二： [MP3Recorder](/doc/Mp3Recorder.MD)
### 3. 播放音乐：[AudioPlayer](/doc/AudioPlayer.MD)
### 4. 播放音乐,解码成PCM进行播放：[PlayBackMusic](/doc/PlayBackMusic.MD)
### 5. 播放PCM文件：[AudioTrackManager](/doc/AudioTrackManager.MD)



如果感觉这个库帮助到了你，可以点右上角 "Star" 支持一下 谢谢！ 

### [Update_log](/doc/Update_log.md)

### [License](https://github.com/SheTieJun/Mp3Recorder/blob/master/LICENSE)