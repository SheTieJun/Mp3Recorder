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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.shetj.base.ktx.launch
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.bean.RecordDbUtils
import me.shetj.mp3recorder.record.utils.EventCallback


/**
 * 录音更多菜单
 */
class RecordBottomSheetDialog(
    val context: AppCompatActivity,
    position: Int,
    private val record: Record,
    private val callback: EventCallback
) : View.OnClickListener {
    private val easyBottomSheetDialog: BottomSheetDialog?
    private var position = -1

    init {
        this.position = position
        this.easyBottomSheetDialog = buildBottomSheetDialog(context)
    }

    private fun buildBottomSheetDialog(context: Context): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context)
        val rootView = LayoutInflater.from(context).inflate(R.layout.dailog_record_case, null)
        bottomSheetDialog.setContentView(rootView)
        rootView.findViewById<View>(R.id.tv_record).setOnClickListener(this)
        rootView.findViewById<View>(R.id.tv_edit_name).setOnClickListener(this)
        rootView.findViewById<View>(R.id.tv_del).setOnClickListener(this)
        rootView.findViewById<View>(R.id.tv_cancel).setOnClickListener(this)

        //对于时间已经大于60 分钟的 不显示继续录制
        rootView.findViewById<View>(R.id.tv_record).visibility =
            if (record.audioLength > 3599) View.GONE else View.VISIBLE
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
            R.id.tv_record -> {
                callback.onEvent(2)
                dismissBottomSheet()
            }
            R.id.tv_del -> {
                context.launch {
                    RecordDbUtils.getInstance().del(record)
                    dismissBottomSheet()
                }
            }
            R.id.tv_cancel -> dismissBottomSheet()
            R.id.tv_edit_name -> dismissBottomSheet()
            else -> {
            }
        }
    }


}
