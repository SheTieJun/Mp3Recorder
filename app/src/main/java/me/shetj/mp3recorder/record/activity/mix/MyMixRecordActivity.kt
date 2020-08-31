package me.shetj.mp3recorder.record.activity.mix

import android.Manifest
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.widget.FrameLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import me.shetj.base.ktx.hasPermission
import me.shetj.base.mvp.BaseActivity
import me.shetj.base.mvp.EmptyPresenter
import me.shetj.base.tools.app.ArmsUtils.Companion.statuInScreen
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.RecordingNotification
import me.shetj.mp3recorder.record.utils.EventCallback
import java.util.concurrent.TimeUnit


/**
 * 我的录音界面
 */
class MyMixRecordActivity : BaseActivity<EmptyPresenter>(), EventCallback {

    private lateinit var mFrameLayout: FrameLayout
    private var myRecordAction: MyMixRecordPage? = null
    private var recordAction: MixRecordPage? = null
    private var isRecord = false
    private var recordTransition: Transition? = null
    private var myRecordTransition: Transition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_record)
    }


    override fun initView() {
        statuInScreen( true)
        canRecord()
        mFrameLayout = findViewById(R.id.frameLayout)
        myRecordAction = MyMixRecordPage(
            this,
            mFrameLayout,
            this
        )
        recordAction = MixRecordPage(
            this,
            mFrameLayout,
            this
        )
        //设置录音界面的动画
        recordTransition = TransitionInflater.from(this).inflateTransition(R.transition.record_page_slide)
        myRecordTransition = TransitionInflater.from(this).inflateTransition(R.transition.my_record_page_slide)
        isRecord = false
        TransitionManager.go(myRecordAction!!.scene, myRecordTransition)
    }

    override fun initData() {

    }


    override fun onEvent(message: Int) {
        when (message) {
            0 -> {
                setTitle(R.string.record)
                TransitionManager.go(recordAction!!.scene, recordTransition)
                recordAction!!.setRecord(null)
                isRecord = true
            }
            1 -> {
                setTitle(R.string.my_record)
                recordAction!!.setRecord(null)
                recordAction?.clearMusic()
                AndroidSchedulers.mainThread().scheduleDirect({
                    TransitionManager.go(myRecordAction!!.scene, myRecordTransition)
                },200,TimeUnit.MICROSECONDS)
                isRecord = false
            }
            2 -> {
                setTitle(R.string.record)
                val curRecord = myRecordAction!!.curRecord
                recordAction!!.setRecord(curRecord)
                TransitionManager.go(recordAction!!.scene, recordTransition)
                isRecord = true
            }
            3 -> canRecord()
            else -> {
            }
        }
    }

    private fun canRecord() {
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                ,isRequest = true)
    }

    override fun onBackPressed() {
        if (isRecord) {
            recordAction!!.onStop()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        if (recordAction != null) {
            recordAction!!.onDestroy()
        }
        if (myRecordAction != null) {
            myRecordAction!!.onDestroy()
        }
        RecordingNotification.cancel(this)
        super.onDestroy()
    }

    companion object {

        val NEED_CLOSE = "needClose"//完成上传后关闭界面 默认不关闭
        val POST_URL = "postUrl"//声音
    }

}
