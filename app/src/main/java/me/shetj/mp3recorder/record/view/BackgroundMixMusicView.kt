/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.MusicQ
import me.shetj.mp3recorder.record.utils.MixRecordUtils
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.OnVolumeChange
import me.shetj.recorder.core.VolumeConfig

/**
 * 背景音乐控制
 * 1.记录设置过的背景音乐大小
 * 2.记录设置过的背景音乐，但是没有取消不是很合理
 * 3.进入选择背景音乐，会暂停录制
 * 4.无法只录制背景音乐，因为是通过MIC录制
 * 5.录音暂停也会暂停背景音乐
 */
class BackgroundMixMusicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener, PlayerListener {

    private var mIvPlay: AppCompatImageView
    private var mIvChange: TextView
    private var mSeekBar: SeekBar
    private var mTvName: TextView
    private var mTvVoice: TextView
    private var mTvProgress: TextView
    private var recordUtils: MixRecordUtils? = null //录音
    private var addMusicView: LinearLayout? = null//添加背景音乐的空间
    private var music: MusicQ? = null //背景文件相关
    private var musicDialog: MusicListBottomQSheetDialog? = null//选择背景音乐
    private val max: Float by lazy { VolumeConfig.getInstance(context).getMaxVoice().toFloat() }
    private val volumeConfig: VolumeConfig by lazy { VolumeConfig.getInstance(context) }

    init {
        //设置view
        val view = LayoutInflater.from(context).inflate(R.layout.bg_music_view, null)
        mIvPlay = view.findViewById(R.id.iv_play)
        mIvChange = view.findViewById(R.id.iv_change)
        mSeekBar = view.findViewById(R.id.seekBar)
        mTvName = view.findViewById(R.id.tv_music_name)
        mTvVoice = view.findViewById(R.id.tv_voice)
        mTvProgress = view.findViewById(R.id.tv_time_progress)
        addView(view)
        mIvPlay.setOnClickListener(this)
        mIvChange.setOnClickListener(this)
    }

    private val onVolumeChange: OnVolumeChange = object : (Float) -> Unit() {
        override fun invoke(p1: Float) {
            mSeekBar.progress = (p1 * max).toInt()
        }
    }

    fun removeMusic() {
        if (visibility == View.VISIBLE) {
            TransitionManager.beginDelayedTransition(parent as ViewGroup?)
            visibility = View.GONE
            addMusicView?.visibility = View.VISIBLE
        }
        music = null
    }

    fun resetMusic() {
        music?.let {
            mTvProgress.text =
                Util.formatSeconds3(0 / 1000) + "/" + Util.formatSeconds3(it.duration.toInt())
        }
    }

    /**
     * 第一步
     */
    fun setAddMusicView(addMusicView: LinearLayout) {
        this.addMusicView = addMusicView
    }

    /**
     * 初始第3步，设置录音控制
     */
    fun setRecordUtil(recordUtils: MixRecordUtils?) {
        this.recordUtils = recordUtils
        recordUtils?.setBackgroundPlayerListener(this)
        mSeekBar.max = volumeConfig.getMaxVoice()
        mSeekBar.progress = volumeConfig.getCurVolume()
        showVolumeString(mSeekBar, recordUtils)
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.let {
                    showVolumeString(it, recordUtils)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    showVolumeString(it, recordUtils)
                }
            }
        })
    }

    private fun showVolumeString(
        it: SeekBar,
        recordUtils: MixRecordUtils?
    ) {
        val fl = it.progress / max
        recordUtils?.setVolume(fl)
        mTvVoice.text = "${(fl * 100).toInt()}%"
    }

    /**
     * 第2步设置背景音乐选择
     */
    fun setDialog(musicDialog: MusicListBottomQSheetDialog) {
        this.musicDialog = musicDialog
    }

    /**
     * isSave 是否保存，默认是保存
     */
    fun setMusic(music: MusicQ, isSave: Boolean = true) {
        if (music.url == null) {
            //如果是错误数据，直接忽略
            return
        }
        if (isSave) {
            this.music?.let {
                ArmsUtils.makeText("已切换背景音乐")
            }
        }
        this.music = music
        mTvName.text = music.name
        mTvProgress.text =
            Util.formatSeconds3(0 / 1000) + "/" + Util.formatSeconds3(music.duration.toInt())
        mSeekBar.progress = volumeConfig.getCurVolume()
        music.startNoPlayMusic()
        if (visibility == View.GONE) {
            TransitionManager.beginDelayedTransition(parent as ViewGroup?)
            visibility = View.VISIBLE
            addMusicView?.visibility = View.GONE
        }
        musicDialog?.dismissBottomSheet()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        volumeConfig.addChangeListener(onVolumeChange)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        volumeConfig.removeChangeListener(onVolumeChange)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_play -> {
                music?.apply {
                    recordUtils?.let {
                        if (it.isRecording) {
                            startPlayMusic()
                        } else {
                            it.startOrPause()
                            startPlayMusic()
                        }
                    }
                }
            }
            R.id.iv_change -> {
                recordUtils?.pause()
                musicDialog?.showBottomSheet()
            }
        }

    }

    private fun startPlayMusic() {
        recordUtils?.startOrPauseBGM()
        recordUtils?.setVolume(mSeekBar.progress / max)
    }

    private fun MusicQ.startNoPlayMusic(): Unit? {
        recordUtils?.setBackGroundUrl(context, url!!)
        return recordUtils?.setVolume(mSeekBar.progress / max)
    }

    override fun onStart( duration: Int) {
        mIvPlay.post {
            mIvPlay.setImageResource(R.drawable.icon_pause_bg_music)
        }
    }


    override fun onPause() {
        mIvPlay.post {
            mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
        }
    }

    override fun onResume() {
        mIvPlay.post {
            mIvPlay.setImageResource(R.drawable.icon_pause_bg_music)
        }
    }

    override fun onStop() {
        mIvPlay.post {
            mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
        }
    }

    override fun onCompletion() {
        mIvPlay.post {
            mIvPlay.setImageResource(R.drawable.icon_play_bg_music)
        }
    }

    override fun onError(throwable: Exception) {

    }

    override fun onProgress(current: Int, duration: Int) {
        mIvPlay.post {
            mTvProgress.text =
                Util.formatSeconds3(current / 1000) + "/" + Util.formatSeconds3(duration / 1000)
        }
    }
}