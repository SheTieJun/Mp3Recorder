package me.shetj.recorder.core


/**
 * 配置选项
 */
data class Mp3RecorderOption(
    //声音来源
    var audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    //单双声道
    var channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
    // debug
    var isDebug: Boolean = false,
    //最大录制事件
    var mMaxTime: Int = 1800 * 1000,
    //采样率
    var samplingRate: Int = 44100,
    //MP3输出的比特率
    var mp3BitRate: Int = 64,//128 /96(高),32（低）
    //Lame   质量1-7
    var mp3Quality: Int = 1,
    var permissionListener: PermissionListener? = null,
    //回调
    var recordListener: RecordListener? = null,
    //声音增强系数
    var wax: Float = 1f
)

fun RecorderBuilder(block: Mp3RecorderOption.() ->Unit): Mp3RecorderOption {
    return Mp3RecorderOption().apply(block)
}