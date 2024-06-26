
package me.shetj.mp3recorder.record.activity.mix

import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import me.shetj.base.fix.FixPermission
import me.shetj.base.mvp.BaseActivity
import me.shetj.base.mvp.EmptyPresenter
import me.shetj.base.tools.app.ArmsUtils.Companion.statuInScreen
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.RecordingNotification
import me.shetj.mp3recorder.record.utils.EventCallback


/**
 * 我的录音界面
 */
class RecordActivity : BaseActivity<EmptyPresenter>(), EventCallback {

    private lateinit var mFrameLayout: FrameLayout
    private var recordListPage: RecordListPage? = null
    private var recordPage: RecordPage? = null
    private var isRecord = false
    private var recordTransition: Transition? = null
    private var myRecordTransition: Transition? = null
    private var btnRecorderType: AppCompatButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enabledOnBack = true
    }


    override fun initBaseView() {
        setContentView(R.layout.activity_my_record)
        statuInScreen(true)
        setOrientation(false)
        canRecord()
        mFrameLayout = findViewById(R.id.frameLayout)
        btnRecorderType = findViewById(R.id.btn_recorderType)
        recordListPage = RecordListPage(
            this,
            mFrameLayout,
            this
        )
        recordPage = RecordPage(
            this,
            mFrameLayout,
            this,
            btnRecorderType!!
        )
        //设置录音界面的动画
        recordTransition = TransitionInflater.from(this).inflateTransition(R.transition.record_page_slide)
        myRecordTransition = TransitionInflater.from(this).inflateTransition(R.transition.my_record_page_slide)
        isRecord = false
        TransitionManager.go(recordListPage!!.scene, myRecordTransition)
    }



    override fun onEvent(message: Int) {
        when (message) {
            0 -> {
                setTitle(R.string.record)
                TransitionManager.go(recordPage!!.scene, recordTransition)
                recordPage!!.setRecord(null)
                isRecord = true
                btnRecorderType?.isVisible = true
            }
            1 -> {
                setTitle(R.string.my_record)
                recordPage!!.setRecord(null)
                recordPage?.clearMusic()
                TransitionManager.go(recordListPage!!.scene, myRecordTransition)
                isRecord = false
                btnRecorderType?.isVisible = false
            }
            2 -> {
                setTitle(R.string.record)
                val curRecord = recordListPage!!.curRecord
                recordPage!!.setRecord(curRecord)
                TransitionManager.go(recordPage!!.scene, recordTransition)
                isRecord = true
                btnRecorderType?.isVisible = true
            }
            3 -> canRecord()
            else -> {
            }
        }
    }

    private fun canRecord() {
        FixPermission.checkReadMediaFile(this,true)
    }

    override fun onBack() {
        if (isRecord) {
            recordPage!!.onStop()
        }else{
            super.onBack()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        if (recordPage != null) {
            recordPage!!.onDestroy()
        }
        if (recordListPage != null) {
            recordListPage!!.onDestroy()
        }
        RecordingNotification.cancel(this)
        super.onDestroy()
    }

}
