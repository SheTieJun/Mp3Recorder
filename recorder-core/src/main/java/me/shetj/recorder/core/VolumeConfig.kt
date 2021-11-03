package me.shetj.recorder.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean


typealias OnVolumeChange = Float.() -> Unit

/**
 * 声音音量控制
 */
class VolumeConfig(private val context: WeakReference<Context>, var currVolumeF: Float = 1f) {

    private val isRegister = AtomicBoolean(false)
    private var audioManager:AudioManager?=null
    private val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?:10
    private var onVolumeChanges:MutableList<OnVolumeChange>  = ArrayList()


    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                currVolumeF = getCurVolume() / getMaxVoice().toFloat()
                onVolumeChanges.forEach {
                    it.invoke(currVolumeF)
                }
            }
        }
    }

    private val intentFilter = IntentFilter().apply {
        addAction("android.media.VOLUME_CHANGED_ACTION")
    }

    fun getCurVolume(): Int {
        return audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?:1
    }

    fun addChangeListener(onVolumeChange: OnVolumeChange){
        if (!onVolumeChanges.contains(onVolumeChange)){
            onVolumeChanges.add(onVolumeChange)
        }
    }

    fun removeChangeListener(onVolumeChange: OnVolumeChange){
        onVolumeChanges.remove(onVolumeChange)
    }

    fun registerReceiver() {
        if (isRegister.compareAndSet(false, true)) {
            if (audioManager == null) {
                audioManager =
                    context.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            }
            context.get()?.registerReceiver(mReceiver, intentFilter)
        }
    }

    fun unregisterReceiver() {
        if (isRegister.compareAndSet(true, false)) {
            context.get()?.unregisterReceiver(mReceiver)
            audioManager = null
        }
    }

    fun getMaxVoice() = max

    fun setAudioVoiceF(volume: Float) {
       setAudioVoice((volume * max).toInt())
    }

    fun setAudioVoice(volume: Int) {
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            0
        )
    }

    companion object {
        @Volatile
        private var sInstance: VolumeConfig? = null

        fun getInstance(context: Context): VolumeConfig {
            return sInstance ?: synchronized(VolumeConfig::class.java) {
                return VolumeConfig(WeakReference(context.applicationContext)).also {
                    it.currVolumeF = it.getCurVolume() / it.getMaxVoice().toFloat()
                    sInstance = it
                }
            }
        }

        fun onDestroy(){
            sInstance?.unregisterReceiver()
            sInstance = null
        }

    }

}