package me.shetj.mp3recorder.record.view

import android.annotation.SuppressLint
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
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.utils.RecordUtils
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.OnVolumeChange
import me.shetj.recorder.core.VolumeConfig

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

    private var recordUtils: RecordUtils?=null
    private var music: Music?=null
    private var musicDialog: MusicListBottomSheetDialog?=null
    private val max :Float by lazy { VolumeConfig.getInstance(context).getMaxVoice().toFloat() }
    private val volumeConfig: VolumeConfig by lazy { VolumeConfig.getInstance(context) }

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

    private val onVolumeChange: OnVolumeChange = object : (Float) -> Unit(){
        override fun invoke(p1: Float) {
            mSeekBar.progress = (p1*volumeConfig.getMaxVoice()).toInt()
        }
    }

    fun setRecordUtil(recordUtils: RecordUtils?){
        this.recordUtils = recordUtils
        recordUtils?.setBackgroundPlayerListener(this)
        mSeekBar.max = volumeConfig.getMaxVoice()
        mSeekBar.progress = volumeConfig.getCurVolume()
        showVolumeString(mSeekBar)
        mSeekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.let { showVolumeString(it) }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    showVolumeString(seekBar)
                }
            }

        })
    }

    private fun showVolumeString(
        it: SeekBar
    ) {
        val fl = it.progress / max
        volumeConfig.setAudioVoiceF(fl)
        mTvVoice.text = "${(fl * 100).toInt()}%"
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

    private fun pauseMusic(){
        recordUtils?.pause()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.iv_play ->{
                music?.apply {
                    recordUtils?.let {
                        if (it.isRecording) {
                            recordUtils?.startOrPauseBGM()
                        }else{
                            recordUtils?.startOrPause()
                            recordUtils?.startOrPauseBGM()
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

    override fun onStart(duration: Int) {
        mIvPlay.setImageResource(R.drawable.icon_pause_bg_music)
    }

    override fun onPause() {
        mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
    }

    override fun onResume() {
        mIvPlay.setImageResource(R.drawable.icon_pause_bg_music)
    }

    override fun onStop() {
        mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
    }

    override fun onCompletion() {

    }

    override fun onError(throwable: Exception) {

    }

    @SuppressLint("SetTextI18n")
    override fun onProgress(current: Int, duration: Int) {
        mTvProgress.text = Util.formatSeconds3(current/1000)+"/"+Util.formatSeconds3(duration/1000)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        VolumeConfig.getInstance(context).addChangeListener(onVolumeChange)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        VolumeConfig.getInstance(context).removeChangeListener(onVolumeChange)
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
        music?.let {
            mTvProgress.text = Util.formatSeconds3(0/1000)+"/"+Util.formatSeconds3((it.duration/1000).toInt())
        }
    }
}