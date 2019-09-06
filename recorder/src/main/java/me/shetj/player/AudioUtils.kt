package me.shetj.player

object AudioUtils {

    /**
     * 对声音进行变化
     * @param audioSamples
     * @param volume
     * @return
     */
    fun adjustVolume(audioSamples: ByteArray, volume: Float): ByteArray {
        val array = ByteArray(audioSamples.size)
        var i = 0
        while (i < array.size) {
            // convert byte pair to int
            var buf1 = audioSamples[i + 1].toInt()
            var buf2 = audioSamples[i].toInt()
            buf1 = (buf1 and 0xff).shl(8)
            buf2 = (buf2 and 0xff)
            var res = (buf1 or buf2)
            res = (res * volume).toInt()
            // convert back
            array[i] = res.toByte()
            array[i + 1] = (res shr 8).toByte()
            i += 2
        }
        return array
    }

}
