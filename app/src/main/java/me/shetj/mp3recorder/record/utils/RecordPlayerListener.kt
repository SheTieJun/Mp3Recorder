package me.shetj.mp3recorder.record.utils

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.player.PlayerListener
import timber.log.Timber


class RecordPlayerListener(private val helper: BaseViewHolder, private val mediaUtils: MediaPlayerUtils) : PlayerListener {
    private val seekBar: SeekBar = helper.getView<SeekBar>(R.id.seekBar_record).apply{
        mediaUtils.setSeekBar(this)
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



    override fun onStart(url: String, duration: Int) {
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

    override fun onError(throwable: Exception) {
        Timber.e("ClassroomPlayerListener ${throwable.message}")
    }


    override fun onProgress(current: Int, duration: Int) {
        if (canChange) {
            if (!mediaUtils.isPause){
                statePlaying()
            }
            if (current != duration) {
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
