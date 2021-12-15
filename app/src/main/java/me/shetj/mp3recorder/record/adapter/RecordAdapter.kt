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
package me.shetj.mp3recorder.record.adapter


import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.animation.doOnEnd
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.utils.MediaPlayerUtils
import me.shetj.mp3recorder.record.utils.RecordPlayerListener
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.PlayerListener
import java.io.File


/**
 * 录音列表
 * 上传动画描述：
 * 1.首先是获取token 时，使用一闪一闪动画 showAlphaAnimator
 * 2.获取到token的时候，上传先执行一个1.5秒的进度动画（0~40）
 * 3.如果在1.5秒执行成功，就在执行一个平滑动画（progress-100）
 * 4.如果1.5后还么有上传成功，就回调变化进度
 * 5.上传成功后重置
 * @author shetj
 */
class RecordAdapter(data: MutableList<Record>?) :
    BaseQuickAdapter<Record, BaseViewHolder>(R.layout.item_record_view, data) {

    var curPosition = -1
        private set
    private val mediaUtils: MediaPlayerUtils = MediaPlayerUtils()
    private var mCompositeDisposable: CompositeDisposable? = null

    override fun convert(holder: BaseViewHolder, item: Record) {
        item.let {
            val itemPosition = holder.layoutPosition - headerLayoutCount
            val seekBar = holder.getView<SeekBar>(R.id.seekBar_record)
            seekBar.max = item.audioLength * 1000
            seekBar.tag = item.audio_url
            holder.getView<ImageView>(R.id.iv_play).apply {
                if (tag == null) {
                    tag = RecordPlayerListener(holder, mediaUtils)
                }
                setOnClickListener {
                    playMusic(item.audio_url, tag as RecordPlayerListener)
                    mediaUtils.setSeekToPlay(seekBar.progress)
                }
            }
            holder.setText(R.id.tv_name, item.audioName)
                .setGone(R.id.rl_record_view2, curPosition != itemPosition)
                .setText(R.id.tv_time_all, Util.formatSeconds3(item.audioLength))
                .setText(R.id.tv_read_time, Util.formatSeconds3(0))
                .setText(R.id.tv_time, Util.formatSeconds2(item.audioLength))
            holder.getView<View>(R.id.tv_upload).setOnClickListener { startUpload(holder, item) }
        }
    }

    override fun convert(holder: BaseViewHolder, item: Record, payloads: List<Any>) {
        val itemPosition = holder.layoutPosition - headerLayoutCount
        super.convert(holder, item, payloads)
        holder.setText(R.id.tv_name, item.audioName)
            .setGone(R.id.rl_record_view2, curPosition != itemPosition)
    }

    /**
     * 把界面收起来，停止播放音乐，开始上传
     */
    private fun startUpload(helper: BaseViewHolder, item: Record) {
        helper.setGone(R.id.rl_record_view2, false)
            .setVisible(R.id.tv_time, true)
        curPosition = -1
        if (!mediaUtils.isPause) {
            mediaUtils.pause()
        }
        uploadMusic(
            item.audio_url,
            helper.getView(R.id.progressbar_upload),
            helper.getView(R.id.tv_progress)
        )
    }

    /**
     * 上传
     */
    private fun uploadMusic(audioUrl: String?, progressBar: ProgressBar, tvProgress: TextView) {
        if (!File(audioUrl!!).exists()) {
            ArmsUtils.makeText("当前选中文件已经丢失~，请删除该记录后重新录制！")
            return
        }
        recyclerView.alpha = 0.7f
        val valueAnimator = showAnimator(progressBar, tvProgress, 0, 100, 2500).apply {
            doOnEnd {
                progressBar.progress = 0
                progressBar.alpha = 0f
                tvProgress.text = ""
            }
        }
        progressBar.alpha = 1f
        //开始执行进度动画
        valueAnimator.start()
    }

    /**
     * 展示进度动画
     */
    private fun showAnimator(
        progressBar: ProgressBar,
        tvProgress: TextView,
        start: Int,
        end: Int,
        time: Int
    ): ValueAnimator {
        val valueAnimator = ValueAnimator.ofInt(start, end)
        valueAnimator.duration = time.toLong()
        valueAnimator.interpolator = AccelerateDecelerateInterpolator()
        valueAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            progressBar.progress = animatedValue
            progressBar.visibility = View.VISIBLE
            tvProgress.visibility = View.VISIBLE
            tvProgress.text = String.format("%s%%", animatedValue.toString())
        }
        return valueAnimator
    }


    /**
     * 设置选中的位置
     */
    fun setPlayPosition(targetPos: Int) {
        //停止音乐
        if (targetPos == -1 || curPosition != targetPos) {
            if (!mediaUtils.isPause) {
                mediaUtils.pause()
            }
        }
        //如果不相等，说明有变化
        if (curPosition != targetPos) {
            val old = curPosition
            this.curPosition = targetPos
            // -1 表示默认不做任何变化
            if (old != -1) {
                notifyItemChanged(old + headerLayoutCount, 1)
            }
            if (targetPos != -1) {
                notifyItemChanged(targetPos + headerLayoutCount, 1)
            }
        }
    }


    private fun playMusic(url: String?, listener: PlayerListener) {
        mediaUtils.playOrStop(url!!, listener)
    }


    fun onPause() {
        mediaUtils.pause()
    }

    fun onDestroy() {
        mediaUtils.stopPlay()
        unDispose()
    }

    fun unDispose() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable!!.clear()
        }
    }

}