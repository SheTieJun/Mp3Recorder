package me.shetj.ndk.soundtouch


/**
 * @author stj
 * @Date 2021/11/4-18:17
 * @Email 375105540@qq.com
 */

internal class SoundTouch {

    companion object{
        init {
            System.loadLibrary("soundTouch")
        }
    }

    external fun newInstance(): Long
    external fun deleteInstance(handle: Long)
    external fun getVersionString(): String
    external fun getErrorString(): String

    external fun init(
        handle: Long,
        channels: Int, //设置声道(1单,2双)
        sampleRate: Int,//设置采样率
        tempo: Float, //指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
        pitch: Float, //指定音调值重点， 大于0 是变女生，小于0是变男声
        speed: Float //指定播放速率 源rate=1.0，小于1变慢；大于1
    )

    // 在原速1.0基础上，按百分比做增量，取值(-50 .. +100 %)
    external fun setRateChange(handle: Long, rateChange: Float)
    external fun setTempoChange(handle: Long, tempoChange: Float)
    external fun setTempo(handle: Long, tempo: Float)
    external fun setPitchSemiTones(handle: Long, pitch: Float)
    external fun setRate(handle: Long, speed: Float)

    //直接WAV处理文件
    external fun processFile(handle: Long, inputFile: String, outputFile: String): Int


    //putSamples 的次数可能小于receiveSamples
    external fun putSamples(handle: Long, samples: ShortArray, len: Int)

    //读取数据
    external fun receiveSamples(handle: Long, outputBuf: ShortArray): Int


    //获取最后一段数据
    external fun flush(handle: Long, mp3buf: ShortArray): Int


}