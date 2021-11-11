package me.shetj.recorder.core

import androidx.annotation.FloatRange

/*** SoundTouch
 *
 *  * ST处理的对象是PCM（Pulse Code Modulation，脉冲编码调制），.wav文件中主要是这种格式
 *  mp3等格式经过了压缩，需转换为PCM后再用ST处理。
 *  * Tempo节拍 ：通过拉伸时间，改变声音的播放速率而不影响音调。
 *  * Playback Rate回放率 : 以不同的转率播放唱片（DJ打碟？），通过采样率转换实现。
 *  * Pitch音调 ：在保持节拍不变的前提下改变声音的音调，结合采样率转换+时间拉伸实现。如：增高音调的处理过程是：将原音频拉伸时长，再通过采样率转换，同时减少时长与增高音调变为原时长。
 *  @author stj
 * @Date 2021/11/5-15:11
 * @Email 375105540@qq.com
 */

interface ISoundTouchCore {

    //是否使用变音，中途可以切换变音
    fun changeUse(isUseST: Boolean)

    //是否使用变音功能
    fun isUse():Boolean

    //指定播放速率，源rate=1.0，小于1变慢；大于1
    fun setRate(rate: Float)

    //一般用来设置倍速，我们变音，默认 1.0就好
    fun setTempo(tempo: Float)

    //在原速1.0基础上，按百分比做增量，取值(-50 .. +100 %)
    fun setRateChange(@FloatRange(from = -50.0, to = 100.0) rateChange: Float)

    //在原速1.0基础上 源tempo=1.0，小于1则变慢；大于1变快 tempo (-50 .. +100 %)
    fun setTempoChange(@FloatRange(from = -50.0, to = 100.0) tempoChange: Float)

    //在源pitch的基础上，使用半音(Semitones)设置新的pitch [-12.0,12.0]
    //男声:-10
    //女声:+10
    fun setPitchSemiTones(@FloatRange(from = -12.0, to = 12.0) pitch: Float)

    //只处理wav 文件
    fun processFile(inputFile: String, outputFile: String): Boolean


}