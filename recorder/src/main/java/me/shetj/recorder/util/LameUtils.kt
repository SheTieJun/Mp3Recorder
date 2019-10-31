package me.shetj.recorder.util

object LameUtils {
    init {
        System.loadLibrary("shetj_mp3lame")
    }
    external fun version(): String

    external fun init(
        inSampleRate: Int,
        inChannel: Int,
        outSampleRate: Int,
        outBitrate: Int,
        quality: Int
    )

    /**
     * 如果单声道使用该方法
     * samples =bufferLeft.size
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

    external fun flush(mp3buf: ByteArray): Int
    external fun close()
}
