package me.shetj.mp3recorder.record.activity.sim

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.IBinder
import android.transition.Scene
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.listener.OnItemClickListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.dialog.OrangeDialog
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.RecordService
import me.shetj.mp3recorder.record.bean.Music
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.bean.RecordDbUtils
import me.shetj.mp3recorder.record.utils.EventCallback
import me.shetj.mp3recorder.record.utils.MainThreadEvent
import me.shetj.mp3recorder.record.utils.RecordCallBack
import me.shetj.mp3recorder.record.utils.Util
import me.shetj.mp3recorder.record.view.BackgroundMusicView
import me.shetj.mp3recorder.record.view.MusicListBottomSheetDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.File

/**
 * 录制声音界面
 */
open class RecordPage(
    private val context: AppCompatActivity,
    mRoot: ViewGroup,
    private val callback: EventCallback
) : View.OnClickListener {

    private val root: RelativeLayout =
        LayoutInflater.from(context).inflate(R.layout.page_record, null) as RelativeLayout
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
    private var bindService: Boolean = false
    private var recordCallBack: RecordCallBack? = null
    private var work: RecordService.Work? = null
    private var intent: Intent? = null
    private var musicView: BackgroundMusicView? = null
    private var addMusic: LinearLayout? = null
    private var musicDialog: MusicListBottomSheetDialog

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            work = iBinder as RecordService.Work
            work!!.setCallBack(recordCallBack)
            Timber.i("onServiceConnected")
            musicView?.setRecordUtil(work!!.getRecordUtil())
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Timber.i("onServiceDisconnected")
        }
    }

    init {
        scene = Scene(mRoot, root as View)
        bindService()
        initView(root)
        initData()
        musicDialog = MusicListBottomSheetDialog(context)
        musicDialog.setOnItemClickListener(OnItemClickListener { adapter, _, position ->
            val music = adapter.getItem(position) as Music
            musicView?.setMusic(music)
            addMusic?.visibility = View.GONE
        })
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
        addMusic = view.findViewById(R.id.ll_add_music)
        addMusic!!.setOnClickListener(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
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


    //绑定service
    private fun bindService() {
        intent = Intent(context, RecordService::class.java)
        context.startService(intent)
        bindService = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unBindService() {
        if (bindService) {
            context.unbindService(serviceConnection)
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun buildEditTextView(): EditText {
        mBunceSv!!.removeAllViews()
        val editText = EditText(this.context)
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        editText.layoutParams = params
        editText.minHeight = ArmsUtils.dp2px(500f)
        editText.textSize = 16f
        editText.gravity = Gravity.TOP
        editText.setBackgroundColor(Color.WHITE)
        editText.setPadding(
            ArmsUtils.dp2px(30f),
            ArmsUtils.dp2px(27f),
            ArmsUtils.dp2px(30f),
            ArmsUtils.dp2px(50f)
        )
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
                Timber.i("time = $time\nvolume$volume")
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
                        saveRecord(file)
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
                work!!.stop()
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
                        saveRecord(file)
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
        OrangeDialog.Builder(context)
            .setTitle("录音已保存")
            .setContent("已成功录满${1200 / 60}分钟，录音已保存。是否继续录制下一条？")
            .setNegativeText("查看本条")
            .setOnNegativeCallBack { _, _ ->  callback.onEvent(1)}
            .setPositiveText("录下一条")
            .setonPositiveCallBack {  _, _ ->
                setRecord(null)
                ArmsUtils.makeText("上条录音已保存至“我的录音”")
            }
            .show()
    }


    //得到media的时间长度
    private fun getMediaTime(s: String): Flowable<Int> {
        return Flowable.create({ emitter ->
            val audioLength = Util.getAudioLength(s)
            emitter.onNext(audioLength)
        }, BackpressureStrategy.BUFFER)
    }


    /**
     * 保存录音，并且通知修改
     */
    private fun saveRecord(file: String) {
        try {
            val record = Record(
                "1", file, System.currentTimeMillis().toString() + "",
                Util.getAudioLength(file), mEditInfo!!.text.toString()
            )
            RecordDbUtils.getInstance().save(record)
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
            work!!.setTime(oldRecord!!.audioLength)
            mEditInfo = buildEditTextView()
            mEditInfo!!.setText(oldRecord!!.audioContent)
            mTvStateMsg!!.text = "点击继续"
        } else {
            oldRecord = null
            work!!.setTime(0)
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
            R.id.tv_save_record -> work!!.recordComplete()
            R.id.iv_record_state -> work!!.statOrPause(oldRecord?.audio_url)
            R.id.ll_add_music -> showMusicDialog()
            else -> {
            }
        }
    }

    private fun showMusicDialog() {
        this.musicDialog.showBottomSheet()
    }

    /**
     * 展示重新录制
     */
    private fun showRerecordDialog() {
        OrangeDialog.Builder(context)
            .setTitle("重新录制")
            .setContent("确定删除当前的录音，并重新录制吗？")
            .setNegativeText("取消")
            .setOnNegativeCallBack {   _, _ -> work!!.recordComplete() }
            .setPositiveText("重录")
            .setonPositiveCallBack {  _, _ ->
                //可自行判断是否删除老的文件
                oldRecord?.audioLength = 0
                oldRecord?.audio_url = ""
                setRecord(oldRecord)
                work!!.reRecord()
            }
            .show()
    }

    /**
     * 展示中途推出,
     */
    private fun showTipDialog() {
        onPause()//先暂停
        OrangeDialog.Builder(context)
            .setTitle("温馨提示")
            .setContent("确定要停止录音吗？")
            .setNegativeText("停止录音")
            .setOnNegativeCallBack { _, _ -> work!!.recordComplete() }
            .setPositiveText("继续录音")
            .setonPositiveCallBack { _, _ -> work!!.statOrPause() }
            .show()
    }

    fun onStop() {
        if (work != null) {
            //如果在正录音中，要提醒用户是否停止录音
            if (work!!.isRecording) {
                showTipDialog()
            } else {
                if (work!!.hasRecord()) {
                    //如果录制了，默认是完成录制
                    work!!.recordComplete()
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
        if (work != null) {
            work!!.pause()
        }
    }

    fun onDestroy() {
        Timber.i("onDestroy")
        context.stopService(intent)
        unBindService()
    }

}
