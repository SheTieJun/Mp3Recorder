package me.shetj.mp3recorder.record.utils

import android.util.Log
import android.widget.SeekBar

import com.chad.library.adapter.base.BaseViewHolder
import me.shetj.mp3recorder.R

import me.shetj.mp3recorder.record.utils.MediaPlayerUtils
import me.shetj.mp3recorder.record.utils.SPlayerListener
import me.shetj.mp3recorder.record.utils.Util


/**
 * **@packageName：** com.shetj.diyalbume.pipiti.utils<br></br>
 * **@author：** shetj<br></br>
 * **@createTime：** 2018/10/24 0024<br></br>
 * **@company：**<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**<br></br>
 */
class SimPlayerListener(private val helper: BaseViewHolder) : SPlayerListener {
    private val seekBar: SeekBar

    override val isLoop: Boolean
        get() = false

    init {
        this.seekBar = helper.getView(R.id.seekBar_record)
    }

    override fun onPause() {
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_play)
    }

    override fun onStart(url: String) {
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_pause)
    }

    override fun onResume() {
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_pause)
    }

    override fun onStop() {
        seekBar.progress = 0
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_play)
                .setText(R.id.tv_read_time, Util.formatSeconds3(0))
    }

    override fun onCompletion() {
        seekBar.progress = 0
        helper.setText(R.id.tv_read_time, Util.formatSeconds3(0))
        helper.setImageResource(R.id.iv_play, R.drawable.selector_weike_record_play)
    }

    override fun onError(throwable: Throwable) {
        Log.e("SimPlayerListener", throwable.message)
    }

    override fun isNext(mp: MediaPlayerUtils): Boolean {
        return false
    }

    override fun onProgress(current: Int, size: Int) {
        if (current != size) {
            seekBar.progress = current
            helper.setText(R.id.tv_read_time, Util.formatSeconds3(current / 1000))
        }
    }

}
