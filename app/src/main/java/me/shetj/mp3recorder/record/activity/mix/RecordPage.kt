
package me.shetj.mp3recorder.record.activity.mix

import android.transition.Scene
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.shetj.base.ktx.logI
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.databinding.PageRecordMixBinding
import me.shetj.mp3recorder.record.bean.MusicQ
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.bean.RecordDbUtils
import me.shetj.mp3recorder.record.utils.EventCallback
import me.shetj.mp3recorder.record.utils.RecordUtils
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.mp3recorder.record.view.BackgroundMixMusicView
import me.shetj.mp3recorder.record.view.MusicListBottomQSheetDialog
import me.shetj.recorder.core.SimRecordListener
import timber.log.Timber
import java.io.File

/**
 * 录制声音界面
 */
@OptIn(InternalCoroutinesApi::class)
open class RecordPage(
    private val context: AppCompatActivity,
    mRoot: ViewGroup,
    private val callback: EventCallback,
    private val btnRecorderType: AppCompatButton
) : View.OnClickListener {

    private val binding: PageRecordMixBinding = PageRecordMixBinding.inflate(LayoutInflater.from(context))

    private val root: RelativeLayout = binding.root
    private var mProgressBarRecord: ProgressBar? = null
    private var mTvRecordTime: TextView? = null
    private var mTvReRecord: TextView? = null
    private var mTvSaveRecord: TextView? = null
    private var mIvRecordState: ImageView? = null
    private var mTvStateMsg: TextView? = null
    private var oldRecord: Record? = null
    private var isHasChange = false
    val scene: Scene = Scene(mRoot, root as View)
    private var mtvAllTime: TextView? = null
    private var mBunceSv: ScrollView? = null
    private var recordCallBack: SimRecordListener? = null
    private var musicView: BackgroundMixMusicView? = null
    private var addMusic: LinearLayout? = null
    private val musicDialog: MusicListBottomQSheetDialog by lazy {
        MusicListBottomQSheetDialog(context).apply {
            setOnItemClickListener { adapter, _, position ->
                val music = adapter.getItem(position) as MusicQ
                musicView?.setMusic(music)
                addMusic?.visibility = View.GONE
            }
        }
    }


    /**
     * Recording voice
     * 录制声音
     */
    val recordingVoice = MutableSharedFlow<Int>(1)
    var currentDuration = 0L

    private var recordUtils: RecordUtils? = null


    init {
        initView(root)
        initData()
        musicView?.setDialog(musicDialog)
    }

    @InternalCoroutinesApi
    private fun initView(view: View) {
        mBunceSv = view.findViewById(R.id.scrollView)
        mProgressBarRecord = view.findViewById(R.id.progressBar_record)
        mTvRecordTime = view.findViewById(R.id.tv_record_time)
        mTvReRecord = view.findViewById(R.id.tv_reRecord)
        mTvReRecord!!.setOnClickListener(this)
        mTvSaveRecord = view.findViewById(R.id.tv_save_record)
        mTvSaveRecord!!.setOnClickListener(this)
        mIvRecordState = view.findViewById(R.id.iv_record_state)
        mIvRecordState!!.setOnClickListener(this)
        mTvStateMsg = view.findViewById(R.id.tv_state_msg)
        mtvAllTime = view.findViewById(R.id.tv_all_time)
        musicView = view.findViewById(R.id.bg_music_view)
        addMusic = view.findViewById(R.id.ll_add_music)
        addMusic!!.setOnClickListener(this)
    }


    fun clearMusic() {
        musicView?.resetMusic()
    }


    private fun initData() {
        recordCallBack = object : SimRecordListener() {

            override fun onStart() {
                super.onStart()

                TransitionManager.beginDelayedTransition(root)
                isHasChange = true
                mIvRecordState!!.setImageResource(R.mipmap.icon_record_pause_2)
                showSaveAndRe(View.INVISIBLE)
                mTvStateMsg!!.text = "录音中"
                recordingVoice.resetReplayCache()
            }

            override fun onRemind(duration: Long) {
                super.onRemind(duration)
                "这是onRemind ：${Util.formatSeconds3((duration / 1000).toInt())}".logI()

            }

            override fun onRecording(time: Long, volume: Int) {
                super.onRecording(time, volume)
                mProgressBarRecord!!.progress = time.toInt()/1000
                mTvRecordTime!!.text = Util.formatSeconds3(time.toInt()/1000)
                currentDuration = time
                if (volume > 0) {
                    recordingVoice.tryEmit(volume)
                }
            }

            override fun onPause() {
                TransitionManager.beginDelayedTransition(root)
                mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                showSaveAndRe(View.VISIBLE)
                mTvStateMsg!!.text = "已暂停，点击继续"
            }

            override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
                Timber.i("onSuccess")
                if (isAutoComplete) {
                    if (File(file).exists()) {
                        TransitionManager.beginDelayedTransition(root)
                        mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                        showSaveAndRe(View.INVISIBLE)
                        mTvStateMsg!!.text = "录制完成"
                        if (oldRecord == null) {
                            saveRecord(file)
                            showRecordNewDialog()
                        } else {
                            saveOldRecord(file, false)
                        }
                    }
                } else {
                    if (File(file).exists()) {
                        TransitionManager.beginDelayedTransition(root)
                        mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                        showSaveAndRe(View.INVISIBLE)
                        mTvStateMsg!!.text = "点击录音"
                        if (oldRecord == null) {
                            saveRecord(file)
                            callback.onEvent(1)
                        } else {
                            saveOldRecord(file, true)
                        }
                    }
                }
            }

            override fun onMaxChange(time: Long) {
                mProgressBarRecord!!.max = time.toInt()
                mtvAllTime!!.text = Util.formatSeconds3(time.toInt())
            }

            override fun onError(e: Exception) {
                mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                setRecord(oldRecord)
            }


            override fun needPermission() {
                callback.onEvent(3)
            }
        }

        recordUtils = RecordUtils(recordCallBack)
        musicView?.setRecordUtil(recordUtils)
        musicView!!.setAddMusicView(addMusic!!)//要先设置“添加控件”，因为后面又隐藏
        musicView!!.setDialog(musicDialog)
        recordUtils!!.recorderLiveDate.observe(context) {
            btnRecorderType.text = recordUtils!!.getRecorderTypeName()
        }
        btnRecorderType.setOnClickListener {
            recordUtils!!.showChangeDialog(context)
        }
    }

    /**
     * 保持录音
     *
     * @param file     文件地址
     * @param isFinish true 保持后切换界面，false 展示是否录制下一个界面
     */
    private fun saveOldRecord(file: String, isFinish: Boolean) {
        context.lifecycleScope.launch {
            kotlin.runCatching {
                val mediaTime = getMediaTime(file)
                if (oldRecord != null && isHasChange) {
                    oldRecord!!.audioLength = mediaTime
                    RecordDbUtils.getInstance().update(oldRecord!!)
                }
                if (isFinish) {
                    callback.onEvent(1)
                } else {
                    showRecordNewDialog()
                }
            }.onFailure {
                callback.onEvent(1)
            }
        }
    }

    /**
     * 是否录制新内容
     */
    private fun showRecordNewDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle("录音已保存")
            .setMessage("录音已保存。是否继续录制下一条？")
            .setNegativeButton("查看本条"){
                _, _ -> callback.onEvent(1)
            }
            .setPositiveButton("录下一条"){ _, _ ->
                setRecord(null)
                ArmsUtils.makeText("上条录音已保存至“我的录音”")
            }
            .show()
    }


    //得到media的时间长度
    private suspend fun getMediaTime(file: String): Int {
        return withContext(Dispatchers.IO) {
            Util.getAudioLength(file)
        }
    }


    /**
     * 保存录音，并且通知修改
     */
    private fun saveRecord(file: String) {
        context.lifecycleScope.launch {
            kotlin.runCatching {
                val record = Record(
                    "1", file, System.currentTimeMillis().toString() + "",
                    Util.getAudioLength(file), ""
                )
                RecordDbUtils.getInstance().save(record)
            }
        }

    }

    /**
     * 设置是否oldRecord,没有点击录音，有点击继续
     */
    fun setRecord(record: Record?) {
        if (record != null) {
            oldRecord = record
            recordUtils!!.setTime(oldRecord!!.audioLength.toLong() * 1000)
            mTvStateMsg!!.text = "点击继续"
        } else {
            oldRecord = null
            recordUtils!!.setTime(0)
            mTvStateMsg!!.text = "点击录音"
        }
        showSaveAndRe(View.INVISIBLE)
    }

    /**
     * 展示重新录制和保持
     */
    private fun showSaveAndRe(invisible: Int) {
        mTvSaveRecord!!.visibility = invisible
        mTvReRecord!!.visibility = invisible
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_reRecord -> showRerecordDialog()
            R.id.tv_save_record -> recordUtils!!.stopFullRecord()
            R.id.iv_record_state -> recordUtils!!.startOrPause(oldRecord?.audio_url ?: "")
            R.id.ll_add_music -> showMusicDialog()
            else -> {
            }
        }
    }

    private fun showMusicDialog() {
        musicDialog.showBottomSheet()
    }

    /**
     * 展示重新录制
     */
    private fun showRerecordDialog() {
        MaterialAlertDialogBuilder(context)
            .setTitle("重新录制")
            .setMessage("确定删除当前的录音，并重新录制吗？")
            .setNegativeButton("取消") { _, _ -> recordUtils!!.stopFullRecord() }
            .setPositiveButton("重录") { _, _ ->
                //可自行判断是否删除老的文件
                oldRecord?.audioLength = 0
                oldRecord?.audio_url = ""
                setRecord(oldRecord)
                recordUtils!!.reset()
                recordUtils!!.setTime(0)
            }
            .show()
    }

    /**
     * 展示中途推出,
     */
    private fun showTipDialog() {
        onPause()//先暂停
        MaterialAlertDialogBuilder(context)
            .setTitle("温馨提示")
            .setMessage("确定要停止录音吗？")
            .setNegativeButton("停止录音") { _, _ -> recordUtils!!.stopFullRecord() }
            .setPositiveButton("继续录音") { _, _ -> recordUtils!!.startOrPause() }
            .show()
    }

    fun onStop() {
        context.lifecycleScope.launch {
            if (recordUtils != null) {
                //如果在正录音中，要提醒用户是否停止录音
                if (recordUtils!!.isRecording) {
                    showTipDialog()
                } else {
                    if (recordUtils!!.hasRecord()) {
                        //如果录制了，默认是完成录制
                        recordUtils!!.stopFullRecord()
                    } else {
                        //如果没有，但是存在老的录音，保持一次文字
                        if (oldRecord != null) {
                            RecordDbUtils.getInstance().update(oldRecord!!)
                        }
                        callback.onEvent(1)
                    }
                }
            }
        }
    }

    protected fun onPause() {
        if (recordUtils != null) {
            recordUtils!!.pause()
        }
    }

    fun onDestroy() {
        Timber.i("onDestroy")
        recordUtils?.clear()
    }

}
