## SoundTouchRecorder
- 变音录音功能
- 暂不不支持背景音乐，背景好像不需要进行变音，所以变音不适合添加背景音乐功能，所以背景音乐相关都废弃

### [SoundTouch NDK](https://github.com/SheTieJun/Android_NDK/tree/master/soundTouch)


```kotlin
 fun init(
        channels: Int, //设置声道(1单,2双)
        sampleRate: Int,//设置采样率
        tempo: Int, //指定节拍，设置新的节拍tempo，源tempo=1.0，小于1则变慢；大于1变快,通过拉伸时间，改变声音的播放速率而不影响音调。
        @FloatRange(from = -12.0, to = 12.0) pitch: Float,//pitch 是音调 这个就是我们的重点了， 大于0 是变女生，小于0是变男声
        rate: Float//指定播放速率，源rate=1.0，小于1变慢；大于1变快 rate (-50 .. +100 %)
    )

    //指定播放速率
    fun setRate(speed: Float)
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

```

