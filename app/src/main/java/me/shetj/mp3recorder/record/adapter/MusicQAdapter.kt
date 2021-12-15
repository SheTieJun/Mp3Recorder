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

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.MusicQ
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.player.AudioPlayer
import me.shetj.player.SimPlayerListener

class MusicQAdapter(date: ArrayList<MusicQ>) :BaseQuickAdapter<MusicQ, BaseViewHolder>(R.layout.item_music_view,date){

    private var curPosition = -1

    private var playPosition = -1 //播放的谁
    private val audioPlayer = AudioPlayer()

    override fun convert(holder: BaseViewHolder, item: MusicQ) {
        item.apply {
            holder.setText(R.id.tv_name,name)
                .setText(R.id.tv_time, Util.formatSeconds3((duration/1000).toInt()))
                .setImageResource(R.id.iv_play,when(holder.layoutPosition == playPosition){
                    false -> R.drawable.icon_record_bg_music
                    true ->  R.drawable.icon_record_bg_music_pause
                })
            holder.getView<View>(R.id.checkbox).isSelected = holder.layoutPosition == curPosition
            holder.getView<View>(R.id.iv_play).setOnClickListener {
                item.let {
                    audioPlayer.playOrPause(context,it.url!!,null,object : SimPlayerListener() {
                        override fun onStart( duration: Int) {
                            super.onStart(duration)
                            setPlayPosition(holder.layoutPosition)
                        }

                        override fun onResume() {
                            super.onResume()
                            setPlayPosition(holder.layoutPosition)
                        }

                        override fun onPause() {
                            super.onPause()
                            setPlayPosition(-1)
                        }

                        override fun onCompletion() {
                            super.onCompletion()
                            setPlayPosition(-1)
                        }

                        override fun onStop() {
                            super.onStop()
                            setPlayPosition(-1)
                        }
                    })
                }
            }
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

    fun stopPlay() {
        audioPlayer.stopPlay()
    }
}