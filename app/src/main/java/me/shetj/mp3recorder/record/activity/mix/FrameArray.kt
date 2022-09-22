package me.shetj.mp3recorder.record.activity.mix

/**
 * Created by chunsheng on 2019-08-21.
 * 构建一个容纳声波数据的 List
 */
class FrameArray( ) {

    private var rawFrameArray: ArrayList<Int> = ArrayList()

    fun add(frames: IntArray) {
        frames.map {
            rawFrameArray.add(it)
        }
    }

    fun add(frame: Int) {
        rawFrameArray.add(frame)
    }

    fun get(): IntArray {
        return rawFrameArray.toIntArray()
    }

    fun getSize(): Int {
        return rawFrameArray.size
    }

    /**
     * 删除指定的序列[fromIndex]包含，[toIndex]不包含
     */
    fun delete(fromIndex: Int, toIndex: Int) {
        if (fromIndex < 0 || toIndex >= rawFrameArray.size) {
            return
        }
        rawFrameArray.subList(fromIndex, toIndex).clear()
    }

    fun reset() {
        rawFrameArray.clear()
    }
}
