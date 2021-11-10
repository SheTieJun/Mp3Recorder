package me.shetj.recorder.core

import android.media.AudioFormat

enum class PCMFormat(val bytesPerFrame: Int, val audioFormat: Int) {
    PCM_16BIT(2, AudioFormat.ENCODING_PCM_16BIT)
}
