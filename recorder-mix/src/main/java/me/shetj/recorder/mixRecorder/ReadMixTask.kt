
package me.shetj.recorder.mixRecorder

import android.util.Log
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.BytesTransUtil

internal class ReadMixTask(
    private val rawData: ByteArray, // 录制的人声音
    private val wax: Float, // 人声音增强
    private val bgData: ByteArray?, // 录制的背景音乐 可能没有
    private val bgWax: Float // 背景声音降低
) {

    fun getData(): ShortArray {
        val mixBuffer = mixBuffer(rawData, bgData) ?: return BytesTransUtil.bytes2Shorts(
            BytesTransUtil.changeDataWithVolume(rawData, wax)
        )
        return BytesTransUtil.bytes2Shorts(BytesTransUtil.changeDataWithVolume(mixBuffer, wax))
    }

    /**
     * 混合 音频,
     */
    private fun mixBuffer(buffer: ByteArray, bgData: ByteArray?): ByteArray? {
        try {
            if (bgData != null) {
                // 如果有背景音乐
                val bytes = BytesTransUtil.changeDataWithVolume(
                    bgData,
                    bgWax
                )
                return BytesTransUtil.averageMix(arrayOf(buffer, bytes))
            }
            return buffer
        } catch (e: Exception) {
            Log.e(BaseRecorder.TAG, "mixBuffer error : ${e.message}")
            return buffer
        }
    }
}
