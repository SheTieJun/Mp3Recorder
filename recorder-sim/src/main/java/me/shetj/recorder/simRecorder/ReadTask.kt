
package me.shetj.recorder.simRecorder


internal class ReadTask(private val data: ShortArray, val readSize: Int, private val mute:Boolean) {

    fun getData():ShortArray{
        if (mute){
            data.fill(0)
        }
        return data
    }
}
