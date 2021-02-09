package me.shetj.recorder.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewbinding.ViewBinding

/**
 * popup 弹窗
 * 设置点击窗口外边窗口不消失，activity 可以被点击
 * {
 *   isOutsideTouchable = false
 *   isFocusable = false
 * }
 */
abstract class BasePopupWindow<VB : ViewBinding>(mContext: AppCompatActivity) :
    PopupWindow(mContext), LifecycleObserver {

    private val lazyViewBinding = lazy { initViewBinding(mContext) }
    protected val mViewBinding: VB by lazyViewBinding


    private var mAudioManager: AudioManager? = null
    private var focusChangeListener: AudioManager.OnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
        focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS ->
                //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                audioLoss()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                //短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                audioLoss()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            AudioManager.AUDIOFOCUS_GAIN -> {
            }
        }
    }

    open fun audioLoss() {

    }

    fun requestAudioFocus() {
        if (mAudioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {            //Android 8.0+
            val audioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setOnAudioFocusChangeListener(focusChangeListener).build()
            audioFocusRequest.acceptsDelayedFocusGain()
            mAudioManager!!.requestAudioFocus(audioFocusRequest)
        } else {
            mAudioManager!!.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun setAudioManager(context: Context) {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    init {
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        animationStyle = R.style.record_popup_window_anim_style
        isOutsideTouchable = false// 设置点击窗口外边窗口不消失
        isFocusable = false
        contentView = mViewBinding.root
        mViewBinding.initUI()
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mContext.lifecycle.addObserver(this)
        setAudioManager(mContext)
    }

    abstract fun initViewBinding(mContext: AppCompatActivity): VB

    abstract fun VB.initUI()

    abstract fun showPop()

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun dismissStop() {
        try {
            dismiss()
        } catch (ignored: Exception) {
            //暴力解决，可能的崩溃
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    open fun dismissOnDestroy() {
        try {
            dismiss()
        } catch (_: Exception) {
            //暴力解决，可能的崩溃
        }
    }
}