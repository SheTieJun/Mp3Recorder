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
package me.shetj.mp3recorder.record.view

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.shetj.base.ktx.launch
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.adapter.MusicQAdapter
import me.shetj.mp3recorder.record.utils.LocalMusicQUtils

/**
 * 背景音乐
 */
class MusicListBottomQSheetDialog (val context: AppCompatActivity) : View.OnClickListener {


    private val easyBottomSheetDialog: BottomSheetDialog?
    private var onItemClickListener: OnItemClickListener? = null
    init {
        this.easyBottomSheetDialog = buildBottomSheetDialog(context)
    }

    private fun buildBottomSheetDialog(context: AppCompatActivity): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.transparent_music_style)
        val rootView = LayoutInflater.from(context).inflate(R.layout.dialog_bg_music_list, null)
        bottomSheetDialog.setContentView(rootView)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view)
        rootView.findViewById<View>(R.id.cancel).setOnClickListener(this )

        val musicAdapter = MusicQAdapter(ArrayList()).apply {
            recyclerView.adapter = this
            setEmptyView(R.layout.base_empty_date_view)
        }
        context.launch {
            LocalMusicQUtils.loadFileData(context).collect{
                musicAdapter.setNewInstance(it.toMutableList())
            }
        }
        musicAdapter.setOnItemClickListener { adapter, view, position ->
            musicAdapter.setSelectPosition(position)
            onItemClickListener?.onItemClick(adapter, view, position)
        }
        bottomSheetDialog.setOnCancelListener {
            musicAdapter.stopPlay()
        }
        bottomSheetDialog.setOnDismissListener {
            musicAdapter.stopPlay()
        }
        return bottomSheetDialog
    }

    fun showBottomSheet() {
        easyBottomSheetDialog?.show()
    }

    fun dismissBottomSheet() {
        easyBottomSheetDialog?.dismiss()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.cancel -> dismissBottomSheet()
        }
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }
}
