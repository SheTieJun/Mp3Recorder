
package me.shetj.recorder.ui

import android.Manifest
import android.transition.TransitionManager
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import me.shetj.dialog.OrangeDialog
import me.shetj.recorder.core.SimRecordListener
import me.shetj.recorder.ui.databinding.RecordLayoutPopupBinding

typealias Success = (file: String) -> Unit

class RecorderPopup(
    private val context: AppCompatActivity,
    private var needPlay: Boolean = true,
    private val maxTime: Long = (30 * 60 * 1000).toLong(),
    private var onSuccess: Success? = null
) : BasePopupWindow<RecordLayoutPopupBinding>(context) {

    private var remindDialog: OrangeDialog? = null
    private var isComplete = false

    private val playerListener: SimPlayerListener = object : SimPlayerListener() {
        override fun onCompletion() {
            super.onCompletion()
            mViewBinding.tvRecordTime.text = formatSeconds(0, maxSecond())
            mViewBinding.tvState.text = "试听"
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_audition)
        }

        override fun onPause() {
            super.onPause()
            mViewBinding.tvState.text = "已暂停试听"
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_audition)
        }

        override fun onStart(duration: Int) {
            super.onStart(duration)
            mViewBinding.tvState.text = "播放中"
            mViewBinding.tvState.isVisible = true
            val info = "/" + formatSeconds((duration / 1000).toLong(), maxSecond())
            mViewBinding.tvRecordDuration.text = info
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_pause)
        }

        override fun onProgress(current: Int, duration: Int) {
            super.onProgress(current, duration)
            mViewBinding.tvRecordTime.text = formatSeconds((current / 1000).toLong(), maxSecond())
        }

        override fun onResume() {
            super.onResume()
            mViewBinding.tvState.text = "播放中"
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_pause)
        }
    }

    private fun maxSecond() = maxTime / 1000

    private val listener: SimRecordListener = object : SimRecordListener() {

        override fun onStart() {
            TransitionManager.beginDelayedTransition(mViewBinding.root)
            mViewBinding.llTime.isVisible = true
            mViewBinding.tvTips.text = "录音中，${maxTime / 60000}分钟后自动保存"
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_pause)
            mViewBinding.spreadView.start = true
        }

        override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
            super.onSuccess(isAutoComplete, file, time)
            if (isAutoComplete) {
                onShowSuccessView()
            } else {
                mViewBinding.tvRecordTime.text = formatSeconds(maxSecond(), maxSecond())
            }
            complete(time)
        }

        override fun onRecording(time: Long, volume: Int) {
            super.onRecording(time, volume)
            mViewBinding.tvRecordTime.text = formatSeconds(time / 1000, maxSecond())
            if (time >= 3000) {
                mViewBinding.tvReRecord.isVisible = true
                mViewBinding.tvSaveRecord.isVisible = true
            }
        }

        override fun onResume() {
            super.onResume()
            TransitionManager.beginDelayedTransition(mViewBinding.root)
            mViewBinding.llTime.isVisible = true
            val tip = "录音中，${maxTime / 60000}分钟后自动保存"
            mViewBinding.tvTips.text = tip
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_pause)
            mViewBinding.spreadView.start = true
        }

        override fun onPause() {
            super.onPause()
            mViewBinding.spreadView.start = false
            mViewBinding.tvTips.isVisible = false
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_start)
            mViewBinding.tvState.text = "继续录制"
            mViewBinding.tvState.isVisible = true
        }

        override fun onMaxChange(time: Long) {
            super.onMaxChange(time)
            val maxTimeInfo = "/" + formatSeconds(time / 1000, maxSecond())
            mViewBinding.tvRecordDuration.text = maxTimeInfo
        }

        override fun onReset() {
            isComplete = false
            mViewBinding.spreadView.start = false
            mViewBinding.tvTips.isVisible = true
            mViewBinding.tvTips.text = "点击下方按钮开始录音"
            mViewBinding.llTime.isVisible = false
            mViewBinding.tvReRecord.isVisible = false
            mViewBinding.tvSaveRecord.isVisible = false
            mViewBinding.tvState.text = ""
            mViewBinding.tvRecordTime.text = formatSeconds(0, maxSecond())
            val timeInfo = "/" + formatSeconds(maxSecond(), maxSecond())
            mViewBinding.tvRecordDuration.text = timeInfo
            mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_start)
        }

        override fun needPermission() {
            super.needPermission()
            context.hasPermission(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, isRequest = true
            )
        }

        override fun onRemind(duration: Long) {
            super.onRemind(duration)
        }

        override fun onError(e: Exception) {
            super.onError(e)
            onReset()
        }
    }

    private fun SimRecordListener.complete(time: Long) {
        if (time > 3000) {
            onShowSuccessView()
        } else {
            Toast.makeText(context, "录制时长不足3秒，无法保存", Toast.LENGTH_SHORT).show()
            onReset()
        }
    }

    private val recordUtils by lazy { MixRecordUtils(context, maxTime, listener) }

    private val player: AudioPlayer by lazy { AudioPlayer() }

    fun setOnSuccess(onSuccess: Success?) {
        this.onSuccess = onSuccess
    }

    override fun audioLoss() {
        super.audioLoss()
        player.pause()
        recordUtils.stopFullRecord()
    }

    override fun RecordLayoutPopupBinding.initUI() {
        ivRecordState.setOnClickListener {
            if (!isComplete) {
                if (needPlay) {
                    recordUtils.startOrComplete()
                } else {
                    recordUtils.startOrPause()
                }
                requestAudioFocus()
            } else {
                player.playOrPause(recordUtils.saveFile, playerListener)
            }
        }

        tvSaveRecord.setOnClickListener {
            if (!isComplete) {
                isComplete = true
                recordUtils.stopFullRecord()
            } else {
                player.pause()
            }
            realDismiss()
        }

        tvReRecord.setOnClickListener {
            recordUtils.stopFullRecord()
            recordUtils.reset()
        }
        mViewBinding.tvCancel.setOnClickListener {
            if (isComplete || recordUtils.hasRecord()) {
                showRemindTip()
            } else {
                dismiss()
            }
        }
    }

    /**
     * activity 返回键判断
     */
    fun onBackPress(): Boolean {
        return if (recordUtils.hasRecord() || isComplete) {
            showRemindTip()
            false
        } else {
            true
        }
    }

    private fun showRemindTip() {
        (
            remindDialog ?: OrangeDialog.Builder(mViewBinding.root.context)
                .setTitle("是否保存录音")
                .setNegativeText("取消")
                .setOnNegativeCallBack { _, _ ->
                    recordUtils.cleanPath()
                    recordUtils.stopFullRecord()
                    realDismiss()
                }
                .setPositiveText("保存")
                .setonPositiveCallBack { _, _ ->
                    if (!isComplete) {
                        isComplete = true
                        recordUtils.stopFullRecord()
                    }
                    realDismiss()
                }.build().also {
                    remindDialog = it
                }
            ).show()
    }

    override fun showPop() {
        recordUtils.onReset()
        setOnDismissListener {
            if (isComplete && mViewBinding.tvReRecord.isVisible) {
                recordUtils.saveFile?.let { onSuccess?.invoke(it) }
            }
            isComplete = false
        }
        showAtLocation(context.window.decorView, Gravity.BOTTOM, 0, 0)
    }

    private fun onShowSuccessView() {
        if (isShowing) {
            isComplete = true
        }
        mViewBinding.tvRecordTime.text = formatSeconds(maxSecond(), maxSecond())
        mViewBinding.spreadView.start = false
        mViewBinding.tvTips.isVisible = false
        mViewBinding.ivRecordState.setImageResource(R.drawable.ic_record_audition)
        mViewBinding.tvState.text = "试听"
        mViewBinding.tvState.isVisible = true
    }

    private fun realDismiss() {
        dismiss()
    }

    override fun dismissStop() {
        player.pause()
        recordUtils.stopFullRecord()
//        super.dismissStop()
    }

    override fun dismissOnDestroy() {
        super.dismissOnDestroy()
        recordUtils.destroy()
        player.stopPlay()
    }

    override fun initViewBinding(mContext: AppCompatActivity): RecordLayoutPopupBinding {
        return RecordLayoutPopupBinding.inflate(mContext.layoutInflater)
    }
}
