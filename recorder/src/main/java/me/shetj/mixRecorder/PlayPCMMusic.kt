@file:Suppress("DEPRECATION")

package me.shetj.mixRecorder

import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import me.shetj.player.PlayerListener
import java.util.concurrent.LinkedBlockingDeque
import android.media.AudioFormat.CHANNEL_OUT_STEREO
import android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC
import java.io.IOException
import java.nio.ByteBuffer


/**
 * 播放音乐，用来播放PCM
 *
 * 1.支持暂停 pause(),resume() <br>
 * 2.支持循环播放setLoop(boolean isLoop)<br>
 * 3. 支持切换背景音乐 setBackGroundUrl(String path)<br>
 * update 2019年10月11日
 * 添加时间进度记录(注意返回不是在主线程需要自己设置在主线程)
 *
 * TODO seekTo 缺失功能
 *
 */
class PlayPCMMusic(private val defaultChannel: Int = CHANNEL_OUT_STEREO) {

    fun getBufferSize() :Int{
        if (backGroundBytes.isEmpty()) {
            return 2048
        }
        return backGroundBytes.first.size
    }

    private val backGroundBytes =
        LinkedBlockingDeque<ByteArray>()//new ArrayDeque<>();// ArrayDeque不是线程安全的
    var isPlayingMusic = false
        private set
    private var mIsRecording = false
    private var mIsLoop = false
    var isIsPause = false
        private set
    private val playHandler: PlayHandler
    private var audioTrack: AudioTrack? = null
    private var volume  = 0.3f
    private var playerListener: PlayerListener?=null

    //音频解码PCM相关
    private var mediaExtractor: MediaExtractor? = null
    private var mediaDecode: MediaCodec? = null
    private var decodeInputBuffers: Array<ByteBuffer>? = null
    private var decodeOutputBuffers: Array<ByteBuffer>? = null
    private var decodeBufferInfo: MediaCodec.BufferInfo? = null
    private var mediaFormat: MediaFormat? = null
        private set
    private var mp3FilePath: String? = null
    internal var isPCMExtractorEOS = true
        private set

    private class PlayHandler(private val playBackMusic: PlayPCMMusic) :
        Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PROCESS_STOP, PROCESS_ERROR -> playBackMusic.release()
                PROCESS_REPLAY -> playBackMusic.restartMusic()
            }
        }
    }

    init {
        playHandler = PlayHandler(this)
    }

    /**
     * 设置或者切换背景音乐
     * @param path
     * @return
     */
    fun setBackGroundUrl(path: String): PlayPCMMusic {
        if (isIsPause) {
            releaseDecoder()
            initDecoder(path)
        } else {
            isIsPause = true
            releaseDecoder()
            initDecoder(path)
            initMediaDecode()
            isIsPause = false
        }
        return this
    }

    fun setBackGroundPlayListener(playerListener: PlayerListener){
        this.playerListener = playerListener
    }

    private fun initDecoder(path: String) {
        mp3FilePath = path
    }

    private fun releaseDecoder() {
        if (mediaDecode != null) {
            mediaDecode!!.release()
        }
        if (mediaExtractor != null) {
            mediaExtractor!!.release()
        }
    }

    /**
     * mIsRecording 标识外部是否正在录音，只有开始录音
     * @param enable
     * @return
     */
    fun setNeedRecodeDataEnable(enable: Boolean): PlayPCMMusic {
        mIsRecording = enable
        return this
    }

    /**
     * 是否循环播放
     * @param isLoop 是否循环
     */
    fun setLoop(isLoop: Boolean) {
        mIsLoop = isLoop
    }

    /**
     * 开始播放
     * @return
     */
    fun startPlayBackMusic(): PlayPCMMusic {
        isPlayingMusic = true
        initMediaDecode()
        PlayNeedMixAudioTask(object : BackGroundFrameListener {
            override fun onFrameArrive(bytes: ByteArray) {
                addBackGroundBytes(bytes)
            }
        }).start()
        playerListener?.onStart("",0)
        return this
    }


    fun getBackGroundBytes(): ByteArray? {
        if (backGroundBytes.isEmpty()) {
            return null
        }
        return backGroundBytes.poll()
    }

    fun hasFrameBytes(): Boolean {
        return !backGroundBytes.isEmpty()
    }

    fun frameBytesSize(): Int {
        return backGroundBytes.size
    }

    /**
     * 暂停播放
     * @return
     */
    fun stop() {
        isPlayingMusic = false
        playerListener?.onStop()
    }

    fun resume() {
        if (isPlayingMusic) {
            isIsPause = false
            playerListener?.onResume()
        }
    }

    fun pause() {
        if (isPlayingMusic) {
            isIsPause = true
            playerListener?.onPause()
        }
    }


    fun release(): PlayPCMMusic {
        isPlayingMusic = false
        isIsPause = false
        if (mediaDecode != null) {
            mediaDecode!!.release()
        }
        if (mediaExtractor != null) {
            mediaExtractor!!.release()
        }
        backGroundBytes.clear()
        return this
    }

    /**
     * 这样的方式控制同步 需要添加到队列时判断同时在播放和录制
     */
    private fun addBackGroundBytes(bytes: ByteArray) {
        if (isPlayingMusic && mIsRecording) {
            backGroundBytes.add(bytes) // what if out of memory?
        }
    }

    /**
     * 重新开始播放
     */
    private fun restartMusic() {
        releaseDecoder()
        initMediaDecode()
    }


    /**
     * 虽然可以新建多个 AsyncTask的子类的实例，但是AsyncTask的内部Handler和ThreadPoolExecutor都是static的，
     * 这么定义的变 量属于类的，是进程范围内共享的，所以AsyncTask控制着进程范围内所有的子类实例，
     * 而且该类的所有实例都共用一个线程池和Handler
     * 这里新开一个线程
     * 自己解析出来 pcm data
     */
    private inner class PlayNeedMixAudioTask internal constructor(private val listener: BackGroundFrameListener?) :
        Thread() {
        override fun run() {
            try {
                if (audioTrack == null) {
                    audioTrack = initAudioTrack()
                    setVolume(volume)
                }
                audioTrack!!.play()
                while (isPlayingMusic) {
                    if (!isIsPause) {
                        playMusic()
                        continue
                    } else {
                        //如果是暂停
                        try {
                            //防止死循环ANR
                            sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
                playerListener?.onStop()
                audioTrack!!.stop()
                audioTrack!!.flush()
                audioTrack!!.release()
                audioTrack = null
            } catch (e: Exception) {
                Log.e("mp3Recorder", "error:" + e.message)
                playerListener?.onError(e)
            } finally {
                isPlayingMusic = false
                isIsPause = false
                playerListener?.onCompletion()
            }
        }

        private fun playMusic() {
            var sawInputEOS = false
            try {
                while (!isPCMExtractorEOS && isPlayingMusic   ) {
                    if (isIsPause) {
                        try {
                            //防止死循环ANR
                            sleep(500)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                        continue
                    }
                    if (!sawInputEOS) {
                        val inputIndex =
                            mediaDecode!!.dequeueInputBuffer(-1)//获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
                        if (inputIndex >= 0) {
                            val inputBuffer = decodeInputBuffers!![inputIndex]//拿到inputBuffer
                            inputBuffer.clear()//清空之前传入inputBuffer内的数据
                            val sampleSize = mediaExtractor!!.readSampleData(
                                inputBuffer,
                                0
                            )
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
                            val pcm = AudioDecoder.PCM(
                                data,
                                decodeBufferInfo!!.size,
                                mediaExtractor!!.sampleTime
                            )
                            val temp = pcm.bufferBytes
                            audioTrack!!.write(temp, 0, temp.size)
                            if (mediaFormat != null) {
                                playerListener?.onProgress(
                                    (pcm.time / 1000).toInt(),
                                    (mediaFormat!!.getLong(MediaFormat.KEY_DURATION) / 1000).toInt()
                                )
                            }
                            listener?.onFrameArrive(temp)
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
                if (isPCMExtractorEOS) {
                    if (mIsLoop) {
                        mediaExtractor!!.seekTo(0, SEEK_TO_PREVIOUS_SYNC)
                        playHandler.sendEmptyMessage(PROCESS_REPLAY)
                    } else {
                        if (mediaDecode != null) {
                            mediaDecode!!.release()
                        }
                        if (mediaExtractor != null) {
                            mediaExtractor!!.release()
                        }
                        isPlayingMusic = false
                    }
                }
            }
        }
    }

    private fun initAudioTrack(): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(
            mSampleRate,
            defaultChannel, mAudioEncoding
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(mAudioEncoding)
                        .setSampleRate(mSampleRate)
                        .setChannelMask(defaultChannel)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            return  AudioTrack(AudioManager.STREAM_MUSIC,
                mSampleRate, defaultChannel, mAudioEncoding, bufferSize,
                AudioTrack.MODE_STREAM
            )
        }
    }

    private fun initMediaDecode() {
        try {
            mediaExtractor = MediaExtractor()//此类可分离视频文件的音轨和视频轨道
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
        isPCMExtractorEOS = false
    }


    fun setVolume(volume: Float) {
        this.volume = volume
        if (audioTrack != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioTrack!!.setVolume(volume)
            }else{
                audioTrack!!.setStereoVolume(volume,volume)
            }
        }
    }

    internal interface BackGroundFrameListener {
        fun onFrameArrive(bytes: ByteArray)
    }

    companion object {
        private val PROCESS_STOP = 3
        private val PROCESS_ERROR = 4
        private val PROCESS_REPLAY = 5
        private val mSampleRate = 44100
        private val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT//一个采样点16比特-2个字节
    }
}
