package me.shetj.recorder

import android.media.AudioFormat
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

    enum class AudioChannel(var type: Int){
        MONO(1), //单声道
        STEREO(2) //双声道
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
    //设置输出路径
    abstract fun setOutputFile(outputFile: String, isContinue: Boolean = false): BaseRecorder
    //设置输出路径
    abstract fun setOutputFile(outputFile: File, isContinue: Boolean = false): BaseRecorder
    //设置录音监听
    abstract fun setRecordListener(recordListener: RecordListener?): BaseRecorder
    //设置权限监听
    abstract fun setPermissionListener(permissionListener: PermissionListener?): BaseRecorder
    //设计背景音乐的url,本地的
    abstract fun setBackgroundMusic(url:String):BaseRecorder
    //设置背景音乐的监听
    abstract fun setBackgroundMusicListener(listener: PlayerListener) :BaseRecorder
    //初始录音质量
    abstract fun setMp3Quality(mp3Quality: Int): BaseRecorder
    // 初始最大声音
    abstract fun setMaxTime(mMaxTime: Int): BaseRecorder
    //设置增强系数
    abstract fun setWax(wax: Float): BaseRecorder
    //设置背景声音大小
    abstract fun setVolume(volume: Float): BaseRecorder
    //开始录音
    abstract fun start()
    //停止录音
    abstract fun stop()
    //重新开始录音
    abstract fun onResume()
    //暂停录音
    abstract fun onPause()
    //开始播放音乐
    abstract fun startPlayMusic()
    //是否在播放音乐
    abstract fun isPauseMusic():Boolean
    //暂停背景音乐
    abstract fun pauseMusic()
    //重新播放背景音乐
    abstract fun resumeMusic()
    //充值
    abstract fun onReset()
    //结束
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
