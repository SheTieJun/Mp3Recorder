
package me.shetj.mp3recorder.record.adapter


import android.widget.ImageView
import android.widget.SeekBar
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.utils.MediaPlayerUtils
import me.shetj.mp3recorder.record.utils.RecordPlayerListener
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.PlayerListener


class RecordAdapter(data: MutableList<Record>?) :
    BaseQuickAdapter<Record, BaseViewHolder>(R.layout.item_record_view, data) {

    var curPosition = -1
        private set
    private val mediaUtils: MediaPlayerUtils = MediaPlayerUtils()

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
        }
    }

    override fun convert(holder: BaseViewHolder, item: Record, payloads: List<Any>) {
        val itemPosition = holder.layoutPosition - headerLayoutCount
        super.convert(holder, item, payloads)
        holder.setText(R.id.tv_name, item.audioName)
            .setGone(R.id.rl_record_view2, curPosition != itemPosition)
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
    }

}