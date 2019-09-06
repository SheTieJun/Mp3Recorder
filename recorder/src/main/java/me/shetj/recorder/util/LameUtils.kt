package me.shetj.recorder.util

object LameUtils {
    init {
        System.loadLibrary("mp3lame")
    }
    external fun version(): String
    external fun init(
        inSamplerate: Int,
        inChannel: Int,
        outSamplerate: Int,
        outBitrate: Int,
        quality: Int
    )
    external fun encode(
        bufferLeft: ShortArray,
        bufferRight: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int
    external fun flush(mp3buf: ByteArray): Int
    external fun close()
}
