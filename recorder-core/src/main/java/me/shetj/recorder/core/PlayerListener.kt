
package me.shetj.recorder.core

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2018/10/24 0024<br></br>
 * **@company：**<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**<br></br>
 */
interface PlayerListener {

    /**
     * 开始播放
     * @param url 播放路径
     * @param duration 最大时间
     */
    fun onStart(duration: Int)

    /**
     * 暂停
     */
    fun onPause()

    /**
     * 继续播放
     */
    fun onResume()

    /**
     * 停止播放
     */
    fun onStop()

    /**
     * 播放结束
     */
    fun onCompletion()

    /**
     * 错误
     * @param throwable 异常信息
     */
    fun onError(throwable: Exception?)

    /**
     * 进度条
     * @param current 当前播放位置
     * @param duration 一共
     */
    fun onProgress(current: Int, duration: Int)
}
