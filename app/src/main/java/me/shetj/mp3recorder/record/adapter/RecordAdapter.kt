package me.shetj.mp3recorder.record.adapter


import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.animation.doOnEnd
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.utils.LifecycleListener
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
class RecordAdapter(data: MutableList<Record>?) : BaseQuickAdapter<Record, BaseViewHolder>(R.layout.item_record_view, data),
    LifecycleListener {

    var curPosition = -1
        private set
    private val mediaUtils: MediaPlayerUtils= MediaPlayerUtils()
    private var mCompositeDisposable: CompositeDisposable? = null
    /**
     * 是否是上传中,上传的时候不能点击其他区域，可以返回
     */
    var isUploading = false
        private set

    override fun convert(helper: BaseViewHolder, item: Record?) {
        item?.let {
            val itemPosition = helper.layoutPosition - headerLayoutCount
            val seekBar = helper.getView<SeekBar>(R.id.seekBar_record)
            seekBar.max = item.audioLength * 1000
            seekBar.tag = item.audio_url
            val listener = RecordPlayerListener(helper, mediaUtils)
            val isCurrent: Boolean = mediaUtils.currentUrl == item.audio_url
            if (isCurrent) {
                mediaUtils.updateListener(listener)
            }

            helper.setText(R.id.tv_name, item.audioName)
                .setGone(R.id.rl_record_view2, curPosition != itemPosition)
                .setText(R.id.tv_time_all, Util.formatSeconds3(item.audioLength))
                .setText(R.id.tv_read_time, Util.formatSeconds3(0))
                .setText(R.id.tv_time, Util.formatSeconds2(item.audioLength))
                addChildClickViewIds(R.id.tv_more)

            //播放
            helper.getView<View>(R.id.iv_play).setOnClickListener { playMusic(item.audio_url, listener) }
            //上传
            helper.getView<View>(R.id.tv_upload).setOnClickListener { startUpload(helper, item) }
        }
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
        uploadMusic(item.audio_url, helper.getView(R.id.progressbar_upload), helper.getView(R.id.tv_progress))
    }

    /**
     * 上传
     */
    private fun uploadMusic(audioUrl: String?, progressBar: ProgressBar, tvProgress: TextView) {
        if (!File(audioUrl!!).exists()) {
            ArmsUtils.makeText("当前选中文件已经丢失~，请删除该记录后重新录制！")
            return
        }
        isUploading = true
        weakRecyclerView.get()?.alpha = 0.7f
        val valueAnimator = showAnimator(progressBar, tvProgress, 0, 100, 2500).apply {
            doOnEnd {
                isUploading = false
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
    private fun showAnimator(progressBar: ProgressBar, tvProgress: TextView, start: Int, end: Int, time: Int): ValueAnimator {
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
        if (isUploading) {
            ArmsUtils.makeText("正在上传...")
            return
        }
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
                notifyItemChanged(old + headerLayoutCount)
            }
            if (targetPos != -1) {
                notifyItemChanged(targetPos + headerLayoutCount)
            }
        }
    }


    private fun playMusic(url: String?, listener: PlayerListener) {
        mediaUtils.playOrStop(url!!, listener)
    }

    override fun onStart() {}

    override fun onStop() {
        mediaUtils.stopPlay()
    }

    override fun onResume() {
        mediaUtils.resume()
    }


    override fun onPause() {
        mediaUtils.pause()
    }

    override fun onDestroy() {
        mediaUtils.resume()
        unDispose()
    }

    /**
     * 将 [Disposable] 添加到 [CompositeDisposable] 中统一管理
     * 可在 {onDestroy() 中使用 [.unDispose] 停止正在执行的 RxJava 任务,避免内存泄漏
     *
     * @param disposable
     */
    fun addDispose(disposable: Disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = CompositeDisposable()
        }
        mCompositeDisposable!!.add(disposable)
    }

    /**
     * 停止集合中正在执行的 RxJava 任务
     */
    fun unDispose() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable!!.clear()
        }
    }

}