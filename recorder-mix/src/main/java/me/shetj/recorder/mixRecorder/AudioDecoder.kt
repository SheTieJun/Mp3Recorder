
package me.shetj.recorder.mixRecorder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log

internal class AudioDecoder {

    private val chunkPCMDataContainer = ArrayList<PCM>() // PCM数据块容器
    private var mediaExtractor: MediaExtractor? = null
    private var mediaDecode: MediaCodec? = null
    private var decodeBufferInfo: MediaCodec.BufferInfo? = null

    internal var isPCMExtractorEOS = true
        private set

    private var mp3FilePath: String? = null

    private var context: Context? = null
    private var mp3URi: Uri? = null

    private var headers: MutableMap<String, String>? = null

    internal var mediaFormat: MediaFormat? = null
        private set

    // 记得加锁
    // 每次取出index 0 的数据
    // 取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
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
     * 会出现不一样的数据
     */
    internal
    val bufferSize: Int
        get() = synchronized(lockPCM) {
            if (chunkPCMDataContainer.isEmpty()) {
                return BUFFER_SIZE
            }
            return chunkPCMDataContainer[0].bufferSize
        }

    fun setMp3FilePath(path: String): AudioDecoder {
        mp3FilePath = path
        if (path.startsWith("http")) {
            Log.w(
                "mixRecorder",
                "the url  may be HTTP ,APP could ANR by MediaExtractor," +
                    "you should download it to use better than direct to use\n如果使用HTTP的url, APP可能ANR," +
                    "因为mediaExtractor实在主线程初始化的" +
                    "你应该下载它后再用，会比只直接用url好很多"
            )
        }
        mp3URi = null
        context = null
        return this
    }

    fun setMp3FilePath(
        context: Context,
        uri: Uri,
        headers: MutableMap<String, String>?
    ): AudioDecoder {
        this.context = context.applicationContext
        mp3URi = uri
        mp3FilePath = null
        this.headers = headers
        return this
    }

    fun startPcmExtractor(): AudioDecoder {
        initMediaDecode()
        if (isPCMExtractorEOS) {
            isPCMExtractorEOS = false
            Thread { srcAudioFormatToPCM() }.start()
        }
        return this
    }

    fun release(): AudioDecoder {
        isPCMExtractorEOS = true
        releaseDecode()
        synchronized(lockPCM) {
            chunkPCMDataContainer.clear()
        }
        return this
    }

    /**
     * 如果是网络的链接可能会出现，ANR
     */
    private fun initMediaDecode() {
        try {
            mediaExtractor = MediaExtractor() // 此类可分离视频文件的音轨和视频轨道
            if (mp3FilePath != null) {
                Log.i("mixRecorder", "mp3FilePath = " + mp3FilePath!!)
                mediaExtractor!!.setDataSource(mp3FilePath!!) // 媒体文件的位置
            } else if (context != null && mp3URi != null) {
                mediaExtractor!!.setDataSource(context!!, mp3URi!!, headers) // 媒体文件的位置
            }
            mediaFormat = mediaExtractor!!.getTrackFormat(0)
            val mime = mediaFormat!!.getString(MediaFormat.KEY_MIME)
            // 检查是否为音频文件
            if (!mime!!.startsWith("audio/")) {
                error("$mp3FilePath is not audio file")
            }
            // 选择此音频轨道，因为是音频只有一条
            mediaExtractor!!.selectTrack(0)
            // 创建Decode解码器
            mediaDecode = MediaCodec.createDecoderByType(mime)
            mediaDecode!!.configure(mediaFormat, null, null, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        if (mediaDecode == null) {
            Log.e("mixRecorder", "create mediaDecode failed")
            return
        }
        mediaDecode!!.start() // 启动MediaCodec ，等待传入数据
        decodeBufferInfo = MediaCodec.BufferInfo() // 用于描述解码得到的byte[]数据的相关信息
    }

    private fun putPCMData(pcmChunk: ByteArray, bufferSize: Int, time: Long) {
        synchronized(lockPCM) {
            // 记得加锁
            chunkPCMDataContainer.add(PCM(pcmChunk, bufferSize, time))
        }
    }

    /**
     * 解码音频文件 得到PCM数据块
     */
    private fun srcAudioFormatToPCM() {

        isPCMExtractorEOS = false
        var sawInputEOS = false
        try {
            while (!isPCMExtractorEOS && mediaExtractor != null && mediaDecode != null) {
                // 加入限制，防止垃圾手机卡顿，- - 防止歌曲太大内存不够用了
                if (chunkPCMDataContainer.size > 60) {
                    try {
                        // 防止死循环ANR
                        Thread.sleep(50)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    continue
                }

                if (!sawInputEOS) {
                    // 获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
                    val inputIndex = mediaDecode!!.dequeueInputBuffer(-1)
                    if (inputIndex >= 0) {
                        val inputBuffer = mediaDecode!!.getInputBuffer(inputIndex) // 拿到inputBuffer
                        inputBuffer!!.clear() // 清空之前传入inputBuffer内的数据
                        // 这里有记录读取失败，导致直接结束跳出循环了
                        try {
                            val sampleSize = mediaExtractor!!.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) { // 小于0 代表所有数据已读取完成
                                sawInputEOS = true
                                mediaDecode!!.queueInputBuffer(
                                    inputIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                val presentationTimeUs = mediaExtractor!!.sampleTime
                                mediaDecode!!.queueInputBuffer(
                                    inputIndex, 0, sampleSize,
                                    presentationTimeUs, 0
                                )
                                // 通知MediaDecode解码刚刚传入的数据
                                mediaExtractor!!.advance() // MediaExtractor移动到下一取样处
                            }
                        } catch (e: Exception) {
                            Log.e("mixRecorder", "message = ${e.message}")
                            isPCMExtractorEOS = true
                            releaseDecode()
                            break
                        }
                    }
                }
                // 获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
                // 此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
                val outputIndex = mediaDecode!!.dequeueOutputBuffer(decodeBufferInfo!!, 10000)
                if (outputIndex >= 0) {
                    // Simply ignore codec config buffers.
                    if (decodeBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // 这是初始化的数据，无需处理
                        mediaDecode!!.releaseOutputBuffer(outputIndex, false)
                        continue
                    }
                    if (decodeBufferInfo!!.size != 0) {
                        val outBuf = mediaDecode!!.getOutputBuffer(outputIndex) // 拿到用于存放PCM数据的Buffer
                        var successBufferGet = true
                        val data = ByteArray(decodeBufferInfo!!.size) // BufferInfo内定义了此数据块的大小
                        try {
                            outBuf!!.position(decodeBufferInfo!!.offset)
                            outBuf.limit(decodeBufferInfo!!.offset + decodeBufferInfo!!.size)
                            // 某些机器上buf可能 isAccessible false
                            outBuf.get(data) // 将Buffer内的数据取出到字节数组中
                            outBuf.clear()
                        } catch (e: Exception) {
                            successBufferGet = false
                            e.printStackTrace()
                        }
                        if (successBufferGet) {
                            // 自己定义的方法，供编码器所在的线程获取数据,下面会贴出代码
                            putPCMData(data, decodeBufferInfo!!.size, mediaExtractor!!.sampleTime)
                        }
                    }
                    mediaDecode!!.releaseOutputBuffer(outputIndex, false)
                    // 此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
                    if (decodeBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isPCMExtractorEOS = true
                        Log.i("mixRecorder", "pcm finished..." + mp3FilePath!!)
                        // 只有读取完成才
                        releaseDecode()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseDecode() {
        try {
            if (mediaDecode != null) {
                mediaDecode!!.stop()
                mediaDecode!!.release()
                mediaDecode = null
            }
            if (mediaExtractor != null) {
                mediaExtractor!!.release()
                mediaExtractor = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class PCM internal constructor(
        internal var bufferBytes: ByteArray,
        internal var bufferSize: Int,
        internal var time: Long // 当前时间
    )

    companion object {

        private val lockPCM = Any()
        const val BUFFER_SIZE = 2048
    }
}
