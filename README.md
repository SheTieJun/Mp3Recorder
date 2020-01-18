# 录音工具-Mp3Recorder

#### 功能点：

1. 边录边转码,没有额外转码时间,录制音频为MP3保存本地。
2. 添加背景音乐,可以背景音乐声音的大小
3. 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
4. 可以设置同文件继续录制功能
5. 支持返回当前已录制时长和当前声音大小
6. 设置最大录制时间，达到最大时间触发自动完成回调


### 缺点

1. 录制声道数设置，因为合成，所有你需要设置和背景音乐相同的声道数据，背景音乐默认需要是 44k ，单声道，16位
因为单声道录制的声音比较清脆
2. 如果设置单声道，播放的背景是双声道，（MIX）会让音乐拉长；反之双声音合成，背景音乐是单声音，节奏会变快
3. 去噪音：MixRecorder使用VOICE_COMMUNICATION ,使用系统自带的AEC,声音会变小、
4. 录音可以设置声音增强，但是可能会加大噪音~

#### 录制（可以选择背景音乐）
  - 录制中可以中断背景音乐，继续录制声音  建议优化这个思路 MixRecorder
  - 如果如果背景音乐的参数我的库中不一样，需要自行修改库中的参数

#### PCM与时间的计算

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

[![](https://jitpack.io/v/SheTieJun/Mp3Recorder.svg)](https://jitpack.io/#SheTieJun/Mp3Recorder)
```
dependencies {
    implementation 'com.github.SheTieJun:Mp3Recorder:+'
}
```

#### [demo](https://github.com/SheTieJun/Mp3Recorder/tree/master/app) 中继续录制 
- 继续录制，是通过音频文件合并，因为重录希望上次录制的没有丢掉,所有采用的文件拼接
- [MixRecordUtils](/app/src/main/java/me/shetj/mp3recorder/record/utils/MixRecordUtils.kt) 
- [RecordUtils](/app/src/main/java/me/shetj/mp3recorder/record/utils/RecordUtils.kt)
- [MixRecordActivity](/app/src/main/java/me/shetj/mp3recorder/record/MixRecordActivity.kt) 

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
```

#### 5. 停止录音

```kotlin
 mRecorder?.stop()  //完成录音
```
   

### 1. 录音方式一：[MixRecorder](/doc/MixRecorder.MD) 
### 2. 录音方式二： [MP3Recorder](/doc/Mp3Recorder.MD)
### 3. 播放音乐：[AudioPlayer](/doc/AudioPlayer.MD)
### 4. 播放音乐,解码成PCM进行播放：[PlayBackMusic](/doc/PlayBackMusic.MD)
### 5. 播放PCM文件：[AudioTrackManager](/doc/AudioTrackManager.MD)


 