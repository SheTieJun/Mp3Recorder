package me.shetj.recorder.core


/**
 * 配置选项
 */
data class Mp3RecorderOption(
    //声音来源：默认麦克风；如果使用VOICE_COMMUNICATION，系统会自动优化录音，但是声音会变小
    var audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
    //声道设置：单双声道
    var audioChannel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.STEREO,
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

fun recorder(block: Mp3RecorderOption.() ->Unit): Mp3RecorderOption {
    return Mp3RecorderOption().apply(block)
}