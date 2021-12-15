/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
