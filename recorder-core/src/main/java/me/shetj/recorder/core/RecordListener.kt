package me.shetj.recorder.core

/**
 * 录音回调
 * @author shetj
 */
interface RecordListener {

    /**
     * 开始录音
     */
    fun onStart()

    /**
     * 重新
     */
    fun onResume()

    /**
     * 重置
     */
    fun onReset()

    /**
     * 正在录音
     * @param time 已经录制的时间
     * @param volume 当前声音大小
     */
    fun onRecording(time: Long, volume: Int)

    /**
     * 暂停
     */
    fun onPause()

    /**
     * 到达提醒时间，默认提醒时间是最大时间前10秒
     * @param mDuration
     */
    fun onRemind(mDuration: Long)

    /**
     * 录制成功
     * @param file 保存的文件
     * @param time 录制时间 （毫秒ms）
     */
    fun onSuccess(file: String, time: Long)


    /**
     * 设置最大进度条，触发 （毫秒ms）
     */
    fun setMaxProgress(time: Long)

    /**
     * 计算时间错误时
     */
    fun onError(e: Exception)

    /**
     * 达到“最大”时间，自动完成的操作
     * （毫秒ms）
     */
    fun autoComplete(file: String, time: Long)


}