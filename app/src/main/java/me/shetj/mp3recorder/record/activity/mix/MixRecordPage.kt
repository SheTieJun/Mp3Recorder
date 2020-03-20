package me.shetj.mp3recorder.record.activity.mix

import android.graphics.Color
import android.transition.Scene
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.listener.OnItemClickListener
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.bean.RecordDbUtils
import me.shetj.mp3recorder.record.utils.*
import me.shetj.mp3recorder.record.view.BackgroundMixMusicView
import me.shetj.mp3recorder.record.view.MusicListBottomSheetDialog
import org.simple.eventbus.EventBus
import org.simple.eventbus.Subscriber
import org.simple.eventbus.ThreadMode
import timber.log.Timber
import java.io.File

/**
 * 录制声音界面
 */
open class MixRecordPage(private val context: AppCompatActivity, mRoot: ViewGroup, private val callback: Callback) : View.OnClickListener {

    private val root: RelativeLayout = LayoutInflater.from(context).inflate(R.layout.page_record_mix, null) as RelativeLayout
    private var mEditInfo: EditText? = null
    private var mProgressBarRecord: ProgressBar? = null
    private var mTvRecordTime: TextView? = null
    private var mTvReRecord: TextView? = null
    private var mTvSaveRecord: TextView? = null
    private var mIvRecordState: ImageView? = null
    private var mTvStateMsg: TextView? = null
    private var oldRecord: Record? = null
    private var isHasChange = false
    val scene: Scene
    private var mtvAllTime: TextView? = null
    private var mBunceSv: ScrollView? = null
    private var recordCallBack: RecordCallBack? = null
    private var musicView: BackgroundMixMusicView?=null
    private var addMusic :LinearLayout ?=null
    private val musicDialog: MusicListBottomSheetDialog by lazy {
        MusicListBottomSheetDialog(context).apply {
            setOnItemClickListener( OnItemClickListener { adapter, _, position ->
                val music = adapter.getItem(position) as Music
                musicView?.setMusic(music)
                addMusic?.visibility = View.GONE
            })
        }
    }
    private var recordUtils:MixRecordUtils?=null


    init {
        scene = Scene(mRoot, root as View)
        initView(root)
        initData()
        musicView?.setDialog(musicDialog)
    }

    private fun initView(view: View) {
        mBunceSv = view.findViewById(R.id.scrollView)
        mEditInfo = buildEditTextView()
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
        addMusic  = view.findViewById(R.id.ll_add_music)
        addMusic!!.setOnClickListener(this)
    }

    @Subscriber(mode = ThreadMode.MAIN)
    fun refreshData(event: MainThreadEvent<*>) {
        when (event.type) {
            MainThreadEvent.REMOVE_MUSIC -> {
                musicView?.removeMusic()
                addMusic?.visibility = View.VISIBLE
            }
        }
    }
    fun clearMusic() {
        musicView?.resetMusic()
    }

    private fun buildEditTextView(): EditText {
        mBunceSv!!.removeAllViews()
        val editText = EditText(this.context)
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        editText.layoutParams = params
        editText.minHeight = ArmsUtils.dip2px(500f)
        editText.textSize = 16f
        editText.gravity = Gravity.TOP
        editText.setBackgroundColor(Color.WHITE)
        editText.setPadding(ArmsUtils.dip2px(30f), ArmsUtils.dip2px(27f), ArmsUtils.dip2px(30f), ArmsUtils.dip2px(50f))
        editText.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        editText.setHorizontallyScrolling(false)
        editText.maxLines = Integer.MAX_VALUE
        try {
            // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
            val cursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            cursorDrawableRes.isAccessible = true
        } catch (ignored: Exception) {
        }

        mBunceSv!!.addView(editText)
        return editText
    }

    private fun initData() {
        recordCallBack = object : RecordCallBack {
            override fun start() {
                TransitionManager.beginDelayedTransition(root)
                isHasChange = true
                mIvRecordState!!.setImageResource(R.mipmap.icon_record_pause_2)
                showSaveAndRe(View.INVISIBLE)
                mTvStateMsg!!.text = "录音中"

            }

            /**
             * 如果要写呼吸灯，就在这里处理
             * @param time
             * @param volume
             */
            override fun onRecording(time: Int, volume: Int) {
                Timber.i( "time = $time\nvolume$volume")
                AndroidSchedulers.mainThread().scheduleDirect {
                    mProgressBarRecord!!.progress = time
                    mTvRecordTime!!.text = Util.formatSeconds3(time)
                }
            }

            override fun pause() {
                TransitionManager.beginDelayedTransition(root)
                mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                showSaveAndRe(View.VISIBLE)
                mTvStateMsg!!.text = "已暂停，点击继续"
            }

            override fun onSuccess(file: String, time: Int) {
                if (File(file).exists()) {
                    TransitionManager.beginDelayedTransition(root)
                    mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                    showSaveAndRe(View.INVISIBLE)
                    mTvStateMsg!!.text = "点击录音"
                    if (oldRecord == null) {
                        saveRecord(file, time)
                        callback.onEvent(1)
                    } else {
                        saveOldRecord(file, true)
                    }
                }
            }

            override fun onProgress(time: Int) {
                mProgressBarRecord!!.progress = time
                mTvRecordTime!!.text = Util.formatSeconds3(time)
            }

            override fun onMaxProgress(time: Int) {
                mProgressBarRecord!!.max = time
                mtvAllTime!!.text = Util.formatSeconds3(time)
            }

            override fun onError(e: Exception) {
                mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                setRecord(oldRecord)
            }

            override fun autoComplete(file: String, time: Int) {
                if (File(file).exists()) {
                    TransitionManager.beginDelayedTransition(root)
                    mIvRecordState!!.setImageResource(R.mipmap.icon_start_record)
                    showSaveAndRe(View.INVISIBLE)
                    mTvStateMsg!!.text = "录制完成"
                    if (oldRecord == null) {
                        saveRecord(file, time)
                        showRecordNewDialog()
                    } else {
                        saveOldRecord(file, false)
                    }
                }
            }

            override fun needPermission() {
                callback.onEvent(3)
            }
        }

        recordUtils = MixRecordUtils(recordCallBack)
        musicView?.setRecordUtil(recordUtils)
        musicView!!.setAddMusicView(addMusic!!)//要先设置“添加控件”，因为后面又隐藏
        musicView!!.setDialog(musicDialog)
    }

    /**
     * 保持录音
     *
     * @param file     文件地址
     * @param isFinish true 保持后切换界面，false 展示是否录制下一个界面
     */
    private fun saveOldRecord(file: String, isFinish: Boolean) {
        Flowable.just(file)
            .subscribeOn(Schedulers.io())
            .flatMap { s -> getMediaTime(s) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ o ->
                if (oldRecord != null && isHasChange) {
                    oldRecord!!.audioContent = mEditInfo!!.text.toString()
                    oldRecord!!.audioLength = o
                    RecordDbUtils.getInstance().update(oldRecord!!)
                    EventBus.getDefault().post(MainThreadEvent(MainThreadEvent.RECORD_REFRESH_RECORD, oldRecord!!))
                }
                if (isFinish) {
                    callback.onEvent(1)
                } else {
                    showRecordNewDialog()
                }
            }, { callback.onEvent(1) })
    }

    /**
     * 是否录制新内容
     */
    private fun showRecordNewDialog() {
        AlertDialog.Builder(context)
            .setTitle("录音已保存")
            .setMessage("已成功录满${1200/60}分钟，录音已保存。是否继续录制下一条？")
            .setNegativeButton("查看本条") { _, _ -> callback.onEvent(1) }
            .setPositiveButton("录下一条") { _, _ ->
                setRecord(null)
                ArmsUtils.makeText("上条录音已保存至“我的录音”")
            }
            .show()
    }


    //得到media的时间长度
    private fun getMediaTime(s: String): Flowable<Int> {
        return Flowable.create({ emitter ->
            val audioLength = Util.getAudioLength(context, s)
            emitter.onNext(audioLength)
        }, BackpressureStrategy.BUFFER)
    }


    /**
     * 保存录音，并且通知修改
     */
    private fun saveRecord(file: String, time: Int) {
        try {
            val record = Record("1", file, System.currentTimeMillis().toString() + "",
                Util.getAudioLength(context, file), mEditInfo!!.text.toString())
            RecordDbUtils.getInstance().save(record)
            EventBus.getDefault().post(MainThreadEvent(MainThreadEvent.RECORD_REFRESH_MY, record))
        } catch (e: Exception) {
            Timber.i(e.message)
        }

    }

    /**
     * 设置是否oldRecord,没有点击录音，有点击继续
     */
    fun setRecord(record: Record?) {
        if (record != null) {
            oldRecord = record
            recordUtils!!.setTime(oldRecord!!.audioLength.toLong()*1000)
            mEditInfo = buildEditTextView()
            mEditInfo!!.setText(oldRecord!!.audioContent)
            mTvStateMsg!!.text = "点击继续"
        } else {
            oldRecord = null
            recordUtils!!.setTime(0)
            mEditInfo!!.setText("")
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
            R.id.iv_record_state -> recordUtils!!.startOrPause(oldRecord?.audio_url?:"")
            R.id.ll_add_music -> showMusicDialog()
            else -> {
            }
        }
    }

    private fun showMusicDialog() {
        musicDialog?.showBottomSheet()
    }

    /**
     * 展示重新录制
     */
    private fun showRerecordDialog() {
        AlertDialog.Builder(context)
            .setTitle("重新录制")
            .setMessage("确定删除当前的录音，并重新录制吗？")
            .setNegativeButton("取消") { _, _ -> recordUtils!!.stopFullRecord() }
            .setPositiveButton("重录") { _, _ ->
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
        AlertDialog.Builder(context)
            .setTitle("温馨提示")
            .setMessage("确定要停止录音吗？")
            .setNegativeButton("停止录音") { _, _ -> recordUtils!!.stopFullRecord() }
            .setPositiveButton("继续录音") { _, _ -> recordUtils!!.startOrPause() }
            .show()
    }

    fun onStop() {
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
                    val content = mEditInfo!!.text.toString()
                    if (oldRecord != null) {
                        oldRecord!!.audioContent = content
                        RecordDbUtils.getInstance().update(oldRecord!!)
                    }
                    callback.onEvent(1)
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
