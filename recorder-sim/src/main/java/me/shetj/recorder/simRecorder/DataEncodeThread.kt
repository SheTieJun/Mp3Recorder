package me.shetj.recorder.simRecorder

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.BaseEncodeThread
import me.shetj.recorder.core.BaseRecorder

/**
 * @param file 输出的 mp3 文件
 * @param bufferSize PCM 缓冲区大小
 * @param isContinue 是否写在文件末尾
 * @param is2CHANNEL 是否双声道交织编码
 * @param isEnableVBR 是否写入 VBR 头
 * @param lameUtils lame 编码器
 * @param baseRecorder 所属录音器，写文件失败时回传错误
 * @throws FileNotFoundException file not found
 */
internal class DataEncodeThread @Throws(FileNotFoundException::class)
constructor(
    file: File,
    bufferSize: Int,
    isContinue: Boolean,
    private val is2CHANNEL: Boolean,
    isEnableVBR: Boolean,
    lameUtils: LameUtils,
    baseRecorder: BaseRecorder,
) :
    BaseEncodeThread(file, bufferSize, isContinue, isEnableVBR, "DataEncodeThread", lameUtils, baseRecorder) {

    private val mTasks = Collections.synchronizedList(ArrayList<ReadTask>())
    private val mOldTasks = Collections.synchronizedList(ArrayList<ReadTask>())


    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    override fun processData(): Int {
        if (mTasks.isNotEmpty()) {
            val task = mTasks.removeAt(0)
            addOldData(task)
            val buffer = beforePCMtoMP3(task.getData())
            val encodedSize: Int
            val readSize: Int
            if (is2CHANNEL) {
                readSize = buffer.size / 2
                encodedSize = lameUtils.encodeInterleaved(buffer, readSize, mMp3Buffer)
            } else {
                readSize = buffer.size
                encodedSize = lameUtils.encode(buffer, buffer, readSize, mMp3Buffer)
            }
            if (encodedSize > 0 && writeMp3Buffer(encodedSize)) {
                checkCut()
            }
            return readSize
        }
        return 0
    }

    override fun flushAndRelease() {
        super.flushAndRelease()
        mOldTasks.clear()
    }

    private fun checkCut() {
        if (needUpdate) {
            val flushResult = lameUtils.flush(mMp3Buffer)
            if (flushResult > 0 && !writeMp3Buffer(flushResult)) {
                return
            }
            try {
                mFileOutputStream?.close()
                mFileOutputStream = null
                mFileOutputStream = FileOutputStream(path, isContinue)
            } catch (e: IOException) {
                onWriteError(e)
                return
            }
            if (isEnableVBR) {
                lameUtils.writeVBRHeader(path)
            }
            while (setOldDateToFile() > 0)
                needUpdate = false
        }
    }

    private fun setOldDateToFile(): Int {
        if (mOldTasks.isNotEmpty() && mFileOutputStream != null) {
            val task = mOldTasks.removeAt(0)
            val buffer = beforePCMtoMP3(task.getData())
            val encodedSize: Int
            val readSize: Int
            if (is2CHANNEL) {
                readSize = buffer.size / 2
                encodedSize = lameUtils.encodeInterleaved(buffer, readSize, mMp3Buffer)
            } else {
                readSize = buffer.size
                encodedSize = lameUtils.encode(buffer, buffer, readSize, mMp3Buffer)
            }
            if (encodedSize > 0) {
                writeMp3Buffer(encodedSize)
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

    override fun addTask(rawData: ByteArray, wax: Float, bgData: ByteArray?, bgWax: Float,mute:Boolean) {

    }

    override fun addTask(rawData: ShortArray, readSize: Int,mute:Boolean) {
        mTasks.add(ReadTask(rawData.clone(), readSize,mute))
    }
}
