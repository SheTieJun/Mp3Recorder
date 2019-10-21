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
 
 跳转    seekTo
 
 停止计时（和是否播放无关）      AudioPlayer.stopProgress() 
 
 开始计时      AudioPlayer.startProgress()  
```

