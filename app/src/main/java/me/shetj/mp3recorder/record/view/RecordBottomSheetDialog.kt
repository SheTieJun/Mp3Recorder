
package me.shetj.mp3recorder.record.view

import android.annotation.SuppressLint
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
import me.shetj.recorder.core.FileUtils


/**
 * 录音更多菜单
 */
class RecordBottomSheetDialog(
    private val context: AppCompatActivity,
    private val record: Record,
    private val callback: EventCallback
) : View.OnClickListener {
    private val easyBottomSheetDialog: BottomSheetDialog?

    init {
        this.easyBottomSheetDialog = buildBottomSheetDialog(context)
    }

    @SuppressLint("InflateParams")
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
                    record.audio_url?.let { FileUtils.deleteFile(it) }
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
