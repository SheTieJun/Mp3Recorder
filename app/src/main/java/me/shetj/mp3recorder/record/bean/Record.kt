
package me.shetj.mp3recorder.record.bean


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 录音
 */
@Entity(tableName = "record")
class Record {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "user_id")
    var user_id: String ? = null //是否绑定用户，默认不绑定用户

    @ColumnInfo(name = "audio_url")
    var audio_url: String? = null//保存的路径

    @ColumnInfo(name = "audio_name")
    var audioName: String? = null//录音的名称

    @ColumnInfo(name = "audio_length")
    var audioLength: Int = 0//长度

    @ColumnInfo(name = "audio_content")
    var audioContent: String? = null//内容

    @ColumnInfo(name = "otherInfo")
    var otherInfo: String? = null// 预览信息

    constructor()

    constructor(
        user_id: String,
        audio_url: String,
        audioName: String,
        audioLength: Int,
        content: String
    ) {
        this.user_id = user_id
        this.audio_url = audio_url
        this.audioName = audioName
        this.audioLength = audioLength
        this.audioContent = content
    }
}
