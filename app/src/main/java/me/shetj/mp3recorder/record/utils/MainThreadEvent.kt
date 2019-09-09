package me.shetj.mp3recorder.record.utils


class MainThreadEvent<T> : BaseEvent<T> {


    constructor() : super() {}

    constructor(type: Int, content: T) : super(type, content) {}

    companion object {

        val RECORD_REFRESH_MY = 0x501//刷新我的录音界面
        val RECORD_REFRESH_DEL = 0x502//刷新我的录音界面
        val RECORD_REFRESH_RECORD = 0x504//已经存在刷新我的录音
        val RECORD_POST_URL = 0x505//通知上传url成功
        val REMOVE_MUSIC = 0x506
    }
}
