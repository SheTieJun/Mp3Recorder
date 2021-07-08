package me.shetj.mp3recorder.record.utils


class MainThreadEvent<T> : BaseEvent<T> {


    constructor() : super() {}

    constructor(type: Int, content: T) : super(type, content) {}

    companion object {
        val REMOVE_MUSIC = 0x506
    }
}
