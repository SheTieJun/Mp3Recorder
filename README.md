###  MP3音频录制和播放

#### 原有功能：

- 边录边转码,没有额外转码时间,录制音频为MP3保存本地。
- 音频权限提示。
- 显示音频的波形，支持单边与双边，自动根据声音大小和控件高度调整波形高度。
- 支持获取声音大小。
- 本地/网络音频播放，音频时长与播放时长支持。
- 播放MP3显示波形数据。
- 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
- 支持返回已经录制时长和当前声音大小
- 权限获取回调
- 设置最大录制时间，达到最大时间触发自动完成
- 录制和播放控制
- 播放可以设置播放开始时间
- 添加speex进行去噪音，但是好像效果不佳（MixRecorder使用VOICE_COMMUNICATION ,使用系统自带的AEC）
- 可以控制背景音乐声音的大小
- 可以设置是否是继续录制功能（已完成）
- 录制中添加背景音乐（支持耳机，文件写入）
- 录制中，试听的功能 
- 录制声道数设置，因为合成，所有你需要设置和背景音乐相同的声道数据，背景音乐需要是44k


so ： https://github.com/GavinCT/AndroidMP3Recorder

#### 录制（可以选择背景音乐）
  - 录制中可以中断背景音乐，继续录制声音  建议优化这个思路 MixRecorder
#### PCM 文件时间计算

音频文件大小的计算公式为: 数据量Byte = 采样频率Hz×（采样位数/8）× 声道数 × 时间s

反之：时间s = 数据量Byte / (采样频率Hz×（采样位数/8）× 声道数)

#### demo 继续录制 
- 继续录制，是通过音频文件合并，因为重录希望上次录制的没有丢掉


##### 使用 MixRecorder
``` kotlin
  private fun mixRecord() {
        if (mixRecorder == null) {
            val  filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + "bg.mp3"
            Timber.i("musicUrl = %s", musicUrl)
            val listener = object : SimRecordListener() {
                override fun onSuccess(file: String, time: Long) {
                    super.onSuccess(file, time)
                    Timber.i("file= %s", file)
       
                }
            }
            mixRecorder = MixRecorder()
                    .setOutputFile(filePath)//设置输出文件
                    .setBackgroundMusic(musicUrl, true)//设置默认的背景音乐
                    .setRecordListener(listener)
                    .setPermissionListener(listener)
                    .setMaxTime(1800 * 1000)//设置最大时间
        }
        if (!mixRecorder!!.isRecording) {
            mixRecorder!!.bgPlayer.startPlayBackMusic()
            mixRecorder!!.start()
            ArmsUtils.makeText("开始录音")
        }

    }
    
    //录制的暂停课重新开始
     private fun recordPauseOrResume() {
        when {
            mixRecorder?.state == RecordState.PAUSED -> {
                mixRecorder?.onResume()
            }
            mixRecorder?.state == RecordState.RECORDING -> {
                mixRecorder?.onPause()
            }
            else ->{
                ArmsUtils.makeText(   "请先开始录音")
            }
        }
    }
    
    //只暂停背景音乐，不暂停录制
    private fun startOrPause() {
        if (!mixRecorder!!.isRecording) {
            ArmsUtils.makeText(   "请先开始录音")
            return
        }
        mixRecorder?.bgPlayer?.let {
            if (it.isIsPause){
                it.resume()
            }else{
                it.pause()
            }
        }
    }
    
```


#### 使用 MP3Recorder
```java
filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
//设置
mRecorder = new MP3Recorder(MediaRecorder.AudioSource.VOICE_COMMUNICATION,BuildConfig.DEBUG)      
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

状态回调

```java
/**
 * 录音回调
 * @author shetj
 */
public interface RecordListener {
	/**
	 * 开始录音
	 */
	void onStart();
	/**
	 * 重新
	 */
	void onResume();
	/**
	 * 重置
	 */
	void onReset();
	/**
	 * 正在录音
	 * @param time 已经录制的时间
	 * @param volume 当前声音大小
	 */
	void onRecording(long time, int volume);
	/**
	 * 暂停
	 */
	void onPause();
	/**
	 * 到达提醒时间，默认提醒时间是最大时间前10秒
	 */
	void onRemind();
	/**
	 * 录制成功
	 * @param file 保存的文件
	 * @param time 录制时间 （毫秒ms）
	 */
	void onSuccess(String file, long time) ;
	/**
	 * 设置最大进度条，触发 （毫秒ms）
	 */
	void setMaxProgress(long time);
	/**
	 * 计算时间错误时
	 */
	void onError(Exception e);
	/**
	 * 达到“最大”时间，自动完成结束的操作
	 *（毫秒ms）
	 */
	void autoComplete(String file, long time);
}
```

```java
public interface PermissionListener {
	/**
	 * 缺少权限时回调该接口
	 */
	void needPermission();
}
```
