
package me.shetj.recorder.simRecorder

internal class ReadTask(rawData: ShortArray, val readSize: Int) {
    val data: ShortArray = rawData.clone()
}
