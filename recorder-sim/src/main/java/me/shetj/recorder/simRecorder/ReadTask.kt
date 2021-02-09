package me.shetj.recorder.simRecorder

class ReadTask(rawData: ShortArray, val readSize: Int) {
    val data: ShortArray = rawData.clone()
}