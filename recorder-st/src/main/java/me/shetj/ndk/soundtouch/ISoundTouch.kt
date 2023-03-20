
package me.shetj.ndk.soundtouch

// /*** SoundTouch
// *
// *  * ST处理的对象是PCM（Pulse Code Modulation，脉冲编码调制），.wav文件中主要是这种格式
// *  mp3等格式经过了压缩，需转换为PCM后再用ST处理。
// *  * Tempo节拍 ：通过拉伸时间，改变声音的播放速率而不影响音调。
// *  * Playback Rate回放率 : 以不同的转率播放唱片（DJ打碟？），通过采样率转换实现。
// *  * Pitch音调 ：在保持节拍不变的前提下改变声音的音调，结合采样率转换+时间拉伸实现。如：增高音调的处理过程是：将原音频拉伸时长，再通过采样率转换，同时减少时长与增高音调变为原时长。
// *  @author stj
// * @Date 2021/11/5-15:11
// * @Email 375105540@qq.com
// */
//
// interface ISoundTouch {
//
//    fun init(
//        channels: Int, //设置声道(1单,2双)
//        sampleRate: Int,//设置采样率
//        tempo: Float, //指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快,通过拉伸时间，改变声音的播放速率而不影响音调。
//        @FloatRange(from = -12.0, to = 12.0) pitch: Float,//音调, 重点， 大于0 是变女生，小于0是变男声
//        rate: Float//指定播放速率，源rate=1.0，小于1变慢；大于1
//    )
//
//    //指定播放速率
//    fun setRate(speed: Float)
//
//    //一般用来设置倍速，我们变音，默认 1.0就好
//    fun setTempo(tempo: Float)
//
//    //在原速1.0基础上，按百分比做增量，取值(-50 .. +100 %)
//    fun setRateChange(@FloatRange(from = -50.0, to = 100.0) rateChange: Float)
//
//    //在原速1.0基础上 源tempo=1.0，小于1则变慢；大于1变快 tempo (-50 .. +100 %)
//    fun setTempoChange(@FloatRange(from = -50.0, to = 100.0) tempoChange: Float)
//
//
//    //在源pitch的基础上，使用半音(Semitones)设置新的pitch [-12.0,12.0]
//    //男声:-10
//    //女声:+10
//    fun setPitchSemiTones(@FloatRange(from = -12.0, to = 12.0) pitch: Float)
//
//
//    //处理文件
//    fun processFile(inputFile: String, outputFile: String): Boolean
//
//    //实时处理PCM 流
//    fun putSamples(samples: ShortArray, len: Int)
//
//    fun receiveSamples(outputBuf: ShortArray): Int
//
//    //获取最后一段数据
//    fun flush(mp3buf: ShortArray): Int
//
//    fun close()
//
// }
