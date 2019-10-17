package me.shetj.mixRecorder


import me.shetj.recorder.util.BytesTransUtil

class ReadMixTask(rawData: ByteArray, private val wax: Float) {
    private val rawData: ByteArray = rawData.clone()

    val data: ShortArray
        get() = BytesTransUtil.bytes2Shorts(BytesTransUtil.changeDataWithVolume(rawData, wax))

}