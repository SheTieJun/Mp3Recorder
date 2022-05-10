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
package me.shetj.mp3recorder.record.utils

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.player.PlayerListener
import timber.log.Timber


class RecordPlayerListener(
    private val helper: BaseViewHolder,
    private val mediaUtils: MediaPlayerUtils
) : PlayerListener {
    private val seekBar: SeekBar = helper.getView<SeekBar>(R.id.seekBar_record).apply {
        mediaUtils.setSeekBar(this)
    }

    init {
        if (canChange) {
            seekBar.progress = mediaUtils.getCurrentPosition()
            statePause()
        } else {
            stateStop()
        }
    }

    private val canChange: Boolean
        get() {
            return mediaUtils.currentUrl == seekBar.tag.toString()
        }

    override fun onPause() {
        if (canChange) {
            statePause()
        }
    }


    override fun onStart(duration: Int) {
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

    override fun onError(throwable: Exception?) {
        Timber.e("ClassroomPlayerListener ${throwable?.message}")
    }


    override fun onProgress(current: Int, duration: Int) {
        if (canChange) {
            if (!mediaUtils.isPause) {
                statePlaying()
            }
            if (current != duration) {
                seekBar.progress = current
                helper.setText(R.id.tv_read_time, Util.formatSeconds3(current/1000))
            }
        }
    }


    private fun statePlaying(isShow: Boolean = false) {
        if (helper.getView<View>(R.id.rl_record_view2).visibility == View.GONE && isShow) {
            TransitionManager.beginDelayedTransition(helper.itemView as ViewGroup)
        }
        helper.setGone(R.id.rl_record_view2, false)
        helper.setImageResource(R.id.iv_play, R.drawable.selector_record_pause)
    }

    private fun statePause() {
        helper.setGone(R.id.rl_record_view2, false)
        helper.setImageResource(R.id.iv_play, R.drawable.selector_record_play)
    }

    private fun stateStop() {
        seekBar.progress = 0
        helper.setGone(R.id.rl_record_view2, true)
        helper.setImageResource(R.id.iv_play, R.drawable.selector_record_play)
    }

}
