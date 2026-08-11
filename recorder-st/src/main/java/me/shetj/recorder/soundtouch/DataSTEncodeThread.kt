package me.shetj.recorder.soundtouch

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.BaseEncodeThread

/**
 * @param file       file
 * @param bufferSize bufferSize
 * @param isContinue 是否写在文件末尾,只支持lame格式的
 * @throws FileNotFoundException file not found
 */
internal class DataSTEncodeThread @Throws(FileNotFoundException::class)
constructor(
    private val file: File,
    bufferSize: Int,
    isContinue: Boolean,
    private val is2CHANNEL: Boolean,
    private val soundTouchKit: SoundTouchKit,
    isEnableVBR: Boolean,
    lameUtils: LameUtils
) :
    BaseEncodeThread(file, bufferSize, isContinue, isEnableVBR, "DataSTEncodeThread",lameUtils) {
    private val mSTBuffer = ShortArray(7200 + (bufferSize.toDouble() * 2.0).toInt()) // 处理变音的后的数据
    private val mTasks = Collections.synchronizedList(ArrayList<ReadTask>())

    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    override fun processData(): Int {
        if (mTasks.isNotEmpty()) {
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

    private fun processBuffer(pcm: ShortArray, size: Int): Int {
        val pcmData = beforePCMtoMP3(pcm)
        val encodedSize: Int
        val readSize: Int
        if (is2CHANNEL) {
            readSize = size / 2
            encodedSize = lameUtils.encodeInterleaved(pcmData, readSize, mMp3Buffer)
        } else {
            readSize = size
            encodedSize = lameUtils.encode(pcmData, pcmData, readSize, mMp3Buffer)
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
    override fun flushAndRelease() {
        soundTouchKit.flush(mSTBuffer)
        super.flushAndRelease()
    }

    private fun checkCut() {
        if (needUpdate) {
            val flushResult = lameUtils.flush(mMp3Buffer)
            if (flushResult > 0) {
                mFileOutputStream!!.write(mMp3Buffer, 0, flushResult)
            }
            mFileOutputStream?.close()
            mFileOutputStream = null
            mFileOutputStream = FileOutputStream(path, isContinue)
            if (isEnableVBR) {
                lameUtils.writeVBRHeader(path)
            }
            needUpdate = false
        }
    }

    override fun addTask(rawData: ByteArray, wax: Float, bgData: ByteArray?, bgWax: Float, mute: Boolean) {

    }

    override fun addTask(rawData: ShortArray, readSize: Int, mute: Boolean) {
        mTasks.add(ReadTask(rawData.clone(), readSize, mute))
    }
}
