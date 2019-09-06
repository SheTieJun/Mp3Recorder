package me.shetj.player

import android.util.Log

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2019/6/21<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**  <br></br>
 */
class SimRecordListener : RecordListener, PermissionListener {
    override fun needPermission() {
        Log.d("SimRecordListener", "needPermission")
    }

    override fun onStart() {
        Log.d("SimRecordListener", "onStart")
    }

    override fun onResume() {
        Log.d("SimRecordListener", "onResume")
    }

    override fun onReset() {
        Log.d("SimRecordListener", "needPermission")
    }

    override fun onRecording(time: Long, volume: Int) {
        Log.d("SimRecordListener", "onRecording")
    }

    override fun onPause() {
        Log.d("SimRecordListener", "onPause")
    }

    override fun onRemind(mDuration: Long) {
        Log.d("SimRecordListener", "onRemind")
    }

    override fun onSuccess(file: String, time: Long) {
        Log.d("SimRecordListener", "onSuccess")
    }

    override fun setMaxProgress(time: Long) {
        Log.d("SimRecordListener", "setMaxProgress")
    }

    override fun onError(e: Exception) {
        Log.d("SimRecordListener", "onError")
    }

    override fun autoComplete(file: String, time: Long) {
        Log.d("SimRecordListener", "autoComplete")
    }
}
