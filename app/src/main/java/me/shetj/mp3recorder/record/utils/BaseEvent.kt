package me.shetj.mp3recorder.record.utils

open class BaseEvent<T> {
    /**
     * 类型
     */
    var type: Int = 0
    /**
     * 数据
     */
    var content: T? = null

    constructor() {}

    constructor(type: Int, content: T) {
        this.type = type
        this.content = content
    }
}
