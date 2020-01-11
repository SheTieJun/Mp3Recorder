package me.shetj.mp3recorder.record.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.bottomsheet.BottomSheetDialog
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.bean.RecordDbUtils
import me.shetj.mp3recorder.record.utils.Callback
import me.shetj.mp3recorder.record.utils.MainThreadEvent
import org.simple.eventbus.EventBus


/**
 * 录音更多菜单
 */
class RecordBottomSheetDialog(private val context: Context, position: Int, private val record: Record, private val baseViewHolder: BaseViewHolder, private val callback: Callback) : View.OnClickListener {
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
        rootView.findViewById<View>(R.id.tv_record).visibility = if (record.audioLength > 3599) View.GONE else View.VISIBLE
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
                RecordDbUtils.getInstance().del(record)
                EventBus.getDefault().post(MainThreadEvent(MainThreadEvent.RECORD_REFRESH_DEL, position))
                dismissBottomSheet()
            }
            R.id.tv_cancel -> dismissBottomSheet()
            R.id.tv_edit_name -> dismissBottomSheet()
            else -> {
            }
        }
    }


}
