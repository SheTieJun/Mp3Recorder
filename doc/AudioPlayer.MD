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
 播放、暂停  AudioPlayer.playOrStop( String url,PlayerListener listener)
 
 设置数据但是不播放  AudioPlayer.playNoStart(String, PlayerListener) 
 
 暂停  	AudioPlayer.pause()
 
 恢复  	AudioPlayer.resume()  
 
 停止  	AudioPlayer.stopPlay() 
 
 跳转    seekTo
 
 停止计时（和是否播放无关）      AudioPlayer.stopProgress() 
 
 开始计时      AudioPlayer.startProgress()  
```
#### 添加缓存播放
[AndroidVideoCache](https://github.com/danikula/AndroidVideoCache)
```
public class CacheManager {
    private static CacheManager mInstance;
    private HttpProxyCacheServer mProxy; //声音代理
    public static synchronized CacheManager newInstance() {
        if (mInstance == null) {
            mInstance = new CacheManager();
        }

        return mInstance;
    }

    public HttpProxyCacheServer getProxy(Context context) {
        if (mProxy == null) {
            mProxy = newProxy(context.getApplicationContext());
        }
        return mProxy;
    }

    /**
     * 创建缓存代理服务
     */
    private HttpProxyCacheServer newProxy(Context context) {


        return   new HttpProxyCacheServer.Builder(context).fileNameGenerator(url -> "."+MD5.getStringMD5(url))
                .headerInjector(url -> Constants.HostDomainReferer.getRefererHeader())
                .cacheDirectory(LizhiStorageUtils.getIndividualCacheDirectory(context))
                .build();
    }

    /**
     * 如果想使用边下载边播放 就使用该方法获取到新的url 再进行播放
     * @param context 上下文
     * @param urlString 播放的URL,必须是HTTP
     * @return 下载的链接地址
     */
    public String  getCacheUrl(Context context, String urlString){
        if (context != null  && !TextUtils.isEmpty(urlString) && urlString.startsWith("http")) {
            return  getProxy(context).getProxyUrl(urlString);
        }
        return urlString;
    }
}
```

```
 public String getCacheUrl(Context context, String urlString){
    if (context != null  && !TextUtils.isEmpty(urlString) && urlString.startsWith("http")) {
        return  CacheManager.newInstance().getProxy(context).getCacheUrl(urlString);
    }
    return urlString;
 }

```

