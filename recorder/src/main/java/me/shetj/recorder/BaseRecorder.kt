package me.shetj.recorder

import kotlin.math.sqrt


abstract class BaseRecorder {

    protected var mVolume: Int = 0
    /**
     * @return 当前声音大小
     */
    abstract val realVolume: Int
    /**
     * 此计算方法来自samsung开发范例
     *
     * @param buffer   buffer
     * @param readSize readSize
     */
    protected fun calculateRealVolume(buffer: ShortArray, readSize: Int) {
        var sum = 0.0
        for (i in 0 until readSize) {
            // 这里没有做运算的优化，为了更加清晰的展示代码
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        if (readSize > 0) {
            val amplitude = sum / readSize
            mVolume = sqrt(amplitude).toInt()
        }
    }

}
