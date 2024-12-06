package me.shetj.recorder.mixRecorder

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.BaseEncodeThread

/**
 * Constructor
 *
 * @param file       file
 * @param bufferSize bufferSize
 * @param isContinue 是否写在文件末尾
 * @throws FileNotFoundException file not found
 */
internal class MixEncodeThread
@Throws(FileNotFoundException::class)
constructor(file: File, bufferSize: Int, isContinue: Boolean, private val is2CHANNEL: Boolean, isEnableVBR: Boolean) :
    BaseEncodeThread(file, bufferSize, isContinue, isEnableVBR, "MixEncodeThread") {

    private val mTasks = Collections.synchronizedList(ArrayList<ReadMixTask>())
    private val mOldTasks = Collections.synchronizedList(ArrayList<ReadMixTask>())

    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    override fun processData(): Int {
        if (mTasks.size > 0) {
            val task = mTasks.removeAt(0)
            addOldData(task)
            val buffer = beforePCMtoMP3(task.getData())
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
    override fun flushAndRelease() {
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
                        mFileOutputStream?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                mOldTasks.clear()
                if (isEnableVBR) {
                    LameUtils.writeVBRHeader(path)
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
            mFileOutputStream = FileOutputStream(path, isContinue)
            while (setOldDateToFile() > 0);
            needUpdate = false
        }
    }

    private fun setOldDateToFile(): Int {
        if (mOldTasks.size > 0 && mFileOutputStream != null) {
            val task = mOldTasks.removeAt(0)
            val buffer = beforePCMtoMP3(task.getData())
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

    /**
     * 为什么要这么做，因为分割路由功能的时候直接分割的
     * 系统自带的播放器会漏字
     */
    private fun addOldData(task: ReadMixTask) {
        if (mOldTasks.size > 10) {
            // 自己调整数量多少合适，我写的是10
            mOldTasks.removeAt(0)
        }
        mOldTasks.add(task)
    }

    override fun addTask(rawData: ByteArray, wax: Float, bgData: ByteArray?, bgWax: Float, mute: Boolean) {
        mTasks.add(ReadMixTask(rawData.clone(), wax, bgData, bgWax, mute))
    }

    override fun addTask(rawData: ShortArray, readSize: Int, mute: Boolean) {
    }
}
