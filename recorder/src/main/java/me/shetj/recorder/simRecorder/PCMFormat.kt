package me.shetj.recorder.simRecorder

import android.media.AudioFormat

enum class PCMFormat(val bytesPerFrame: Int, val audioFormat: Int) {
    PCM_8BIT(1, AudioFormat.ENCODING_PCM_8BIT),
    PCM_16BIT(2, AudioFormat.ENCODING_PCM_16BIT)
}
