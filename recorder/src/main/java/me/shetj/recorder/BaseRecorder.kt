package me.shetj.recorder

import android.media.MediaRecorder
import me.shetj.player.PermissionListener
import me.shetj.player.PlayerListener
import me.shetj.player.RecordListener
import me.shetj.recorder.util.BytesTransUtil
import java.io.File
import kotlin.math.sqrt


abstract class BaseRecorder {

    enum class RecorderType(name: String) {
        SIM("Mp3Recorder"), //
        MIX("MixRecorder")
    }

    enum class AudioSource(var type:Int){
        MIC(MediaRecorder.AudioSource.MIC),
        VOICE_COMMUNICATION(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
    }

    protected var mVolume: Int = 0
    var isRecording = false
        protected set
    //当前状态
    var state = RecordState.STOPPED
        protected set

    //录制时间
    var duration = 0L
        protected set

    abstract val realVolume: Int
    abstract fun setOutputFile(outputFile: String, isContinue: Boolean = false): BaseRecorder
    abstract fun setOutputFile(outputFile: File, isContinue: Boolean = false): BaseRecorder
    abstract fun setRecordListener(recordListener: RecordListener?): BaseRecorder
    abstract fun setPermissionListener(permissionListener: PermissionListener?): BaseRecorder
    abstract fun setBackgroundMusic(url:String):BaseRecorder
    abstract fun setBackgroundMusicListener(listener: PlayerListener) :BaseRecorder
    abstract fun setMp3Quality(mp3Quality: Int): BaseRecorder
    abstract fun setMaxTime(mMaxTime: Int): BaseRecorder
    abstract fun setWax(wax: Float): BaseRecorder
    abstract fun setVolume(volume: Float): BaseRecorder
    abstract fun start()
    abstract fun stop()
    abstract fun onResume()
    abstract fun onPause()
    abstract fun startPlayMusic()
    abstract fun isPauseMusic():Boolean
    abstract fun pauseMusic()
    abstract fun resumeMusic()
    abstract fun onReset()
    abstract fun onDestroy()


    protected fun calculateRealVolume(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        if (readSize > 0) {
            val amplitude = sum / readSize
            mVolume = sqrt(amplitude).toInt()
            if (mVolume < 5) {
                for (i in 0 until readSize) {
                    buffer[i] = 0
                }
            }
        }
    }
    protected fun calculateRealVolume(buffer: ByteArray) {
        val shorts = BytesTransUtil.bytes2Shorts(buffer)
        val readSize = shorts.size
        calculateRealVolume(shorts,readSize)
    }
}
