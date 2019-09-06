package me.shetj.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;


import java.util.concurrent.atomic.AtomicBoolean;


/**
 * <b>@author：</b> shetj<br>
 * <b>@createTime：</b> 2018/10/23 0023<br>
 * <b>@email：</b> 375105540@qq.com<br>
 * <b>@describe</b> {@link AudioPlayer} 音乐播放</b><br>
 *   <b> 播放 {@link AudioPlayer#playOrStop( String url,PlayerListener listener)}}</b><br>
 *   <b> 设置但是不播放 {@link AudioPlayer#playNoStart(String, PlayerListener)}</b><br>
 *   <b> 暂停  {@link AudioPlayer#pause()} <br/>
 *   <b> 恢复  {@link AudioPlayer#resume()} <br/>
 *   <b> 停止  {@link AudioPlayer#stopPlay() } <br/>
 *   <b> 停止计时  {@link AudioPlayer#stopProgress()}   <br/>
 *   <b> 开始计时  {@link AudioPlayer#startProgress()}   <br/>
 * <br>
 */
public class AudioPlayer implements
		MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener,
		MediaPlayer.OnCompletionListener,
		MediaPlayer.OnSeekCompleteListener
{
	private MediaPlayer mediaPlayer;
	private AudioManager mAudioManager;
	/**
	 * 播放回调
	 */
	private PlayerListener listener;
	/**
	 * 当前播放的url
	 */
	private String currentUrl = "";
	/**
	 * {@link AudioPlayer#onPrepared(MediaPlayer) } and {@link AudioPlayer#onSeekComplete(MediaPlayer)}
	 * true 才会会开始, false 会暂停
	 */
	private AtomicBoolean isPlay = new AtomicBoolean(true);
	/**
	 * 是否是循环
	 */
	private boolean isLoop ;
	/**
	 * {@link AudioPlayer#onPrepared(MediaPlayer)}  如果大于0 表示，不是从头开始
	 */
	private int seekToPlay = 0;
	private final static int HANDLER_PLAYING  = 0x201; //正在录音
	private final static int HANDLER_START    = 0x202;   //开始了
	private final static int HANDLER_RESUME   = 0x208;  //暂停后开始
	private final static int HANDLER_COMPLETE = 0x203;//完成
	private final static int HANDLER_ERROR    = 0x205;   //错误
	private final static int HANDLER_PAUSE    = 0x206;   //暂停
	private final static int HANDLER_RESET    = 0x209;   //重置


	private Handler handler = new Handler(Looper.getMainLooper()){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what){
				case HANDLER_PLAYING:
					if (listener != null && mediaPlayer !=null && mediaPlayer.isPlaying()) {
						listener.onProgress(mediaPlayer.getCurrentPosition(),mediaPlayer.getDuration());
						handler.sendEmptyMessageDelayed(HANDLER_PLAYING,300);
					}
					break;
				case  HANDLER_START :
					if (listener != null && mediaPlayer !=null) {
						listener.onStart(getCurrentUrl(),mediaPlayer.getDuration());
					}
					if (mAudioManager !=null){
						mAudioManager.setMode(AudioManager.MODE_NORMAL);
					}
					break;
				case HANDLER_RESUME:
					if (listener != null) {
						listener.onResume();
					}
					if (mAudioManager !=null){
						mAudioManager.setMode(AudioManager.MODE_NORMAL);
					}
				case  HANDLER_COMPLETE :
					if (listener != null) {
						listener.onCompletion();
					}
					break;
				case  HANDLER_ERROR :
					if (listener != null){
						Exception obj = (Exception) msg.obj;
						listener.onError(obj);
					}
					break;
				case  HANDLER_PAUSE :
					if (listener != null) {
						listener.onPause();
					}
					break;
				case  HANDLER_RESET :
					if (listener != null){
						listener.onStop();
					}
					break;
				default:
					break;
			}
		}
	};


	public AudioPlayer(){
		initMedia();
	}

	public AudioPlayer(AudioManager audioManager) {
		this.mAudioManager = audioManager;
		initMedia();
	}
	/**
	 * 设置 {@link AudioManager} 获取声道
	 * @param context 上下文
	 */
	public void setAudioManager(Context context){
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

//	/**
//	 * 如果想使用边下载边播放 就使用该方法获取到新的url 再进行播放
//	 * @param context 上下文
//	 * @param urlString 播放的URL,必须是HTTP
//	 * @return 下载的链接地址
//	 */
//	public String getCacheUrl(Context context, String urlString){
//		if (context != null  && !TextUtils.isEmpty(urlString) && urlString.startsWith("http")) {
//			return  Manager.newInstance().getProxy(context).getProxyUrl(urlString);
//		}
//		return urlString;
//	}


	/**
	 * 设置声音
	 * @param volume
	 */
	public void setVolume(float volume){
		if (mediaPlayer != null){
			mediaPlayer.setVolume(volume,volume);
		}
	}

	/**
	 * 通过url 比较 进行播放 还是暂停操作
	 * @param url 播放的url
	 * @param listener 监听变化
	 */
	public  void playOrStop(String url, PlayerListener listener) {
		//判断是否是当前播放的url
		if (url.equals(getCurrentUrl()) && mediaPlayer != null){
			if (listener!=null) {
				this.listener = listener;
			}
			if (mediaPlayer.isPlaying()){
				pause();
			}else {
				resume();
			}
		}else {
			//直接播放
			play(url,listener);
		}
	}


	/**
	 * 播放url
	 * @param url 播放的url
	 * @param listener 监听变化
	 */
	private void play(String url, PlayerListener listener){
		if (null != listener) {
			this.listener = listener;
		}
		setCurrentUrl(url);
		if (null == mediaPlayer){
			initMedia();
		}
		configMediaPlayer();
	}

	/**
	 * 设置 但是不播放
	 * @param url 文件路径
	 * @param listener 回调监听
	 */
	public void playNoStart(String url, PlayerListener listener){
		if (null != listener) {
			this.listener = listener;
		}
		setIsPlay(false);
		setCurrentUrl(url);
		if (null == mediaPlayer){
			initMedia();
		}
		configMediaPlayer();
	}

	private void configMediaPlayer() {
		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(currentUrl);
			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setOnSeekCompleteListener(this);
			mediaPlayer.setLooping(isLoop);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 暂停，并且停止计时
	 */
	public void pause(){
		if (mediaPlayer !=null && mediaPlayer.isPlaying()
				&& !TextUtils.isEmpty(getCurrentUrl())) {
			stopProgress();
			mediaPlayer.pause();
			handler.sendEmptyMessage(HANDLER_PAUSE);
		}
	}
	/**
	 * 恢复，并且开始计时
	 */
	public void resume(){
		if (isPause() && !TextUtils.isEmpty(getCurrentUrl())) {
			mediaPlayer.start();
			handler.sendEmptyMessage(HANDLER_RESUME);
			startProgress();
		}
	}

	/**
	 * 是否暂停
	 * @return
	 */
	public  boolean isPause() {
		return  !isPlaying();
	}

	/**
	 * 是否正在播放
	 * @return
	 */
	public  boolean isPlaying() {
		return  (mediaPlayer!=null && mediaPlayer.isPlaying());
	}

	public  void  stopPlay(){
		try {
			if (null != mediaPlayer) {
				stopProgress();
				mediaPlayer.stop();
				handler.sendEmptyMessage(HANDLER_RESET);
				release();
			}
		}catch (Exception e) {
			release();
		}
	}

	/**
	 * 外部设置进度变化
	 */
	public void seekTo(int seekTo){
		if (mediaPlayer != null && !TextUtils.isEmpty(getCurrentUrl())) {
			setIsPlay(!isPause());
			mediaPlayer.start();
			mediaPlayer.seekTo(seekTo);
		}
	}

	public void reset(){
		pause();
		seekTo(0);
	}

	public int getDuration(){
		if (mediaPlayer != null){
			return 	mediaPlayer.getDuration();
		}
		return 0;
	}

	/**
	 * 获取当前播放的url
	 * @return currentUrl
	 */
	public String getCurrentUrl(){
		return currentUrl;
	}


	/**
	 * 修改是否循环
	 * @param isLoop
	 */
	public void setLoop(boolean isLoop){
		this.isLoop = isLoop;
		if (mediaPlayer != null){
			mediaPlayer.setLooping(isLoop);
		}
	}



	/**
	 * 清空播放信息
	 */
	private void release(){
		currentUrl = "";
		//释放MediaPlay
		if (null != mediaPlayer) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}

	/**
	 * 设置是否是播放状态
	 * @param isPlay
	 */
	private void setIsPlay(boolean isPlay) {
		this.isPlay.set(isPlay);
	}


	private void setCurrentUrl(String url){
		currentUrl = url;
	}


	/**
	 * 开始计时
	 * 使用场景：拖动结束
	 */
	public void startProgress() {
		handler.sendEmptyMessage(HANDLER_PLAYING);
	}

	/**
	 * 停止计时
	 * 使用场景：拖动进度条
	 */
	public void stopProgress(){
		handler.removeMessages(HANDLER_PLAYING);
	}


	/**
	 * 设置媒体
	 */
	private void initMedia() {
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
			AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
			attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setAudioAttributes(attrBuilder.build());
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (seekToPlay != 0){
			mp.seekTo(seekToPlay);
			seekToPlay = 0;
		}
		if (!isPlay.get()){
			setIsPlay(true);
			return;
		}
		mp.start();
		startProgress();
		handler.sendEmptyMessage(HANDLER_START);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Message message = Message.obtain();
		message.what = HANDLER_ERROR;
		message.obj = new Exception(String.format("what = %s extra = %s", String.valueOf(what), String.valueOf(extra)));
		handler.sendMessage(message);
		release();
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (!mp.isLooping()){
			listener.onCompletion();
			stopProgress();
			release();
		}
	}


	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if (!isPlay.get()){
			setIsPlay(true);
			pause();
		}
	}
}
