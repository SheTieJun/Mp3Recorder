#### [AudioPlayer]

##### 如何使用

```
  private void initBgMusicPlayer() {
        if (backgroundPlayer == null) {
            backgroundPlayer = new AudioPlayer();
        }
    }
```

```
 播放  AudioPlayer.playOrStop( String url,PlayerListener listener)
 
 设置但是不播放  AudioPlayer.playNoStart(String, PlayerListener) 
 
 暂停  	AudioPlayer.pause()
 
 恢复  	AudioPlayer.resume()  
 
 停止  	AudioPlayer.stopPlay() 
 
 停止计时      AudioPlayer.stopProgress() 
 
 开始计时      AudioPlayer.startProgress()  
```

```
public interface PlayerListener {

	/**
	 * 开始播放
	 * @param url 播放路径
	 * @param duration 最大时间
	 */
	void onStart(String url,int duration);

	/**
	 * 暂停
	 */
	void onPause();

	/**
	 * 继续播放
	 */
	void onResume();

	/**
	 * 停止释放
	 */
	void onStop();

	/**
	 * 播放结束
	 */
	void onCompletion();

	/**
	 * 错误
	 * @param throwable 异常信息
	 */
	void onError(Exception throwable);

	/**
	 * 进度条
	 * @param current 当前播放位置
	 * @param size 一共
	 */
	void onProgress(int current, int size);
}

```