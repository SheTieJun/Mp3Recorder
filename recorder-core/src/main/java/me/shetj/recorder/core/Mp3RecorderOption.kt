package me.shetj.recorder.core

import android.media.AudioFormat
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioSource
import androidx.annotation.IntDef


/**
 * 配置选项
 * [android.media.AudioFormat] :声道设置
 * [MediaRecorder.AudioSource] :声音来源
 */
data class Mp3RecorderOption(
    //声音来源：默认麦克风；如果使用VOICE_COMMUNICATION，系统会自动优化录音，但是声音会变小
    @Source var audioSource: Int = AudioSource.MIC,
    //声道设置：单双声道
    @Channel var audioChannel: Int = AudioFormat.CHANNEL_IN_STEREO,
    // debug，输出录音过程日志
    var isDebug: Boolean = false,
    //最大录制事件
    var mMaxTime: Int = 1800 * 1000,
    //采样率，影响音质
    var samplingRate: Int = 44100,
    //MP3输出的比特率,影响输出大小和输出音质，同时会影响文件的大小，质量越好文件越大
    var mp3BitRate: Int = 32,//128 /96(高),32（低）
    //Lame   质量1-7，Lame的输出质量，1最快
    var mp3Quality: Int = 3,
    //无法录音回调（一般是需要权限：录音和存储）
    var permissionListener: PermissionListener? = null,
    //回调
    var recordListener: RecordListener? = null,
    //声音增强系数,不建议配置，可能会有噪音
    var wax: Float = 1f
)

fun recorder(block: Mp3RecorderOption.() -> Unit): Mp3RecorderOption {
    return Mp3RecorderOption().apply(block)
}


/** * CAMCORDER	录音来源于同方向的相机麦克风相同，若相机无内置相机或无法识别，则使用预设的麦克风
 *  * DEFAULT	默认音频源
 *  * MIC	录音来源为主麦克风
 *  * REMOTE_SUBMIX	用于远程呈现的音频流的子混音的音频源，需要Manifest.permission.CAPTURE_AUDIO_OUTPUT权限，第三方应用无法申请
 *  * UNPROCESSED	与默认相同
 *  * VOICE_CALL	记录上行与下行音频源，需要Manifest.permission.CAPTURE_AUDIO_OUTPUT权限，第三方应用无法申请
 *  * VOICE_COMMUNICATION	麦克风音频源针对VoIP等语音通信进行了调整,可以接收到通话的双方语音
 *  * VOICE_DOWNLINK、VOICE_UPLINK	上行下行的语音，需要Manifest.permission.CAPTURE_AUDIO_OUTPUT权限，第三方应用无法申请
 *  * VOICE_PERFORMANCE	捕获音频的来源意味着要实时处理并播放以进行现场演出
 *  * VOICE_RECOGNITION	用于获取语音进行语音识别
 */
@IntDef(value = [AudioSource.CAMCORDER,AudioSource.DEFAULT, AudioSource.MIC,
    AudioSource.VOICE_PERFORMANCE, AudioSource.UNPROCESSED,
    AudioSource.VOICE_RECOGNITION, AudioSource.VOICE_COMMUNICATION,
    //下面的需要Manifest.permission.CAPTURE_AUDIO_OUTPUT权限，第三方应用无法申请
    AudioSource.VOICE_UPLINK, AudioSource.VOICE_DOWNLINK,
    AudioSource.VOICE_CALL,
   ])
@Retention(AnnotationRetention.SOURCE )
annotation class Source

@IntDef(value = [AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO])
@Retention(AnnotationRetention.SOURCE)
annotation class Channel
