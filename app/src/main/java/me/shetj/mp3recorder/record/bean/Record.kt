package me.shetj.mp3recorder.record.bean


import me.shetj.simxutils.db.annotation.Column
import me.shetj.simxutils.db.annotation.Table

/**
 * 录音
 */
@Table(name = "record")
class Record {

    @Column(name = "id", isId = true, autoGen = true)
    private val id: Int = 0
    @Column(name = "user_id")
    var user_id: String? = null//是否绑定用户，默认不绑定用户
    @Column(name = "audio_url")
    var audio_url: String? = null//保存的路径
    @Column(name = "audio_name")
    var audioName: String? = null//录音的名称
    @Column(name = "audio_length")
    var audioLength: Int = 0//长度
    @Column(name = "audio_content")
    var audioContent: String? = null//内容
    @Column(name = "otherInfo")
    var otherInfo: String? = null// 预览信息

    constructor()

    constructor(user_id: String, audio_url: String, audioName: String, audioLength: Int, content: String) {
        this.user_id = user_id
        this.audio_url = audio_url
        this.audioName = audioName
        this.audioLength = audioLength
        this.audioContent = content
    }
}
