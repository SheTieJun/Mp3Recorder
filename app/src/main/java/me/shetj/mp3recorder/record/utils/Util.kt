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