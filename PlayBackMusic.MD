# AudioTrack 进行播放音频

[SheTieJun/Mp3Recorder](https://github.com/SheTieJun/Mp3Recorder/blob/master/recorder/src/main/java/me/shetj/mixRecorder/PlayBackMusic.kt)

### 一、初始化

```
private val mSampleRate = 44100
private val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT//一个采样点16比特-2个字节
private val defaultChannel = 2
private fun initAudioTrack(): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(
            mFrequence,
            mPlayChannelConfig, mAudioEncoding
        )
        // 实例AudioTrack
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(mAudioEncoding)
                        .setSampleRate(mSampleRate)
                        .setChannelMask(defaultChannel)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            return  AudioTrack(
                AudioManager.STREAM_MUSIC,
                mSampleRate,
                defaultChannel, mAudioEncoding, bufferSize,
                AudioTrack.MODE_STREAM
            )
        }
    }
```

### 二、播放、暂停、循环

```
private inner class PlayNeedMixAudioTask internal constructor(private val listener: BackGroundFrameListener?) :
        Thread() {
        override fun run() {
            try {
                if (audioTrack == null) {
                    audioTrack = initAudioTrack()
                    setVolume(volume)
                }
                // 开始播放
                audioTrack!!.play()
                while (isPlayingMusic) {
                    if (!isIsPause) {
                        val pcm = mAudioDecoder!!.pcmData
                        val temp = pcm?.bufferBytes
                        if (pcm == null || temp == null){
                            if (mIsLoop) {
                                playHandler.sendEmptyMessage(PROCESS_REPLAY)
                                sleep(50)
                            }
                            continue
                        }
                        audioTrack!!.write(temp, 0, temp.size)
                        playerListener?.onProgress((pcm.time/1000).toInt(),
                            (mAudioDecoder!!.mediaFormat!!.getLong(MediaFormat.KEY_DURATION)/1000).toInt())
                        listener?.onFrameArrive(temp)
                    } else {
                        //如果是暂停
                        try {
                            //防止死循环ANR
                            sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
                playerListener?.onStop()
                audioTrack!!.stop()
                audioTrack!!.flush()
                audioTrack!!.release()
                audioTrack = null
            } catch (e: Exception) {
                Log.e("mp3Recorder", "error:" + e.message)
                playerListener?.onError(e)
            } finally {
                isPlayingMusic = false
                isIsPause = false
                playerListener?.onCompletion()
            }
        }
    }
```

### 三、获取时间进度

解码PCM 时，会有对应得时间   mediaExtractor!!.sampleTime

[解码器的介绍相关](https://isuperqiang.cn/post/android-shi-yong-mediaextractor-yu-mediamuxer-fen-chi-he-cheng/)