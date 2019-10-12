package me.shetj.recorder

import me.shetj.recorder.util.BytesTransUtil
import kotlin.math.sqrt


abstract class BaseRecorder {

    protected var mVolume: Int = 0
    /**
     * @return 当前声音大小 db
     */
    abstract val realVolume: Int

    protected fun calculateRealVolume(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        if (readSize > 0) {
            val amplitude = sum / readSize
            mVolume = sqrt(amplitude).toInt()
            if (mVolume < 5) {
                for (i in 0 until readSize) {
                    buffer[i] = 0
                }
            }
        }

    }
    protected fun calculateRealVolume(buffer: ByteArray) {
        val shorts = BytesTransUtil.bytes2Shorts(buffer)
        val readSize = shorts.size
        calculateRealVolume(shorts,readSize)
    }
}
