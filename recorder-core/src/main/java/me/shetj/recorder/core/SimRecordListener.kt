
package me.shetj.recorder.core

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2019/6/21<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**  <br></br>
 */
open class SimRecordListener : RecordListener, PermissionListener,PCMListener {
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

    override fun onRemind(duration: Long) {
    }

    override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
    }

    override fun onMaxChange(time: Long) {
    }

    override fun onError(e: Exception) {
    }

    override fun onMuteRecordChange(mute: Boolean) {
    }
}
