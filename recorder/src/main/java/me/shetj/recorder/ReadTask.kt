package me.shetj.recorder

class ReadTask(rawData: ShortArray, val readSize: Int) {
    val data: ShortArray = rawData.clone()
}