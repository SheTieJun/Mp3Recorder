###  音频录制和播放

---------------------------------
学习：
 - https://github.com/CarGuo/RecordWave
 - https://github.com/GavinCT/AndroidMP3Recorder
#### 原有功能：

- 边录边转码,没有额外转码时间,录制音频为MP3保存本地。
- 音频权限提示。
- 显示音频的波形，支持单边与双边，自动根据声音大小和控件高度调整波形高度。
- 支持获取声音大小。
- 本地/网络音频播放，音频时长与播放时长支持。
- 播放MP3显示波形数据。
- 录制过程中**暂停**,已录制的那段音频是**可以播放**的.
- 根据录制和播放的波形根据特征变颜色。
- 自定义线大小、方向和绘制偏移。

#### 添加新功能：

- 支持返回已经录制时长和当前声音大小
- 权限获取回调
- 设置最大录制时间，达到最大时间触发自动完成
- Android x
- 播放控制 
- 播放可以设置播放开始时间



#### 该分支实现

- 录制背景音乐
  - 录制中可以中断背景音乐，继续录制声音 （但暂时不支持蓝牙和耳机录制，无法内录）


#### 想加的的东西

- 录制中，试听的功能 （已有）
  
  - 意外发现：录制过程中暂停,已录制的那段音频是可以播放
  
- 录制内部声音（暂时无法实现，只有系统应用才可以内录）
  
  - 希望可以设置录制时内部还是麦克风  
  
 - 录制背景音乐（完成）

    - 通过扬声器器录制，所有暂时只支持录制麦克风 (2019年8月20日加入)
    - 需要更加优秀的思路
  
- 可以设置是否是继续录制功能（完成）

    - ~~demo 中的继续录制是通过文件拼接~~
    - setOutputFile(filePath，isContinue) ：(2019年8月22日加入) 
    - demo已经修改（2019年8月30日）

  

#### PCM 文件时间计算

音频文件大小的计算公式为: 数据量Byte = 采样频率Hz×（采样位数/8）× 声道数 × 时间s

反之：时间s = 数据量Byte / (采样频率Hz×（采样位数/8）× 声道数)



#### 使用

简单修改 给自己项目使用 [demo ](https://github.com/SheTieJun/RecordWave/blob/master/app/src/main/java/me/shetj/audio/record/utils/RecordUtils.kt)

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
	 * 达到“最大”时间，自动完成除非的操作
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
