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
package me.shetj.recorder.soundtouch

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
import kotlin.collections.ArrayList
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.FileUtils

/**
 * @param file       file
 * @param bufferSize bufferSize
 * @param isContinue 是否写在文件末尾
 * @throws FileNotFoundException file not found
 */
internal class DataSTEncodeThread @Throws(FileNotFoundException::class)
constructor(
    file: File,
    bufferSize: Int,
    isContinue: Boolean,
    private val is2CHANNEL: Boolean,
    private val soundTouchKit: SoundTouchKit
) :
    HandlerThread("DataSTEncodeThread"),
    AudioRecord.OnRecordPositionUpdateListener {
    private var mHandler: StopHandler? = null
    private val mMp3Buffer: ByteArray
    private var mFileOutputStream: FileOutputStream?
    private var path: String
    private var needUpdate = false
    private val mSTBuffer = ShortArray(7200 + (bufferSize.toDouble() * 2.0).toInt()) // 处理变音的后的数据
    private val mTasks = Collections.synchronizedList(ArrayList<ReadTask>())

    private class StopHandler(looper: Looper, private val encodeThread: DataSTEncodeThread) :
        Handler(looper) {

        override fun handleMessage(msg: Message) {
            if (msg.what == PROCESS_STOP) {
                // 处理缓冲区中的数据
                while (encodeThread.processData() > 0);
                removeCallbacksAndMessages(null)
                encodeThread.flushAndRelease()
                looper.quit()
            } else if (msg.what == PROCESS_ERROR) {
                // 处理缓冲区中的数据
                while (encodeThread.processData() > 0);
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

    fun getEncodeHandler(): Handler? {
        return mHandler
    }

    fun sendStopMessage() {
        mHandler?.sendEmptyMessage(PROCESS_STOP)
    }

    fun sendErrorMessage() {
        mHandler?.sendEmptyMessage(PROCESS_ERROR)
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
    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    private fun processData(): Int {
        if (mTasks.size > 0) {
            val task = mTasks.removeAt(0)
            // 处理变音，如果需要变音，仅需要得到变音后的数据，以及长度
            return if (soundTouchKit.isUse()) {
                var processSamples: Int
                soundTouchKit.putSamples(task.getData(), task.readSize)
                do {
                    processSamples = soundTouchKit.receiveSamples(mSTBuffer)
                    if (processSamples != 0) {
                        processBuffer(mSTBuffer, processSamples)
                    }
                } while (processSamples != 0)
                1
            } else {
                processBuffer(task.getData(), task.readSize)
            }
        }
        return 0
    }

    private fun processBuffer(stTask: ShortArray, size: Int): Int {
        val encodedSize: Int
        val readSize: Int
        if (is2CHANNEL) {
            readSize = size / 2
            encodedSize = LameUtils.encodeInterleaved(stTask, readSize, mMp3Buffer)
        } else {
            readSize = size
            encodedSize = LameUtils.encode(stTask, stTask, readSize, mMp3Buffer)
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

    /**
     * Flush all data left in lame buffer to file
     */
    private fun flushAndRelease() {
        soundTouchKit.flush(mSTBuffer)
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
            needUpdate = false
        }
    }

    fun addTask(rawData: ShortArray, readSize: Int, mute: Boolean) {
        mTasks.add(ReadTask(rawData, readSize, mute))
    }

    companion object {
        private const val PROCESS_STOP = 1
        private const val PROCESS_ERROR = 2
    }
}
