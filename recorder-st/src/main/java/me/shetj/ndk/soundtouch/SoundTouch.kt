package me.shetj.ndk.soundtouch


/**
 * @author stj
 * @Date 2021/11/4-18:17
 * @Email 375105540@qq.com
 */

internal class SoundTouch {


    external fun newInstance(): Long
    external fun deleteInstance()
    external fun getVersionString(): String
    external fun getErrorString(): String

    external fun init(
        channels: Int, //设置声道(1单,2双)
        sampleRate: Int,//设置采样率
        tempo: Int, //指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快
        pitch: Float, //指定音调值
        speed: Float//指定播放速率
    )

    // 在原速1.0基础上，按百分比做增量，取值(-50 .. +100 %)
    external fun setRateChange(rateChange: Float)
    external fun setTempoChange(tempoChange: Float)
    external fun setTempo(tempo: Float)
    external fun setPitchSemiTones(pitch: Float)
    external fun setRate(speed: Float)

    //直接WAV处理文件
    external fun processFile(inputFile: String, outputFile: String): Int


    //putSamples 的次数可能小于receiveSamples
    external fun putSamples(samples: ShortArray, len: Int)
    external fun receiveSamples(outputBuf: ShortArray): Int



    //获取最后一段数据
    external fun flush(mp3buf: ShortArray): Int


}