/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.mp3recorder.record.activity.mix

import android.Manifest
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import me.shetj.base.ktx.hasPermission
import me.shetj.base.mvp.BaseActivity
import me.shetj.base.mvp.EmptyPresenter
import me.shetj.base.tools.app.ArmsUtils.Companion.statuInScreen
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.RecordingNotification
import me.shetj.mp3recorder.record.utils.EventCallback
import kotlinx.coroutines.delay
import me.shetj.base.ktx.launch


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
        setContentView(R.layout.activity_my_record)
    }


    override fun initView() {
        statuInScreen(true)
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

    override fun initData() {

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
        hasPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO, isRequest = true
        )
    }

    override fun onBackPressed() {
        if (isRecord) {
            recordPage!!.onStop()
        } else {
            super.onBackPressed()
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
