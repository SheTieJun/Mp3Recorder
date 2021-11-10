package me.shetj.mp3recorder.record.utils

import android.media.MediaMetadataRetriever
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

object Util {



    fun formatSeconds2(seconds: Int): String {
        var seconds = seconds
        if (seconds > 3600) {
            seconds = 3600
        }
        return (getTwoDecimalsValue(seconds / 60) + "分"
                + getTwoDecimalsValue(seconds % 60) + "秒")
    }

    fun formatSeconds3(seconds: Int): String {
        var seconds = seconds
        if (seconds > 3600) {
            seconds = 3600
        }
        return (getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60))
    }

    private fun getTwoDecimalsValue(value: Int): String {
        return if (value in 0..9) {
            "0$value"
        } else {
            value.toString() + ""
        }
    }


    fun getAudioLength(filename: String?): Int {
      return  filename?.let { getAudioLength(it).toInt()/1000 }?:0
    }


    private fun getAudioLength(filename: String): String {
        val mmr = MediaMetadataRetriever();
        var duration = "1"
        try {
            mmr.setDataSource(filename);
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toString()
        } catch (ex: Exception) {
        } finally {
            mmr.release()
        }
        return duration
    }
}