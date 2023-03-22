package me.shetj.recorder.core

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import java.io.File
import java.io.FileOutputStream
import me.shetj.ndk.lame.LameUtils

/**
 *
 * <b>@author：</b> shetj<br>
 * <b>@createTime：</b> 2023/3/22<br>
 */
abstract class BaseEncodeThread(
    file: File, bufferSize: Int, var isContinue: Boolean, protected val isEnableVBR:Boolean, name: String
) : HandlerThread(name), AudioRecord.OnRecordPositionUpdateListener {
    protected var path: String
    protected var mFileOutputStream: FileOutputStream?
    protected val mMp3Buffer: ByteArray
    protected var needUpdate = false
    protected var mPCMListener: PCMListener? = null
    init {
        this.mFileOutputStream = FileOutputStream(file, isContinue)
        path = file.absolutePath
        if(isEnableVBR){
            LameUtils.writeVBRHeader(path)
        }
        mMp3Buffer = ByteArray((7200 + bufferSize.toDouble() * 2.0 * 1.25).toInt())
    }

    protected class StopHandler(looper: Looper, private val encodeThread: BaseEncodeThread) :
        Handler(looper) {

        override fun handleMessage(msg: Message) {
            if (msg.what == PROCESS_STOP) {
                // 处理缓冲区中的数据
                while (encodeThread.processData() > 0);
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null)
                encodeThread.flushAndRelease()
                looper.quit()
            } else if (msg.what == PROCESS_ERROR) {
                // 处理缓冲区中的数据
                while (encodeThread.processData() > 0);
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null)
                encodeThread.flushAndRelease()
                looper.quit()
                FileUtils.deleteFile(encodeThread.path)
            }
        }
    }

    protected var mHandler: StopHandler? = null


    fun getEncodeHandler(): Handler? {
        return mHandler
    }


    open fun update(outputFilePath: String) {
        this.path = outputFilePath
        needUpdate = true
    }

    @Synchronized
    override fun start() {
        super.start()
        mHandler = StopHandler(looper, this)
    }

    override fun onMarkerReached(p0: AudioRecord?) {

    }

    override fun onPeriodicNotification(p0: AudioRecord?) {
        processData()
    }

    open fun beforePCMtoMP3(pcm:ShortArray): ShortArray {
        if (mPCMListener == null) return pcm
       return mPCMListener!!.onBeforePCMToMp3(pcm)
    }


    abstract fun addTask(rawData: ByteArray, wax: Float, bgData: ByteArray?, bgWax: Float)


    abstract fun addTask(rawData: ShortArray, readSize: Int)

    open fun sendStopMessage() {
        mHandler?.sendEmptyMessage(PROCESS_STOP)
    }

    open fun sendErrorMessage() {
        mHandler?.sendEmptyMessage(PROCESS_ERROR)
    }

    protected abstract fun flushAndRelease()

    protected abstract fun processData(): Int


    open fun setPCMListener(pcmListener: PCMListener?) {
        mPCMListener = pcmListener
    }

    companion object {
        const val PROCESS_STOP = 1
        const val PROCESS_ERROR = 2
    }
}