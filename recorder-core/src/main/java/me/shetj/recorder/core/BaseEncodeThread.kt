package me.shetj.recorder.core

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import java.io.File
import java.io.FileOutputStream
import me.shetj.ndk.lame.LameUtils
import java.io.IOException

/**
 *
 * <b>@author：</b> shetj<br>
 * <b>@createTime：</b> 2023/3/22<br>
 *
 * @param file 输出的 mp3 文件
 * @param bufferSize PCM 缓冲区大小，用来估算 mp3 缓冲
 * @param isContinue 是否在已有文件末尾续写
 * @param isEnableVBR 是否在结束时写入 VBR 头
 * @param name 编码线程名
 * @param lameUtils lame 编码器
 * @param baseRecorder 所属录音器，写文件失败时把错误回传给用户回调
 */
abstract class BaseEncodeThread(
    file: File,
    bufferSize: Int,
    var isContinue: Boolean,
    protected val isEnableVBR: Boolean,
    name: String,
    val lameUtils: LameUtils,
    protected val baseRecorder: BaseRecorder,
) : HandlerThread(name), AudioRecord.OnRecordPositionUpdateListener {
    protected var path: String
    protected var mFileOutputStream: FileOutputStream?
    protected val mMp3Buffer: ByteArray
    protected var needUpdate = false
    protected var mPCMListener: PCMListener? = null


    init {
        this.mFileOutputStream = FileOutputStream(file, isContinue)
        path = file.absolutePath
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
                // 错误收尾：不再继续写文件，释放编码器后删除损坏文件
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

    open fun beforePCMtoMP3(pcm: ShortArray): ShortArray {
        if (mPCMListener == null) return pcm
        return mPCMListener!!.onBeforePCMToMp3(pcm)
    }


    abstract fun addTask(rawData: ByteArray, wax: Float, bgData: ByteArray?, bgWax: Float, mute: Boolean)


    abstract fun addTask(rawData: ShortArray, readSize: Int, mute: Boolean)

    open fun sendStopMessage() {
        mHandler?.sendEmptyMessage(PROCESS_STOP)
    }

    open fun sendErrorMessage() {
        mHandler?.sendEmptyMessage(PROCESS_ERROR)
    }

    /**
     * 将 lame 编码后的 mp3 数据写入文件
     * @param encodedSize 本次需要写入的字节数
     * @return 写入成功返回 true；失败时已回调错误并返回 false
     */
    protected fun writeMp3Buffer(encodedSize: Int): Boolean {
        if (encodedSize <= 0) {
            return true
        }
        if (baseRecorder.hasNotifyError) {
            return false
        }
        return try {
            mFileOutputStream!!.write(mMp3Buffer, 0, encodedSize)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            onWriteError(e)
            false
        }
    }

    /**
     * 写文件失败：通知 PCM 监听，并让录音器进入错误状态
     * @param e 写入失败的 IO 异常
     */
    protected fun onWriteError(e: IOException) {
        baseRecorder.onError(e)
    }

    open fun flushAndRelease() {
        // 将MP3结尾信息写入buffer中
        val flushResult = lameUtils.flush(mMp3Buffer)
        if (flushResult > 0 && !baseRecorder.hasNotifyError) {
            writeMp3Buffer(flushResult)
        }
        try {
            mFileOutputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            mFileOutputStream = null
            if (isEnableVBR && !baseRecorder.hasNotifyError) {
                lameUtils.writeVBRHeader(path)
            }
            lameUtils.close()
        }
    }

    protected abstract fun processData(): Int


    open fun setPCMListener(pcmListener: PCMListener?) {
        mPCMListener = pcmListener
    }

    companion object {
        const val PROCESS_STOP = 1
        const val PROCESS_ERROR = 2
    }
}
