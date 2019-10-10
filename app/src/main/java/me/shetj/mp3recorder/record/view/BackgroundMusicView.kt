package me.shetj.mp3recorder.record.view

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.utils.RecordUtils
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.AudioPlayer
import me.shetj.player.PlayerListener
import timber.log.Timber

/**
 * 背景音乐控制
 */
class BackgroundMusicView @JvmOverloads constructor(context: Context,
                                                    attrs: AttributeSet? = null,
                                                    defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) , View.OnClickListener, PlayerListener {


    private var mIvPlay:AppCompatImageView
    private var mIvChange: TextView
    private var mSeekBar: AppCompatSeekBar
    private var mTvName: TextView
    private var mTvVoice:TextView
    private var mTvProgress:TextView

    private var audioPlayer: AudioPlayer?=null
    private var recordUtils: RecordUtils?=null
    private var music: Music?=null
    private var musicDialog: MusicListBottomSheetDialog?=null

    init {
        //设置view
        val view = LayoutInflater.from(context).inflate(R.layout.bg_music_view, null)
        mIvPlay= view.findViewById(R.id.iv_play)
        mIvChange = view.findViewById(R.id.iv_change)
        mSeekBar = view.findViewById(R.id.seekBar)
        mTvName = view.findViewById(R.id.tv_music_name)
        mTvVoice = view.findViewById(R.id.tv_voice)
        mTvProgress = view.findViewById(R.id.tv_time_progress)
        addView(view)

        mIvPlay.setOnClickListener(this)
        mIvChange.setOnClickListener(this)


    }
    fun setRecordUtil(recordUtils: RecordUtils?){
        this.recordUtils = recordUtils
        audioPlayer = recordUtils?.getBgPlayer()
        recordUtils?.setBackgroundPlayerListener(this)
        audioPlayer?.setAudioManager(context)
        audioPlayer?.setLoop(true)
        mSeekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mTvVoice.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    audioPlayer?.setVolume(it.progress/100f)
                    mTvVoice.text = "${it.progress}%"
                }
            }

        })
        mSeekBar.progress = 30
        mTvVoice.text = "30%"
        audioPlayer?.setVolume(30/100f)
    }

    fun setMusic(music: Music){
        this.music= music
        mTvName.text = music.name
        mTvProgress.text = Util.formatSeconds3(0/1000)+"/"+ Util.formatSeconds3((music.duration/1000).toInt())
        recordUtils?.setBackgroundMusic(music.url!!)
        if (visibility == View.GONE){
            TransitionManager.beginDelayedTransition(parent as ViewGroup?)
            visibility = View.VISIBLE
        }
        musicDialog?.dismissBottomSheet()
    }

    fun pauseMusic(){
        audioPlayer?.pause()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.iv_play ->{
                music?.apply {
                    recordUtils?.let {
                        if (it.isRecording) {
                            audioPlayer?.playOrPause(url!!, this@BackgroundMusicView)
                        }else{
                            ArmsUtils.makeText("请先开始录音！")
                        }
                    }
                }
            }
            R.id.iv_change ->{
                pauseMusic()
                musicDialog?.showBottomSheet()
            }
        }

    }

    override fun onStart(url: String, duration: Int) {
        mIvPlay.setImageResource(R.drawable.selector_weike_record_pause)
    }

    override fun onPause() {
        mIvPlay.setImageResource(R.drawable.selector_weike_record_play)
    }

    override fun onResume() {
        mIvPlay.setImageResource(R.drawable.selector_weike_record_pause)
    }

    override fun onStop() {
        mIvPlay.setImageResource(R.drawable.selector_weike_record_play)
    }

    override fun onCompletion() {

    }

    override fun onError(throwable: Exception) {

    }

    override fun onProgress(current: Int, size: Int) {
        mTvProgress.text = Util.formatSeconds3(current/1000)+"/"+Util.formatSeconds3(size/1000)
    }

    //不展示在界面的时候
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.i("onDetachedFromWindow")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.i("onAttachedToWindow")
    }

    fun setDialog(musicDialog: MusicListBottomSheetDialog) {
        this.musicDialog = musicDialog
    }

    fun removeMusic() {
        if (visibility == View.VISIBLE){
            TransitionManager.beginDelayedTransition(parent as ViewGroup?)
            visibility = View.GONE
        }
        music = null
    }

    fun resetMusic() {
        audioPlayer?.reset()
        music?.let {
            mTvProgress.text = Util.formatSeconds3(0/1000)+"/"+Util.formatSeconds3((it.duration/1000).toInt())
        }
    }
}