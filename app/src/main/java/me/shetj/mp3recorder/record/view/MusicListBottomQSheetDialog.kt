package me.shetj.mp3recorder.record.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.adapter.MusicQAdapter
import me.shetj.mp3recorder.record.utils.LocalMusicQUtils
import timber.log.Timber

/**
 * 背景音乐
 */
class MusicListBottomQSheetDialog (context: Context) : View.OnClickListener {


    private val easyBottomSheetDialog: BottomSheetDialog?
    private var onItemClickListener: OnItemClickListener? = null
    init {
        this.easyBottomSheetDialog = buildBottomSheetDialog(context)
    }

    private fun buildBottomSheetDialog(context: Context): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.transparent_music_style)
        val rootView = LayoutInflater.from(context).inflate(R.layout.dialog_bg_music_list, null)
        bottomSheetDialog.setContentView(rootView)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view)
        rootView.findViewById<View>(R.id.cancel).setOnClickListener(this )

        val musicAdapter = MusicQAdapter(ArrayList()).apply {
            recyclerView.adapter = this
            setEmptyView(R.layout.base_empty_date_view)
        }
        LocalMusicQUtils.loadFileData(context)
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe ({
            musicAdapter.setNewInstance(it.toMutableList())
        },{ Timber.e(it) })
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
