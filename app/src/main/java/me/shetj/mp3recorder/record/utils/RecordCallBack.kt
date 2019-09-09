package me.shetj.mp3recorder.record.utils

/**
 * 录音回调
 * @author shetj
 */
interface RecordCallBack {

    /**
     * 开始/重新 录音
     */
    fun start()

    /**
     * 正在录音
     */
    fun onRecording(time: Int, volume: Int)

    /**
     * 暂停
     */
    fun pause()

    /**
     * 录制成功
     */
    fun onSuccess(file: String, time: Int)

    /**
     * 返回录制时间长，每一秒触发一次
     * @param time
     */
    fun onProgress(time: Int)

    /**
     * 设置最大进度条，触发
     */
    fun onMaxProgress(time: Int)

    /**
     * 计算时间错误时
     */
    fun onError(e: Exception)

    /**
     * 时间到了自动完成除非的操作
     */
    fun autoComplete(file: String, time: Int)

    /**
     * 触发回去权限
     */
    fun needPermission()

}