package me.shetj.mp3recorder.record.utils

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2018/10/24 0024<br></br>
 * **@company：**<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**<br></br>
 */
class EasyPlayerListener : SPlayerListener {

    override val isLoop: Boolean
        get() = false

    override fun onStart(url: String) {

    }

    override fun onPause() {

    }

    override fun onResume() {

    }

    override fun onStop() {

    }

    override fun onCompletion() {

    }

    override fun onError(throwable: Throwable) {

    }

    override fun isNext(mp: MediaPlayerUtils): Boolean {

        return false
    }

    override fun onProgress(current: Int, size: Int) {}


}
