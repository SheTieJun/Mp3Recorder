package me.shetj.player

import android.util.Log

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2019/6/21<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**  <br></br>
 */
open class SimRecordListener : RecordListener, PermissionListener {
    override fun needPermission() {
    }

    override fun onStart() {
    }

    override fun onResume() {
    }

    override fun onReset() {
    }

    override fun onRecording(time: Long, volume: Int) {
    }

    override fun onPause() {
    }

    override fun onRemind(mDuration: Long) {
    }

    override fun onSuccess(file: String, time: Long) {
    }

    override fun setMaxProgress(time: Long) {
    }

    override fun onError(e: Exception) {
    }

    override fun autoComplete(file: String, time: Long) {
    }
}
