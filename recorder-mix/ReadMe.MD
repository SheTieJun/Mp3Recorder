[TOC]

# MixRecorder
- [MixRecordUtils](https://github.com/SheTieJun/Mp3Recorder/blob/master/app/src/main/java/me/shetj/mp3recorder/record/utils/MixRecordUtils.kt)
### 背景音频要求

```
 44k ，双声道，16位   
 必须保持和录音的参数一样，如果不一样需要自行转码音频或者修改库代码
```

```
 fix : 已经添加参数设置声道数量
 但是如果设置单声道，然后播放的双声道音乐，音乐会被拉长 
```

### 背景音乐实现思路

1.  播放音乐,可以得到PCM：[PlayBackMusic](https://github.com/SheTieJun/Mp3Recorder/blob/master/doc/PlayBackMusic.MD)
2.  AudioRecord 得到麦克风的声音：PCM
3.  2个PCM进行声音混合，然后Lame转码成MP3 写入到文件
4.  支持耳机，但是分2种情况，设置了PlugConfig 和没有设置PlugConfig
5.  可以设置释放使用PlugConfig,如果使用将会使用 ：没有连接耳机，只用外放的背景音乐；连接耳机，会使用写入合成背景音乐的方式

#### 1. 初始化


```kotlin
recorder {
    recordListener = this@MixRecordUtils
    permissionListener = this@MixRecordUtils
    isDebug = false
    pcmListener = this@RecordUtils
}.buildMix(context)
```

#### 2. 开始录音

```kotlin
  mixRecorder!!.start()
```

#### 3. 暂停、重新开始录音

```kotlin
mRecorder?.onPause() //暂停
mRecorder?.onResume() //重新开始
mRecorder?.state //当前录音的状态 3个专题，停止，录音中，暂停
```

#### 4. 背景音乐相关

```kotlin
mRecorder?.setBackgroundMusic(musicUrl)//设置背景音乐
mRecorder?.setVolume(volume)//设置背景音乐大小0-1,即使没有把手机的声音关闭，也会按照该比例记录到录制的声音中	
mRecorder?.startPlayMusic() //开始播放背景音乐
mRecorder?.isPlayMusic() //是否播放了背景应用
mRecorder?.pauseMusic() //暂停背景音乐
mRecorder?.isPauseMusic()// 背景音乐是否暂停
mRecorder?.resumeMusic() //重新开始播放
```
#### 5.中途替换输出文件
```
//filePath 新的文件
mRecorder.updateDataEncode(filePath)
```

#### 6.中途暂停、重置
```
mRecorder.onReset() //会回调 Listener: onReset()
```
或者
```
mRecorder.onDestroy() //没有任何回调
```

#### 6. 停止录音

```kotlin
mRecorder?.stop()  //完成录音
```

#### 7. 强制启动写入的混音


#### 单双声道

Lame 在使用lame_encode_buffer 在转码双声道时 会出现噪音

解决方法 
```
     if (is2CHANNEL) {
                readSize = buffer.size / 2
                encodedSize = LameUtils.encodeInterleaved(buffer,readSize,mMp3Buffer)
     } else {
                readSize = buffer.size
                encodedSize = LameUtils.encode(buffer, buffer, readSize, mMp3Buffer)
      }
```

[音频基础知识](https://blog.csdn.net/StjunF/article/details/121296111)

```
    /*
    * 16Kbps= 电话音质
    * 24Kbps= 增加电话音质、短波广播、长波广播、欧洲制式中波广播
    * 40Kbps= 美国制式中波广播
    * 56Kbps= 话音
    * 64Kbps= 增加话音（手机铃声最佳比特率设定值、手机单声道MP3播放器最佳设定值）
    * 112Kbps= FM调频立体声广播
    * 128Kbps= 磁带（手机立体声MP3播放器最佳设定值、低档MP3播放器最佳设定值）
    * 160Kbps= HIFI高保真（中高档MP3播放器最佳设定值）
    * 192Kbps= CD（高档MP3播放器最佳设定值）
    * 256Kbps= Studio音乐工作室（音乐发烧友适用）
    * 实际上随着技术的进步，比特率也越来越高，MP3的最高比特率为320Kbps，但一些格式可以达到更高的比特率和更高的音质。
    * 比如正逐渐兴起的APE音频格式，能够提供真正发烧级的无损音质和相对于WAV格式更小的体积，其比特率通常为550kbps-----950kbps。
     */
```
