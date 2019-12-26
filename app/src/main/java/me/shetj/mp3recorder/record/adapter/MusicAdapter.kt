package me.shetj.mp3recorder.record.adapter

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.utils.Util

class MusicAdapter(date: ArrayList<Music>) :BaseQuickAdapter<Music,BaseViewHolder>(R.layout.item_music_view,date){

    private var curPosition = -1

    private var playPosition = -1 //播放的谁

    override fun convert(helper: BaseViewHolder, item: Music) {
        item?.apply {
            helper?.setText(R.id.tv_name,name)
                    ?.setText(R.id.tv_time, Util.formatSeconds3((duration/1000).toInt()))
                    ?.addOnClickListener(R.id.iv_play)
                    ?.setImageResource(R.id.iv_play,when(helper.adapterPosition == playPosition){
                        false -> R.drawable.icon_record_bg_music
                        true ->  R.drawable.icon_record_bg_music_pause
                    })
            helper?.getView<View>(R.id.checkbox)?.isSelected = helper?.adapterPosition == curPosition
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