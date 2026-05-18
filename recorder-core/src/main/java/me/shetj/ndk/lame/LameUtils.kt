package me.shetj.ndk.lame

/**
 * LAME MP3 编码器工具类
 * 
 * 基于 LAME 库的 Android NDK 封装，提供高质量的 PCM 音频到 MP3 格式的编码功能。
 * LAME 是一个高质量的 MPEG 音频层 III (MP3) 编码器，在 LGPL 许可证下发布。
 * 
 * ## 主要功能
 * - 支持 CBR（固定比特率）和 VBR（可变比特率）编码模式
 * - 提供灵活的音频参数配置（采样率、比特率、声道数等）
 * - 内置音频滤波器（低通和高通滤波器）
 * - 支持实时 PCM 数据流编码
 * - 提供音频分析功能（分贝值计算）
 * 
 * ## 基本使用流程
 * ```kotlin
 * // 1. 初始化编码器
 * LameUtils.init(
 *     inSampleRate = 44100,
 *     inChannel = 2,
 *     outSampleRate = 44100,
 *     outBitrate = 128,
 *     quality = 2,
 *     lowpassFreq = -1,
 *     highpassFreq = -1,
 *     vbr = false,
 *     enableLog = true
 * )
 * 
 * // 2. 编码音频数据
 * val pcmBuffer = ShortArray(1024)
 * val mp3Buffer = ByteArray(1024)
 * val bytesEncoded = LameUtils.encode(pcmBuffer, pcmBuffer, pcmBuffer.size, mp3Buffer)
 * 
 * // 3. 完成编码并释放资源
 * val finalBuffer = ByteArray(1024)
 * LameUtils.flush(finalBuffer)
 * LameUtils.close()
 * ```
 * 
 * @author SheTieJun
 * @since 1.0.0
 * 
 * ⚠️ **注意事项**
 * - 本类不是线程安全的，多线程使用需要外部同步
 * - 使用完毕后必须调用 [close] 方法释放资源
 * - MP3 输出缓冲区建议至少为输入 PCM 数据的 1.25 倍
 * - VBR 模式下必须调用 [writeVBRHeader] 写入正确的头信息（不建议使用）
 */
class LameUtils {

    private var lameHandle: Long = 0L

    companion object {
        init {
            System.loadLibrary("shetj_mp3lame")
        }

        /**
         * 获取 LAME 库版本信息
         * 
         * @return LAME 库的版本字符串
         * 
         * @sample
         * ```kotlin
         * val version = LameUtils.version()
         * println("LAME Version: $version")
         * ```
         */
        @JvmStatic
        external fun version(): String

        @JvmStatic
        external fun getPCMDB(pcm: ShortArray, samples: Int): Int
    }

    /**
     * 初始化 LAME 编码器
     * 
     * 配置编码器的各项参数，包括采样率、声道数、比特率、编码质量等。
     * 必须在进行任何编码操作之前调用此方法。
     * 
     * @param inSampleRate 输入音频采样率（Hz），支持范围：8000-48000
     * @param inChannel 输入音频声道数（1=单声道，2=立体声）
     * @param outSampleRate 输出 MP3 采样率（Hz），通常与输入采样率相同
     * @param outBitrate 输出 MP3 比特率（kbps），建议范围：64-320
     * @param quality 编码质量等级（0-9）
     *   - 0: 最高质量，最慢速度（专业音频制作）
     *   - 2: 高质量，推荐用于一般应用
     *   - 5: 标准质量，适合实时编码
     *   - 9: 最低质量，最快速度（语音录制）
     * @param lowpassFreq 低通滤波器截止频率（Hz）
     *   - 高于此频率的声音会被截除
     *   - -1 表示不使用滤波器
     *   - 建议范围：1000-20000
     * @param highpassFreq 高通滤波器截止频率（Hz）
     *   - 低于此频率的声音会被截除
     *   - -1 表示不使用滤波器
     *   - 建议范围：20-1000
     * @param vbr 是否启用 VBR（可变比特率）模式
     *   - true: 启用 VBR，文件大小更小，质量更均匀
     *   - false: 使用 CBR（固定比特率）模式
     * @param enableLog 是否启用日志输出，用于调试和监控编码过程
     * 
     * @throws IllegalArgumentException 当参数超出有效范围时抛出
     * 
     *
     * ```kotlin
     * // 高质量音乐编码配置
     * LameUtils.init(
     *     inSampleRate = 44100,
     *     inChannel = 2,
     *     outSampleRate = 44100,
     *     outBitrate = 192,
     *     quality = 2,
     *     lowpassFreq = 15000,  // 截止 15kHz 以上频率
     *     highpassFreq = 80,    // 截止 80Hz 以下频率
     *     vbr = true,
     *     enableLog = true
     * )
     * 
     * // 语音录制配置
     * LameUtils.init(
     *     inSampleRate = 16000,
     *     inChannel = 1,
     *     outSampleRate = 16000,
     *     outBitrate = 64,
     *     quality = 5,
     *     lowpassFreq = -1,
     *     highpassFreq = -1,
     *     vbr = false,
     *     enableLog = false
     * )
     * ```
     */
    fun init(
        inSampleRate: Int,
        inChannel: Int,
        outSampleRate: Int,
        outBitrate: Int,
        quality: Int,
        lowpassFreq: Int,
        highpassFreq: Int,
        vbr: Boolean,
        enableLog: Boolean
    ) {
        if (lameHandle != 0L) {
            close()
        }
        lameHandle = initNative(
            inSampleRate, inChannel, outSampleRate, outBitrate, quality,
            lowpassFreq, highpassFreq, vbr, enableLog
        )
    }

    /**
     * 编码 PCM 音频数据（分离声道模式）
     * 
     * 适用于单声道录音或需要分别处理左右声道的场景。
     * 对于单声道音频，左右声道参数可以使用相同的数据。
     * 
     * @param bufferLeft 左声道 PCM 数据（16位有符号整数）
     * @param bufferRight 右声道 PCM 数据（16位有符号整数）
     * @param samples 每个声道的样本数，通常等于 bufferLeft.size
     * @param mp3buf MP3 输出缓冲区，建议大小至少为输入数据的 1.25 倍
     * 
     * @return 编码结果
     *   - `> 0`: 成功编码的字节数
     *   - `0`: 没有输出数据（正常情况，可能需要更多输入数据）
     *   - `-1`: MP3 缓冲区太小，需要增大 mp3buf 大小
     *   - `-2`: 内存分配问题
     *   - `-3`: 编码器未正确初始化，请先调用 [init]
     *   - `-4`: 心理声学模型问题
     * 
     * @throws IllegalStateException 当编码器未初始化时抛出
     * 
     *
     * ```kotlin
     * // 单声道编码示例
     * val pcmData = ShortArray(1024)
     * val mp3Buffer = ByteArray(1280)  // 1024 * 1.25
     * 
     * // 填充 PCM 数据...
     * val result = LameUtils.encode(pcmData, pcmData, pcmData.size, mp3Buffer)
     * 
     * when {
     *     result > 0 -> {
     *         // 成功编码，result 为输出的 MP3 字节数
     *         val mp3Data = mp3Buffer.copyOf(result)
     *         // 处理编码后的 MP3 数据...
     *     }
     *     result == 0 -> {
     *         // 正常情况，继续输入更多数据
     *     }
     *     result < 0 -> {
     *         // 编码错误，检查错误码
     *         handleEncodingError(result)
     *     }
     * }
     * ```
     */
    fun encode(
        bufferLeft: ShortArray,
        bufferRight: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int {
        if (lameHandle == 0L) return -3
        return encodeNative(lameHandle, bufferLeft, bufferRight, samples, mp3buf)
    }

    /**
     * 编码交错的立体声 PCM 数据
     * 
     * 适用于立体声录音，PCM 数据按左右声道交替排列（L, R, L, R, ...）。
     * 这是处理立体声音频的推荐方法，效率更高。
     * 
     * @param pcm 交错排列的立体声 PCM 数据（左右声道交替）
     * @param samples 每个声道的样本数，等于 pcm.size / 2
     * @param mp3buf MP3 输出缓冲区，建议大小至少为输入数据的 1.25 倍
     * 
     * @return 编码结果，含义与 [encode] 方法相同
     * 
     * @throws IllegalArgumentException 当 samples 不等于 pcm.size / 2 时抛出
     * @throws IllegalStateException 当编码器未初始化时抛出
     * 
     *
     * ```kotlin
     * // 立体声编码示例
     * val interleavedPcm = ShortArray(2048)  // 1024 个立体声样本
     * val mp3Buffer = ByteArray(1280)
     * 
     * // 填充交错的 PCM 数据: [L0, R0, L1, R1, L2, R2, ...]
     * val samplesPerChannel = interleavedPcm.size / 2
     * val result = LameUtils.encodeInterleaved(interleavedPcm, samplesPerChannel, mp3Buffer)
     * 
     * if (result > 0) {
     *     val mp3Data = mp3Buffer.copyOf(result)
     *     // 处理编码后的 MP3 数据...
     * }
     * ```
     */
    fun encodeInterleaved(
        pcm: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int {
        if (lameHandle == 0L) return -3
        return encodeInterleavedNative(lameHandle, pcm, samples, mp3buf)
    }

    /**
     * 编码字节数组格式的 PCM 数据（分离声道模式）
     * 
     * 与 [encode] 方法功能相同，但接受字节数组格式的 PCM 数据。
     * 适用于从文件或网络流中读取的原始音频数据。
     * 
     * @param bufferLeft 左声道 PCM 数据（字节格式）
     * @param bufferRight 右声道 PCM 数据（字节格式）
     * @param samples 每个声道的样本数
     * @param mp3buf MP3 输出缓冲区
     * 
     * @return 编码结果，含义与 [encode] 方法相同
     * 
     *
     * ```kotlin
     * val leftChannel = ByteArray(2048)   // 1024 个 16位样本
     * val rightChannel = ByteArray(2048)
     * val mp3Buffer = ByteArray(1280)
     * 
     * val result = LameUtils.encodeByByte(leftChannel, rightChannel, 1024, mp3Buffer)
     * ```
     */
    fun encodeByByte(
        bufferLeft: ByteArray,
        bufferRight: ByteArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int {
        if (lameHandle == 0L) return -3
        return encodeByByteNative(lameHandle, bufferLeft, bufferRight, samples, mp3buf)
    }

    /**
     * 编码字节数组格式的交错立体声 PCM 数据
     * 
     * 与 [encodeInterleaved] 方法功能相同，但接受字节数组格式的 PCM 数据。
     * 
     * @param pcm 交错排列的立体声 PCM 数据（字节格式）
     * @param samples 每个声道的样本数
     * @param mp3buf MP3 输出缓冲区
     * 
     * @return 编码结果，含义与 [encode] 方法相同
     * 
     * @sample
     * ```kotlin
     * val interleavedBytes = ByteArray(4096)  // 1024 个立体声样本（每样本2字节）
     * val mp3Buffer = ByteArray(1280)
     * 
     * val result = LameUtils.encodeInterleavedByByte(interleavedBytes, 1024, mp3Buffer)
     * ```
     */
    fun encodeInterleavedByByte(
        pcm: ByteArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int {
        if (lameHandle == 0L) return -3
        return encodeInterleavedByByteNative(lameHandle, pcm, samples, mp3buf)
    }

    /**
     * 写入 VBR 头信息到 MP3 文件
     * 
     * 在 VBR（可变比特率）模式下，必须调用此方法来写入正确的头信息，
     * 否则某些播放器可能无法正确显示文件时长或进度。
     * 
     * ⚠️ **重要提醒**：
     * - 仅在启用 VBR 模式时需要调用
     * - 必须在所有编码完成并调用 [flush] 之后调用
     * - 文件路径必须是已存在的 MP3 文件
     * 
     * @param file MP3 文件的完整路径
     * 
     * @throws IllegalArgumentException 当文件路径无效时抛出
     * @throws IllegalStateException 当在非 VBR 模式下调用时抛出
     * 
     *
     * ```kotlin
     * // VBR 编码完整流程
     * LameUtils.init(
     *     inSampleRate = 44100,
     *     inChannel = 2,
     *     outSampleRate = 44100,
     *     outBitrate = 128,
     *     quality = 2,
     *     lowpassFreq = -1,
     *     highpassFreq = -1,
     *     vbr = true,  // 启用 VBR
     *     enableLog = true
     * )
     * 
     * // ... 进行编码操作 ...
     * 
     * // 刷新缓冲区
     * val finalBuffer = ByteArray(1024)
     * LameUtils.flush(finalBuffer)
     * 
     * // 写入 VBR 头信息
     * val outputFile = "/sdcard/output.mp3"
     * LameUtils.writeVBRHeader(outputFile)
     * 
     * // 关闭编码器
     * LameUtils.close()
     * ```
     */
    fun writeVBRHeader(file: String) {
        if (lameHandle != 0L) {
            writeVBRHeaderNative(lameHandle, file)
        }
    }

    /**
     * 刷新编码器缓冲区
     * 
     * 获取编码器内部缓冲区中剩余的 MP3 数据。
     * 在完成所有音频数据编码后，必须调用此方法以确保所有数据都被正确编码输出。
     * 
     * @param mp3buf 用于接收剩余 MP3 数据的缓冲区
     * 
     * @return 刷新出的 MP3 数据字节数
     *   - `> 0`: 成功刷新的字节数
     *   - `0`: 没有剩余数据
     *   - `< 0`: 错误码（含义与 [encode] 方法相同）
     * 
     *
     * ```kotlin
     * // 完成编码流程
     * val finalBuffer = ByteArray(1024)
     * val finalBytes = LameUtils.flush(finalBuffer)
     * 
     * if (finalBytes > 0) {
     *     val finalMp3Data = finalBuffer.copyOf(finalBytes)
     *     // 写入最后的 MP3 数据到文件...
     * }
     * ```
     */
    fun flush(mp3buf: ByteArray): Int {
        if (lameHandle == 0L) return -3
        return flushNative(lameHandle, mp3buf)
    }

    /**
     * 关闭编码器并释放资源
     * 
     * 释放 LAME 编码器占用的所有资源。
     * 使用完毕后必须调用此方法，否则可能导致内存泄漏。
     * 
     * ⚠️ **重要提醒**：
     * - 调用此方法后，编码器将无法继续使用
     * - 如需再次编码，必须重新调用 [init] 方法
     * - 建议在 try-finally 块中调用以确保资源释放
     * 
     *
     * ```kotlin
     * try {
     *     LameUtils.init(...)
     *     // 进行编码操作...
     *     LameUtils.flush(buffer)
     * } finally {
     *     LameUtils.close()  // 确保资源被释放
     * }
     * ```
     */
    fun close() {
        if (lameHandle != 0L) {
            closeNative(lameHandle)
            lameHandle = 0L
        }
    }

    private external fun initNative(
        inSampleRate: Int,
        inChannel: Int,
        outSampleRate: Int,
        outBitrate: Int,
        quality: Int,
        lowpassFreq: Int,
        highpassFreq: Int,
        vbr: Boolean,
        enableLog: Boolean
    ): Long

    private external fun encodeNative(
        handle: Long,
        bufferLeft: ShortArray,
        bufferRight: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    private external fun encodeInterleavedNative(
        handle: Long,
        pcm: ShortArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    private external fun encodeByByteNative(
        handle: Long,
        bufferLeft: ByteArray,
        bufferRight: ByteArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    private external fun encodeInterleavedByByteNative(
        handle: Long,
        pcm: ByteArray,
        samples: Int,
        mp3buf: ByteArray
    ): Int

    private external fun writeVBRHeaderNative(handle: Long, file: String)

    private external fun flushNative(handle: Long, mp3buf: ByteArray): Int

    private external fun closeNative(handle: Long)
}
