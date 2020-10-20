package me.shetj.mp3recorder.record.adapter

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.bean.MusicQ
import me.shetj.mp3recorder.record.utils.Util

class MusicQAdapter(date: ArrayList<MusicQ>) :BaseQuickAdapter<MusicQ, BaseViewHolder>(R.layout.item_music_view,date){

    private var curPosition = -1

    private var playPosition = -1 //播放的谁

    override fun convert(holder: BaseViewHolder, item: MusicQ) {
        item.apply {
            holder.setText(R.id.tv_name,name)
                .setText(R.id.tv_time, Util.formatSeconds3((duration/1000).toInt()))
                .setImageResource(R.id.iv_play,when(holder.layoutPosition == playPosition){
                    false -> R.drawable.icon_record_bg_music
                    true ->  R.drawable.icon_record_bg_music_pause
                })
            holder.getView<View>(R.id.checkbox).isSelected = holder.layoutPosition == curPosition
            addChildClickViewIds(R.id.iv_play)
        }
    }

    fun setSelectPosition(position: Int) {
        this.curPosition = position
        notifyDataSetChanged()
    }

    fun setPlayPosition(position: Int){
        this.playPosition = position
        notifyDataSetChanged()
    }
}