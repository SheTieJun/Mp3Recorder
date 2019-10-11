package me.shetj.mp3recorder.record.view

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mixRecorder.PlayBackMusic
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.utils.MixRecordUtils
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.PlayerListener

/**
 * 背景音乐控制
 * 1.记录设置过的背景音乐大小
 * 2.记录设置过的背景音乐，但是没有取消不是很合理
 * 3.进入选择背景音乐，会暂停录制
 * 4.无法只录制背景音乐，因为是通过MIC录制
 * 5.录音暂停也会暂停背景音乐
 */
class BackgroundMixMusicView @JvmOverloads constructor(context: Context,
                                                       attrs: AttributeSet? = null,
                                                       defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) , View.OnClickListener, PlayerListener {

    private var mIvPlay: AppCompatImageView
    private var mIvChange: TextView
    private var mSeekBar: SeekBar
    private var mTvName: TextView
    private var mTvVoice:TextView
    private var mTvProgress:TextView
    private var audioPlayer: PlayBackMusic?=null //播放器
    private var recordUtils: MixRecordUtils?=null //录音
    private var addMusicView: LinearLayout  ?=null//添加背景音乐的空间
    private var music: Music?=null //背景文件相关
    private var musicDialog: MusicListBottomSheetDialog?=null//选择背景音乐
    private val max = 600f
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


    fun removeMusic() {
        if (visibility == View.VISIBLE){
            TransitionManager.beginDelayedTransition(parent as ViewGroup?)
            visibility = View.GONE
            addMusicView?.visibility = View.VISIBLE
        }
        music = null
    }

    fun resetMusic() {
        music?.let {
            mTvProgress.text = Util.formatSeconds3(0 / 1000) +"/"+  it.duration
        }
    }

    /**
     * 第一步
     */
    fun setAddMusicView(addMusicView: LinearLayout){
        this.addMusicView = addMusicView
    }
    /**
     * 初始第3步，设置录音控制
     */
    fun setRecordUtil(recordUtils: MixRecordUtils?){
        this.recordUtils = recordUtils
        audioPlayer = recordUtils?.getBgPlayer()
        recordUtils?.setBackgroundPlayerListener(this)
        audioPlayer?.setLoop(true)
        mSeekBar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mTvVoice.text = "$progress%"
                recordUtils?.setVolume(progress/max)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    recordUtils?.setVolume(it.progress/max)
                    mTvVoice.text = "${it.progress}%"
                }
            }
        })
        recordUtils?.setVolume(30/max)
    }
    /**
     * 第2步设置背景音乐选择
     */
    fun setDialog(musicDialog: MusicListBottomSheetDialog) {
        this.musicDialog = musicDialog
    }
    /**
     * isSave 是否保存，默认是保存
     */
    fun setMusic(music: Music,isSave: Boolean = true){
        if (music.url == null){
            //如果是错误数据，直接忽略
            return
        }
        if (isSave) {
            this.music?.let {
                ArmsUtils.makeText( "已切换背景音乐")
            }
        }
        this.music= music
        mTvName.text = music.name
        mTvProgress.text = Util.formatSeconds3(0 / 1000) +"/"+  music.duration
        music.startNoPlayMusic()
        if (visibility == View.GONE){
            TransitionManager.beginDelayedTransition(parent as ViewGroup?)
            visibility = View.VISIBLE
            addMusicView?.visibility = View.GONE
        }
        musicDialog?.dismissBottomSheet()
    }





    private fun pauseMusic(){
        audioPlayer?.pause()
    }


    override fun onClick(v: View?) {
        when(v?.id){
            R.id.iv_play ->{
                music?.apply {
                    recordUtils?.let {
                        if (it.isRecording) {
                            startPlayMusic()
                        }else{
                            it.startOrPause()
                            audioPlayer?.let {
                                startPlayMusic()
                            }
                        }
                    }
                }
            }
            R.id.iv_change ->{
                pauseMusic()
                recordUtils?.pause()
                musicDialog?.showBottomSheet()
            }
        }

    }

    private fun Music.startPlayMusic(): Unit? {
        audioPlayer?.let {
            if (!it.isPlayingMusic) {
                audioPlayer?.setBackGroundUrl(url!!)
                audioPlayer?.startPlayBackMusic()
            }else{
                if (it.isIsPause){
                    it.resume()
                }else{
                    it.pause()
                }
            }
        }
        return recordUtils?.setVolume(mSeekBar.progress / max)
    }

    private fun Music.startNoPlayMusic(): Unit? {
        audioPlayer?.setBackGroundUrl(url!!)
        return recordUtils?.setVolume(mSeekBar.progress / max)
    }

    override fun onStart(url: String, duration: Int) {
        AndroidSchedulers.mainThread().scheduleDirect {
            mIvPlay.setImageResource(R.drawable.icon_pause_bg_music)
        }
    }


    override fun onPause() {
        AndroidSchedulers.mainThread().scheduleDirect {
            mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
        }
    }

    override fun onResume() {
        AndroidSchedulers.mainThread().scheduleDirect {
            mIvPlay.setImageResource(R.drawable.icon_pause_bg_music)
        }
    }

    override fun onStop() {
        AndroidSchedulers.mainThread().scheduleDirect {
            mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
        }
    }

    override fun onCompletion() {
        AndroidSchedulers.mainThread().scheduleDirect {
            mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
        }
    }

    override fun onError(throwable: Exception) {

    }

    override fun onProgress(current: Int, size: Int) {
        AndroidSchedulers.mainThread().scheduleDirect {
            mTvProgress.text = Util.formatSeconds3(current / 1000) +"/"+ Util.formatSeconds3(size / 1000)
        }
    }
}