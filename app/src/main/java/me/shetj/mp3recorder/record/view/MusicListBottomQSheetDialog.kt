
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
