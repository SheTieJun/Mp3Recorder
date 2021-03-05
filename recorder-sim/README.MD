# Mp3Recorder

### 使用方法

```
filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
// kotlin:  mRecorder = recorderBuilder(permissionListener = this, recordListener = this)
//设置
//MediaRecorder.AudioSource.VOICE_COMMUNICATION;//**对麦克风中类似ip通话的交流声音进行识别，默认会开启回声消除和自动增益*/
mRecorder = new SimRecorder(MediaRecorder.AudioSource.VOICE_COMMUNICATION,BuildConfig.DEBUG)      
			.setOutputFile(filePath)        
			.setRecordListener(simRecordListener)    			
			.setPermissionListener(simRecordListener)        
			.setMaxTime(1800*1000);
//开始录制
mRecorder.start();
//暂停录制
mRecorder.onPause();
//重新开始录制
mRecorder.onResume();
//完成录制
mRecorder.stop();
```

#### 录制中替换输出音频路径
```
 mRecorder?.updateDataEncode(outputFilePath);
```


#### 也支持背景音乐，但不能戴耳机

```
 mRecorder?.bgPlayer  //获取背景音乐播放器
 mRecorder?.setBackgroundMusic(musicUrl)//设置背景音乐
 mRecorder?.setVolume(volume)//设置背景音乐大小0-1	,但是如果关闭手机的声音，会无法录制到声音中
 mRecorder?.startPlayMusic() //开始播放背景音乐
 mRecorder?.pauseMusic() //暂停背景音乐
 mRecorder?.isPauseMusic()// 背景音乐是否暂停
 mRecorder?.resumeMusic() //重新开始播放
```

