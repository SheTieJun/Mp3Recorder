# PlayBackMusic进行播放音频

[SheTieJun/PlayBackMusic](https://github.com/SheTieJun/Mp3Recorder/blob/master/recorder/src/main/java/me/shetj/mixRecorder/PlayBackMusic.kt)

### 一、初始化

```
  mPlayBackMusic = PlayBackMusic(
                when (defaultLameInChannel == 2) {
                    true -> AudioFormat.CHANNEL_OUT_STEREO
                    else -> AudioFormat.CHANNEL_IN_LEFT
                }
            )
```

### 二、播放、暂停、循环、回调

```kotlin
  mPlayBackMusic!!.setBackGroundUrl(url) //设置音乐
  mPlayBackMusic!!.setLoop(isLoop)//设置循环
  mPlayBackMusic!!.setBackGroundPlayListener(listener) //设置回调
  mPlayBackMusic!!.startPlayBackMusic()//开始播放
  mPlayBackMusic!!.pause()//暂停
  mPlayBackMusic!!.resume()//重新开始播放
  mPlayBackMusic!!.stop()//停止
  mPlayBackMusic!!.release()//释放
```

### 三、获取时间进度

解码PCM 时，会有对应得时间   mediaExtractor!!.sampleTime

回调会返回当前播放的时间

[解码器的介绍相关](https://isuperqiang.cn/post/android-shi-yong-mediaextractor-yu-mediamuxer-fen-chi-he-cheng/)