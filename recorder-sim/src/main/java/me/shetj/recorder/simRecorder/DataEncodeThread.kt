/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.recorder.simRecorder

import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.FileUtils

/**
 * @param file       file
 * @param bufferSize bufferSize
 * @param isContinue 是否写在文件末尾
 * @throws FileNotFoundException file not found
 */
internal class DataEncodeThread @Throws(FileNotFoundException::class)
constructor(file: File, bufferSize: Int, isContinue: Boolean, private val is2CHANNEL: Boolean) :
    HandlerThread("DataEncodeThread"),
    AudioRecord.OnRecordPositionUpdateListener {
    private var mHandler: StopHandler? = null
    private val mMp3Buffer: ByteArray
    private var mFileOutputStream: FileOutputStream?
    private var path: String
    private var needUpdate = false

    val handler: Handler?
        get() {
            check()
            return mHandler
        }

    private val mTasks = Collections.synchronizedList(ArrayList<ReadTask>())
    private val mOldTasks = Collections.synchronizedList(ArrayList<ReadTask>())

    private class StopHandler(looper: Looper, private val encodeThread: DataEncodeThread) :
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

    fun update(outputFilePath: String) {
        this.path = outputFilePath
        needUpdate = true
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
            addOldData(task)
            val buffer = task.getData()
            val encodedSize: Int
            val readSize: Int
            if (is2CHANNEL) {
                readSize = buffer.size / 2
                encodedSize = LameUtils.encodeInterleaved(buffer, readSize, mMp3Buffer)
            } else {
                readSize = buffer.size
                encodedSize = LameUtils.encode(buffer, buffer, readSize, mMp3Buffer)
            }
            if (encodedSize > 0) {
                try {
                    mFileOutputStream!!.write(mMp3Buffer, 0, encodedSize)
                    checkCut()
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
        // 将MP3结尾信息写入buffer中
        val flushResult = LameUtils.flush(mMp3Buffer)
        if (flushResult > 0) {
            try {
                mFileOutputStream!!.write(mMp3Buffer, 0, flushResult)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                LameUtils.close()
            }
        }
    }

    private fun checkCut() {
        if (needUpdate) {
            val flushResult = LameUtils.flush(mMp3Buffer)
            if (flushResult > 0) {
                mFileOutputStream!!.write(mMp3Buffer, 0, flushResult)
            }
            mFileOutputStream?.close()
            mFileOutputStream = null
            mFileOutputStream = FileOutputStream(path, true)
            while (setOldDateToFile() > 0)
                needUpdate = false
        }
    }

    private fun setOldDateToFile(): Int {
        if (mOldTasks.size > 0 && mFileOutputStream != null) {
            val task = mOldTasks.removeAt(0)
            val buffer = task.getData()
            val encodedSize: Int
            val readSize: Int
            if (is2CHANNEL) {
                readSize = buffer.size / 2
                encodedSize = LameUtils.encodeInterleaved(buffer, readSize, mMp3Buffer)
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

    private fun addOldData(task: ReadTask) {
        if (mOldTasks.size > 10) {
            // 自己调整数量多少合适，我写的是10
            mOldTasks.removeAt(0)
        }
        mOldTasks.add(task)
    }

    fun addTask(rawData: ShortArray, readSize: Int, mute: Boolean) {
        mTasks.add(ReadTask(rawData.clone(), readSize, mute))
    }

    companion object {
        private const val PROCESS_STOP = 1
        private const val PROCESS_ERROR = 2
    }
}
