[TOC]

# MixRecorder

### 背景音频要求

```
 44k ，双声道，16位   
 必须保持和录音的参数一样，如果不一样需要自行转码音频或者修改库代码
```

### 背景音乐实现思路

1.  播放音乐,可以得到PCM：[PlayBackMusic](https://github.com/SheTieJun/Mp3Recorder/blob/master/doc/PlayBackMusic.MD)
2.  AudioRecord 得到麦克风的声音：PCM
3.  2个PCM进行声音混合，然后Lame转码成MP3 写入到文件

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
                .setOutputFile(filePath)//设置输出文件
                .setBackgroundMusic(musicUrl, true)//设置默认的背景音乐
                .setRecordListener(listener)
                .setPermissionListener(listener)
                setWax(wax)//声音增强
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


```
  readSize = buffer.size / 2
                val leftData = ShortArray(readSize)
                val rightData = ShortArray(readSize)
                for (i in 0 until readSize) {
                    if (i % 2 == 0) {
                        System.arraycopy(buffer, i * 2, leftData, i, 2)
                    } else {
                        System.arraycopy(buffer, i * 2, rightData, i - 1, 2)
                    }
                }
```
