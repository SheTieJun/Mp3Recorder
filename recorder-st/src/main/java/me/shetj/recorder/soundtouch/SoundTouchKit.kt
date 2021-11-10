package me.shetj.recorder.soundtouch

import me.shetj.ndk.soundtouch.STKit
import me.shetj.recorder.core.ISoundTouchCore

/**
 * @author stj
 * @Date 2021/11/4-18:17
 * @Email 375105540@qq.com
 * 变音控制
 */
internal class SoundTouchKit private constructor() : ISoundTouchCore {

    private var isUseST: Boolean = true

    companion object {

        @Volatile
        private var sInstance: SoundTouchKit? = null

        fun getInstance(): SoundTouchKit {
            return sInstance ?: synchronized(SoundTouchKit::class.java) {
                return SoundTouchKit()
            }
        }

        fun onDestroy() {
            sInstance = null
        }
    }


    override fun changeUse(isUseST: Boolean) {
        this.isUseST = isUseST
    }

    fun isUse(): Boolean {
        return isUseST
    }

    /**
     *   fun init(
    channels: Int, //设置声道(1单,2双)
    sampleRate: Int,//设置采样率
    tempo: Int, //指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快,通过拉伸时间，改变声音的播放速率而不影响音调。
    @FloatRange(from = -12.0, to = 12.0) pitch: Float,//pitch 是音调 这个就是我们的重点了， 大于0 是变女生，小于0是变男声
    rate: Float//指定播放速率，源rate=1.0，小于1变慢；大于1变快
    )
     */
    override fun init(channels: Int, sampleRate: Int, tempo: Int, pitch: Float, rate: Float) {
        STKit.getInstance().init(channels, sampleRate, tempo, pitch, rate)
    }

    /**
     * rate (-50 .. +100 %)
     */
    override fun setRateChange(rateChange: Float) {
        STKit.getInstance().setRateChange(rateChange)
    }

    override fun setTempoChange(tempoChange: Float) {
        STKit.getInstance().setTempoChange(tempoChange)
    }


    //处理玩最后的数据
    fun flush(mp3buf: ShortArray): Int {
        return STKit.getInstance().flush(mp3buf)
    }

    override fun close() {
        STKit.getInstance().close()
    }

    //  指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
    override fun setTempo(tempo: Float) {
        STKit.getInstance().setTempo(tempo)
    }

    //在源pitch的基础上，使用半音(Semitones)设置新的pitch [-12.0,12.0]
    override fun setPitchSemiTones(pitch: Float) {
        STKit.getInstance().setPitchSemiTones(pitch)
    }

    //指定播放速率
    override fun setRate(speed: Float) {
        STKit.getInstance().setRate(speed)
    }

    override fun processFile(inputFile: String, outputFile: String): Boolean {
        return STKit.getInstance().processFile(inputFile, outputFile)
    }

    fun putSamples(samples: ShortArray, len: Int) {
        return STKit.getInstance().putSamples(samples, len)
    }

    fun receiveSamples(outputBuf: ShortArray): Int {
        return STKit.getInstance().receiveSamples(outputBuf)
    }

}