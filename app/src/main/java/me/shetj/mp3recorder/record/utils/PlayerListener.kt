package me.shetj.mp3recorder.record.utils

import android.content.Context
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.chad.library.adapter.base.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.player.AudioPlayer
import me.shetj.player.SimPlayerListener

open class PlayerListener(private val mContext: Context, private val helper: BaseViewHolder, private val mediaUtils: AudioPlayer) :
    SimPlayerListener() {
    private val seekBar: SeekBar = helper.getView<SeekBar>(R.id.seekBar).apply{
        helper.getView<View>(R.id.content)?.setOnClickListener { mediaUtils.playOrPause(tag.toString(), this@PlayerListener) }
         setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mediaUtils.stopProgress()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (mediaUtils.currentUrl == seekBar.tag.toString()) {
                    mediaUtils.seekTo(seekBar.progress)
                    if (!mediaUtils.isPause){
                        mediaUtils.startProgress()
                    }
                }
            }
        })
    }
    private val msgVoiceStatus:View = helper.getView(R.id.state_thumb)

    init {
        if (canChange) {
            seekBar.progress =  mediaUtils.currentPosition
            statePause()
        }else{
            stateStop()
        }
    }

    private val canChange :Boolean
        get() {
            return mediaUtils.currentUrl == seekBar.tag.toString()
        }

    override fun onPause() {
        if (canChange) {
            statePause()
        }
    }

    override fun onStart(url: String, duration: Int) {
        super.onStart(url, duration)
    }

    override fun onResume() {
        if (canChange) {
            statePlaying()
        }
    }

    override fun onStop() {
        if (canChange) {
            seekBar.progress = 0
            stateStop()
        }
    }

    override fun onCompletion() {
        seekBar.progress = 0
        stateStop()
    }

    override fun onError(throwable: Exception) {
        super.onError(throwable)
    }

    override fun onProgress(current: Int, size: Int) {
        if (canChange) {
            if (!seekBar.isEnabled){
                statePlaying()
            }
            if (current != size) {
                seekBar.progress = current
            }
        }
    }

    private fun statePlaying() {

    }

    private fun statePause() {

    }

    private fun stateStop() {

    }
}
