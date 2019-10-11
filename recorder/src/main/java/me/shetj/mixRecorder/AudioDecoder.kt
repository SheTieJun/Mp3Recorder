package me.shetj.mixRecorder

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.util.*
import javax.sql.RowSetInternal

class AudioDecoder {

    private val chunkPCMDataContainer = ArrayList<PCM>()//PCM数据块容器
    private var mediaExtractor: MediaExtractor? = null
    private var mediaDecode: MediaCodec? = null
    private var decodeInputBuffers: Array<ByteBuffer>? = null
    private var decodeOutputBuffers: Array<ByteBuffer>? = null
    private var decodeBufferInfo: MediaCodec.BufferInfo? = null

    internal var isPCMExtractorEOS = true
        private set

    private var mp3FilePath: String? = null

    var mediaFormat: MediaFormat? = null
        private set

    //记得加锁
    //每次取出index 0 的数据
    //取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
    val pcmData: PCM?
        get() = synchronized(lockPCM) {
            if (chunkPCMDataContainer.isEmpty()) {
                return null
            }
            val pcmChunk = chunkPCMDataContainer[0]
            chunkPCMDataContainer.removeAt(0)
            return pcmChunk
        }

    /**
     * 测试时发现 播放音频的 MediaCodec.BufferInfo.size 是变换的
     */
    internal//记得加锁
    val bufferSize: Int
        get() = synchronized(lockPCM) {
            if (chunkPCMDataContainer.isEmpty()) {
                return BUFFER_SIZE
            }
            val pcm = chunkPCMDataContainer[0]
            return pcm?.bufferSize ?: BUFFER_SIZE
        }

    fun setMp3FilePath(path: String): AudioDecoder {
        mp3FilePath = path
        return this
    }

    fun startPcmExtractor(): AudioDecoder {
        initMediaDecode()
        Thread(Runnable { srcAudioFormatToPCM() }).start()
        return this
    }

    fun release(): AudioDecoder {
        isPCMExtractorEOS = true
        chunkPCMDataContainer.clear()
        return this
    }

    private fun initMediaDecode() {
        while (!isPCMExtractorEOS) {
            try {
                Thread.sleep(80)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
        try {
            mediaExtractor = MediaExtractor()//此类可分离视频文件的音轨和视频轨道
            Log.i("mixRecorder", "mp3FilePath = " + mp3FilePath!!)
            mediaExtractor!!.setDataSource(mp3FilePath!!)//媒体文件的位置
            mediaFormat = mediaExtractor!!.getTrackFormat(0)
            val mime = mediaFormat!!.getString(MediaFormat.KEY_MIME)
            // 检查是否为音频文件
            if (!mime!!.startsWith("audio/")) {
                Log.e("mixRecorder", "不是音频文件!")
                return
            }
            //选择此音频轨道，因为是音频只有一条
            mediaExtractor!!.selectTrack(0)
            //创建Decode解码器
            mediaDecode = MediaCodec.createDecoderByType(mime)
            mediaDecode!!.configure(mediaFormat, null, null, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("mixRecorder", "error :: " + e.message)
            return
        }

        if (mediaDecode == null) {
            Log.e("mixRecorder", "create mediaDecode failed")
            return
        }
        mediaDecode!!.start()//启动MediaCodec ，等待传入数据
        //MediaCodec在此ByteBuffer[]中获取输入数据
        decodeInputBuffers = mediaDecode!!.inputBuffers
        //MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        decodeOutputBuffers = mediaDecode!!.outputBuffers

        decodeBufferInfo = MediaCodec.BufferInfo()//用于描述解码得到的byte[]数据的相关信息
    }

    private fun putPCMData(pcmChunk: ByteArray, bufferSize: Int,time: Long) {
        synchronized(lockPCM) {
            //记得加锁
            chunkPCMDataContainer.add(PCM(pcmChunk, bufferSize,time))
        }
    }

    /**
     * 解码音频文件 得到PCM数据块
     */
    private fun srcAudioFormatToPCM() {

        isPCMExtractorEOS = false
        var sawInputEOS = false
        try {
            while (!isPCMExtractorEOS) {
                if (!sawInputEOS) {
                    val inputIndex =
                        mediaDecode!!.dequeueInputBuffer(-1)//获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
                    if (inputIndex >= 0) {
                        val inputBuffer = decodeInputBuffers!![inputIndex]//拿到inputBuffer
                        inputBuffer.clear()//清空之前传入inputBuffer内的数据
                        val sampleSize = mediaExtractor!!.readSampleData(
                            inputBuffer,
                            0
                        )//MediaExtractor读取数据到inputBuffer中
                        if (sampleSize < 0) {//小于0 代表所有数据已读取完成
                            sawInputEOS = true
                            mediaDecode!!.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        } else {
                            val presentationTimeUs = mediaExtractor!!.sampleTime
                            mediaDecode!!.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                presentationTimeUs,
                                0
                            )//通知MediaDecode解码刚刚传入的数据
                            mediaExtractor!!.advance()//MediaExtractor移动到下一取样处
                        }
                    }
                }

                //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
                //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
                val outputIndex = mediaDecode!!.dequeueOutputBuffer(decodeBufferInfo!!, 10000)
                if (outputIndex >= 0) {
                    // Simply ignore codec config buffers.
                    if (decodeBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        mediaDecode!!.releaseOutputBuffer(outputIndex, false)
                        continue
                    }

                    if (decodeBufferInfo!!.size != 0) {

                        val outBuf = decodeOutputBuffers!![outputIndex]//拿到用于存放PCM数据的Buffer

                        outBuf.position(decodeBufferInfo!!.offset)
                        outBuf.limit(decodeBufferInfo!!.offset + decodeBufferInfo!!.size)
                        val data = ByteArray(decodeBufferInfo!!.size)//BufferInfo内定义了此数据块的大小
                        outBuf.get(data)//将Buffer内的数据取出到字节数组中
                        //                        Log.i("mixRecorder","try put pcm data ...");
                        putPCMData(data, decodeBufferInfo!!.size,mediaExtractor!!.sampleTime)//自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
                    }

                    mediaDecode!!.releaseOutputBuffer(
                        outputIndex,
                        false
                    )//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据

                    if (decodeBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isPCMExtractorEOS = true
                        Log.i("mixRecorder", "pcm finished..." + mp3FilePath!!)
                    }

                } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    decodeOutputBuffers = mediaDecode!!.outputBuffers
                }
            }
        } finally {
            if (mediaDecode != null) {
                mediaDecode!!.release()
            }
            if (mediaExtractor != null) {
                mediaExtractor!!.release()
            }
        }
    }


    inner class PCM internal constructor(
        internal var bufferBytes: ByteArray,
        internal var bufferSize: Int,
        internal var time :Long
    )

    companion object {

        /**
         * 初始化解码器
         */
        private val lockPCM = Any()
        private val BUFFER_SIZE = 2048
    }
}
