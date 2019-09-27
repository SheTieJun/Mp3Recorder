package me.shetj.recorder

import android.media.MediaRecorder
import androidx.annotation.IntRange
import me.shetj.player.PermissionListener
import me.shetj.player.RecordListener

@JvmOverloads
fun recorderBuilder(audioSource: Int = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    isDebug: Boolean = BuildConfig.DEBUG,
                    mMaxTime: Int = 1800 * 1000,
                    @IntRange(from = 0, to = 9) mp3Quality: Int = 5,
                    permissionListener: PermissionListener ? =null,
                    recordListener: RecordListener ?= null,
                    wax: Float = 1f

):MP3Recorder{
   return MP3Recorder(audioSource, isDebug)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax) //声音增强处理 默认
}