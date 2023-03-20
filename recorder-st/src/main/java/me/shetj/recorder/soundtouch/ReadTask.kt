
package me.shetj.recorder.soundtouch

internal class ReadTask(rawData: ShortArray, val readSize: Int) {
    val data: ShortArray = rawData.clone()
}
