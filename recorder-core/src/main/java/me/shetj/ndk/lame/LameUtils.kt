package me.shetj.ndk.lame

object LameUtils {

    init {
        System.loadLibrary("shetj_mp3lame")
    }

    external fun version(): String

    /**
     *  * –lowpass freq	设定低通滤波器的起始点为 freq
     * 高于这个频率的声音会被截除。 Hz
     *  * –highpass freq	设定高通滤波起始点为 freq
     * 低于这个频率的声音会被截除。 Hz
     *  -1=no filter
     */
    external fun init(
        inSampleRate: Int,
        inChannel: Int,
        outSampleRate: Int,
        outBitrate: Int,
        quality: Int,
        lowpassFreq: Int,
        highpassFreq: Int,
        vbr: Boolean,
        enableLog:Boolean //是否输出日志
    )

    /**
     * 如果单声道使用该方法
     * samples =bufferLeft.size
     *
     * @return      number of bytes output in mp3buf. Can be 0
     *                 -1:  mp3buf was too small
     *                 -2:  malloc() problem
     *                 -3:  lame_init_params() not called
     *                 -4:  psycho acoustic problems
     */
    external fun encode(
        bufferLeft: ShortArray,
        bufferRight: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    /**
     * 双声道使用该方法
     * samples = pcm.size/2
     * @return      number of bytes output in mp3buf. Can be 0
     *                 -1:  mp3buf was too small
     *                 -2:  malloc() problem
     *                 -3:  lame_init_params() not called
     *                 -4:  psycho acoustic problems
     */
    external fun encodeInterleaved(
        pcm: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int


    external fun encodeByByte(
        bufferLeft: ByteArray,
        bufferRight: ByteArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    external fun encodeInterleavedByByte(
        pcm: ByteArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int


    /**
     * 用来处理，VBR模式的时候，时间出现问题
     */
    external fun writeVBRHeader(file: String)

    external fun flush(mp3buf: ByteArray): Int
    external fun close()

    /**
     * 获取pcm的db
     */
    external fun getPCMDB(pcm: ShortArray, samples: Int):Int

}
