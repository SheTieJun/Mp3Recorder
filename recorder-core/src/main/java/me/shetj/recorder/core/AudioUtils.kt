
package me.shetj.recorder.core

import android.content.Context
import android.media.AudioManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

object AudioUtils {

    /**
     * 对声音进行变化
     * @param audioSamples
     * @param volume
     * @return
     */
    fun adjustVolume(audioSamples: ByteArray, volume: Float): ByteArray {
        val array = ByteArray(audioSamples.size)
        var i = 0
        while (i < array.size) {
            // convert byte pair to int
            var buf1 = audioSamples[i + 1].toInt()
            var buf2 = audioSamples[i].toInt()
            buf1 = (buf1 and 0xff).shl(8)
            buf2 = (buf2 and 0xff)
            var res = (buf1 or buf2)
            res = (res * volume).toInt()
            // convert back
            array[i] = res.toByte()
            array[i + 1] = (res shr 8).toByte()
            i += 2
        }
        return array
    }

    fun getAudioFormat(url: String): MediaFormat {
        val mediaExtractor = MediaExtractor() // 此类可分离视频文件的音轨和视频轨道
        mediaExtractor.setDataSource(url) // 媒体文件的位置
        return mediaExtractor.getTrackFormat(0).also {
            mediaExtractor.release()
        }
    }

    fun getAudioChannel(url: String): Int {
        return getAudioFormat(url).getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    }

    fun getAudioFormat(context: Context, url: Uri): MediaFormat {
        val mediaExtractor = MediaExtractor() // 此类可分离视频文件的音轨和视频轨道
        mediaExtractor.setDataSource(context, url, null) // 媒体文件的位置
        return mediaExtractor.getTrackFormat(0).also {
            mediaExtractor.release()
        }
    }


    fun getAudioChannel(context: Context, url: Uri): Int {
        return getAudioFormat(context, url).getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    }

    //获取最佳采样率
    fun getBestSampleRate(context: Context): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val sampleRateStr: String? = am?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val sampleRate: Int = sampleRateStr?.let { str ->
            Integer.parseInt(str).takeUnless { it == 0 }
        } ?: 44100 // Use a default value if property not found
        return sampleRate
    }
}
