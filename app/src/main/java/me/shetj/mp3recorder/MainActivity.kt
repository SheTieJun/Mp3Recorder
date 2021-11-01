package me.shetj.mp3recorder

import android.Manifest
import me.shetj.base.ktx.hasPermission
import me.shetj.base.ktx.showToast
import me.shetj.base.ktx.start
import me.shetj.base.mvvm.BaseBindingActivity
import me.shetj.base.mvvm.BaseViewModel
import me.shetj.mp3recorder.databinding.ActivityMainBinding
import me.shetj.mp3recorder.record.activity.mix.MyMixRecordActivity
import me.shetj.mp3recorder.record.activity.sim.MyRecordActivity
import me.shetj.recorder.ui.RecorderPopup

class MainActivity : BaseBindingActivity<BaseViewModel, ActivityMainBinding>() {

    private val recorderPopup: RecorderPopup by lazy {
        RecorderPopup(this, needPlay = false, maxTime = (10 * 1000).toLong()) {
            it.showToast()
        }
    }

    override fun onActivityCreate() {
        super.onActivityCreate()
        hasPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            isRequest = true
        )

        mViewBinding.btnDemo.setOnClickListener {
            if (hasPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO, isRequest = true
                )
            ) {
                start<MyRecordActivity>()
            }
        }
        mViewBinding.btnDemo3.setOnClickListener {
            if (hasPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO, isRequest = true
                )
            ) {
                start<MyMixRecordActivity>()
            }
        }

        mViewBinding.btnDemo4.setOnClickListener {
            recorderPopup.showPop()
        }
    }

    override fun onBackPressed() {
        if (recorderPopup.onBackPress()) {
            super.onBackPressed()
        }
    }
}
