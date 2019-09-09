package me.shetj.mp3recorder.record.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.adapter.MusicAdapter
import me.shetj.mp3recorder.record.utils.LocalMusicUtils
import me.shetj.player.AudioPlayer
import me.shetj.player.SimPlayerListener
import timber.log.Timber

/**
 * 背景音乐
 */
class MusicListBottomSheetDialog (context: Context) : View.OnClickListener {


    private val easyBottomSheetDialog: BottomSheetDialog?
    private var onItemClickListener: BaseQuickAdapter.OnItemClickListener? = null
    private val audioPlayer: AudioPlayer
    init {
        this.easyBottomSheetDialog = buildBottomSheetDialog(context)
        audioPlayer = AudioPlayer()
    }

    private fun buildBottomSheetDialog(context: Context): BottomSheetDialog {
        val bottomSheetDialog = BottomSheetDialog(context, R.style.transparent_music_style)
        val rootView = LayoutInflater.from(context).inflate(R.layout.dialog_bg_music_list, null)
        bottomSheetDialog.setContentView(rootView)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycler_view)
        rootView.findViewById<View>(R.id.cancel).setOnClickListener(this )

        val musicAdapter = MusicAdapter(ArrayList()).apply {
            setOnItemChildClickListener { _, view, position ->
                when(view.id){
                    R.id.iv_play ->{
                        val music =  getItem(position)
                        music?.let {
                            audioPlayer.playOrPause(it.url!! ,object : SimPlayerListener() {
                                override fun onStart(url: String, duration: Int) {
                                    super.onStart(url, duration)
                                    setPlayPosition(position)
                                }

                                override fun onResume() {
                                    super.onResume()
                                    setPlayPosition(position)
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
        }
        recyclerView.adapter = musicAdapter
        LocalMusicUtils.loadFileData(context)
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe ({
            musicAdapter.setNewData(it)
        },{ Timber.e(it) })
        musicAdapter.setOnItemClickListener { adapter, view, position ->
            musicAdapter.setSelectPosition(position)
            onItemClickListener?.onItemClick(adapter, view, position)
        }
        bottomSheetDialog.setOnCancelListener {
            audioPlayer.stopPlay()
        }
        bottomSheetDialog.setOnDismissListener {
            audioPlayer.stopPlay()
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

    fun setOnItemClickListener(onItemClickListener: BaseQuickAdapter.OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }
}
