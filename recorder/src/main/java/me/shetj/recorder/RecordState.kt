package me.shetj.recorder

/**
 * 录音的状态
 * @author shetj
 */
enum class RecordState private constructor(val state: String) {
    RECORDING("recording"),
    PAUSED("pause"),
    STOPPED("stop")

}