package me.shetj.recorder.soundtouch

import android.util.Log
import androidx.annotation.FloatRange
import me.shetj.ndk.soundtouch.SoundTouch
import me.shetj.recorder.core.ISoundTouchCore

/**
 * @author stj
 * @Date 2021/11/4-18:17
 * @Email 375105540@qq.com
 * 变音控制
 */
internal class SoundTouchKit : ISoundTouchCore {

    private var isUseST: Boolean = true //是否进行变音

    private var tempo: Float = 1f //指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
    private var pitch: Float = 0f//指定音调值大于0 是变女生，小于0是变男声
    private var rate: Float = 1f//指定播放速率，源rate=1.0，小于1变慢；大于1

    private val soundTouch: SoundTouch by lazy { SoundTouch() }

    private var handle: Long = 0

    internal fun init(channel: Int, samplingRate: Int) {
        if (handle == 0L) {
            handle = soundTouch.newInstance()
        }
        soundTouch.init(channel, samplingRate, tempo, pitch, rate)
    }


    override fun changeUse(isUseST: Boolean) {
        this.isUseST = isUseST
    }

    override fun isUse(): Boolean {
        return isUseST
    }

    /**
     * rate (-50 .. +100 %)
     */
    override fun setRateChange(@FloatRange(from = -50.0, to = 100.0) rateChange: Float) {
        this.rate = 1f + 0.01f * rateChange
        soundTouch.setRateChange(rateChange)
    }

    override fun setTempoChange(@FloatRange(from = -50.0, to = 100.0) tempoChange: Float) {
        this.tempo = 1f + 0.01f * tempoChange
        soundTouch.setTempoChange(tempoChange)
    }


    //  指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
    override fun setTempo(tempo: Float) {
        this.tempo = tempo
        soundTouch.setTempo(tempo)
    }

    //在源pitch的基础上，使用半音(Semitones)设置新的pitch [-12.0,12.0]
    override fun setPitchSemiTones(@FloatRange(from = -12.0, to = 12.0) pitch: Float) {
        this.pitch = pitch
        soundTouch.setPitchSemiTones(pitch)
    }

    //指定播放速率，源rate=1.0，小于1变慢；大于1
    override fun setRate(rate: Float) {
        this.rate = rate
        soundTouch.setRate(rate)
    }

    //只处理wav 文件
    override fun processFile(inputFile: String, outputFile: String): Boolean {
        return if (soundTouch.processFile(inputFile, outputFile) == 0) {
            true
        } else {
            Log.e("soundTouch", soundTouch.getErrorString())
            false
        }
    }

    fun receiveSamples(outputBuf: ShortArray): Int {
        return soundTouch.receiveSamples(outputBuf)
    }

    fun putSamples(samples: ShortArray, len: Int) {
        soundTouch.putSamples(samples, len)
    }

    //重置到最开始的值
    fun clean() {
        tempo = 1f
        pitch = 0f
        rate = 1f
    }

    fun destroy() {
        if (handle != 0L) {
            soundTouch.deleteInstance()
            handle = 0
        }
    }

    //处理玩最后的数据
    fun flush(mp3buf: ShortArray): Int {
        return soundTouch.flush(mp3buf)
    }

}