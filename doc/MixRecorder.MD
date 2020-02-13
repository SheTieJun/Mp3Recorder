[TOC]

# MixRecorder

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
4.  支持耳机

#### 1. 初始化

##### 方式一：

```kotlin
   mRecorder = mixRecorderBuilder(permissionListener = this, recordListener = this)
```

##### 方式二：

```kotlin
    val listener = object : SimRecordListener() {
                override fun onSuccess(file: String, time: Long) {
                    super.onSuccess(file, time)
                    //录音完成
                    Timber.i("file= %s", file)
              }
              
     //设置录音来源和单双声道  ，录制中无法改变属性 AudioRecord 的相关属性
     mixRecorder = MixRecorder(MediaRecorder.AudioSource.VOICE_COMMUNICATION,2)
     
     //设置相关参数和回调
     mixRecorder.setOutputFile(filePath)//设置输出文件
                .setBackgroundMusic(musicUrl, true)//设置默认的背景音乐
                .setRecordListener(listener)
                .setPermissionListener(listener)
                .setWax(wax)//声音增强,增大噪音
                .setMaxTime(1800 * 1000)//设置最大时间
```

#### 2. 开始录音

```kotlin
  mixRecorder!!.start()
```

#### 3. 暂停、重新开始录音

```kotlin
 mixRecorder?.onPause() //暂停
 mixRecorder?.onResume() //重新开始
 mixRecorder?.state //当前录音的状态 3个专题，停止，录音中，暂停
```

#### 4. 背景音乐相关

```kotlin
 mixRecorder?.bgPlayer  //获取背景音乐播放器
 
 mixRecorder?.setBackgroundMusic(musicUrl)//设置背景音乐
 
 mixRecorder?.setVolume(volume)//设置背景音乐大小0-1	
 
 mixRecorder?.startPlayMusic() //开始播放背景音乐
 
 mixRecorder?.pauseMusic() //暂停背景音乐
 mixRecorder?.isPauseMusic()// 背景音乐是否暂停
 mixRecorder?.resumeMusic() //重新开始播放
```

#### 5. 停止录音

```kotlin
 mixRecorder?.stop()  //完成录音
```

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
https://www.jianshu.com/p/87095c155ea5