package me.shetj.mixRecorder

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import me.shetj.recorder.util.FileUtils
import me.shetj.recorder.util.LameUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MixEncodeThread
/**
 * Constructor
 *
 * @param file       file
 * @param bufferSize bufferSize
 * @param isContinue 是否写在文件末尾
 * @throws FileNotFoundException file not found
 */
@Throws(FileNotFoundException::class)
constructor(file: File, bufferSize: Int, isContinue: Boolean, private val is2CHANNEL: Boolean) :
    HandlerThread("DataEncodeThread"), AudioRecord.OnRecordPositionUpdateListener {
    private var mHandler: StopHandler? = null
    private val mMp3Buffer: ByteArray
    private val mFileOutputStream: FileOutputStream?
    private val path: String

    val handler: Handler?
        get() {
            check()
            return mHandler
        }

    private val mTasks = Collections.synchronizedList(ArrayList<ReadMixTask>())

    private class StopHandler(looper: Looper, private val encodeThread: MixEncodeThread) :
        Handler(looper) {

        override fun handleMessage(msg: Message) {
            if (msg.what == PROCESS_STOP) {
                //处理缓冲区中的数据
                while (encodeThread.processData() > 0);
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null)
                encodeThread.flushAndRelease()
                looper.quit()
            } else if (msg.what == PROCESS_ERROR) {
                //处理缓冲区中的数据
                while (encodeThread.processData() > 0);
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null)
                encodeThread.flushAndRelease()
                looper.quit()
                FileUtils.deleteFile(encodeThread.path)
            }
        }
    }

    init {
        this.mFileOutputStream = FileOutputStream(file, isContinue)
        path = file.absolutePath
        mMp3Buffer = ByteArray((7200 + bufferSize.toDouble() * 2.0 * 1.25).toInt())
    }

    @Synchronized
    override fun start() {
        super.start()
        mHandler = StopHandler(looper, this)
    }

    private fun check() {
        checkNotNull(mHandler)
    }

    fun sendStopMessage() {
        check()
        mHandler!!.sendEmptyMessage(PROCESS_STOP)
    }

    fun sendErrorMessage() {
        check()
        mHandler!!.sendEmptyMessage(PROCESS_ERROR)
    }

    override fun onMarkerReached(recorder: AudioRecord) {
        // Do nothing
    }

    override fun onPeriodicNotification(recorder: AudioRecord) {
        processData()
    }

    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    private fun processData(): Int {
        if (mTasks.size > 0) {
            val task = mTasks.removeAt(0)
            val buffer = task.getData()
            val encodedSize: Int
            val readSize: Int
            if (is2CHANNEL) {
                readSize = buffer.size / 2
                //因为是双声道所有要对录制的声音进行左右区分
                val leftChannelAudioData = ShortArray(readSize)
                val rightChannelAudioData = ShortArray(readSize)
                //readSize -1 因为有可能 i+1超过了
                var i = 0
                while (i < readSize - 1) {
                    leftChannelAudioData[i] = buffer[2 * i]
                    leftChannelAudioData[i + 1] = buffer[2 * i + 1]
                    rightChannelAudioData[i] = buffer[2 * i + 2]
                    rightChannelAudioData[i + 1] = buffer[2 * i + 3]
                    i += 2
                }
                encodedSize = LameUtils.encode(
                    leftChannelAudioData,
                    rightChannelAudioData,
                    readSize,
                    mMp3Buffer
                )
            } else {
                readSize = buffer.size
                encodedSize = LameUtils.encode(buffer, buffer, readSize, mMp3Buffer)
            }
            if (encodedSize > 0) {
                try {
                    mFileOutputStream!!.write(mMp3Buffer, 0, encodedSize)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return readSize
        }
        return 0
    }

    /**
     * Flush all data left in lame buffer to file
     */
    private fun flushAndRelease() {
        //将MP3结尾信息写入buffer中
        val flushResult = LameUtils.flush(mMp3Buffer)
        if (flushResult > 0) {
            try {
                mFileOutputStream!!.write(mMp3Buffer, 0, flushResult)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
                LameUtils.close()
            }
        }
    }

    fun addTask(rawData: ByteArray, wax: Float,bgData: ByteArray?, bgWax: Float) {
        mTasks.add(ReadMixTask(rawData, wax,bgData,bgWax))
    }

    companion object {
        private val PROCESS_STOP = 1
        private val PROCESS_ERROR = 2
    }


}
