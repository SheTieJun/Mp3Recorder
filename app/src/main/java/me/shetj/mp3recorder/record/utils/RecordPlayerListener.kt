package me.shetj.mp3recorder.record.utils

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import me.shetj.mp3recorder.R
import timber.log.Timber


class RecordPlayerListener(private val helper: BaseViewHolder, private val mediaUtils: MediaPlayerUtils) : SPlayerListener {
    private val seekBar: SeekBar = helper.getView<SeekBar>(R.id.seekBar_record).apply{
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                mediaUtils.stopProgress()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (canChange) {
                    mediaUtils.seekTo(seekBar.progress)
                    if (!mediaUtils.isPause) {
                        mediaUtils.startProgress()
                    }
                }
            }
        })
    }

    init {
        if (canChange) {
            seekBar.progress =  mediaUtils.getCurrentPosition()
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

    override val isLoop: Boolean
        get() = false


    override fun onStart(url: String) {
        if (canChange) {
            statePlaying(true)
        }
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

    override fun onError(throwable: Throwable) {
        Timber.e("ClassroomPlayerListener ${throwable.message}")
    }


    override fun isNext(mp: MediaPlayerUtils): Boolean {
        return false
    }

    override fun onProgress(current: Int, size: Int) {
        if (canChange) {
            if (!mediaUtils.isPause){
                statePlaying()
            }
            if (current != size) {
                seekBar.progress = current
            }
        }
    }


    private fun statePlaying(isShow: Boolean = false) {
        if(helper.getView<View>(R.id.rl_record_view2).visibility == View.GONE && isShow) {
            TransitionManager.beginDelayedTransition(helper.itemView as ViewGroup)
        }
        helper.setGone(R.id.rl_record_view2, false)
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_pause)
    }

    private fun statePause() {
        helper.setGone(R.id.rl_record_view2, false)
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_play)
    }

    private fun stateStop() {
        seekBar.progress = 0
        helper.setGone(R.id.rl_record_view2, true)
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_play)
    }

}
