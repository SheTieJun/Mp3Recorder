package me.shetj.recorder.core

import androidx.annotation.WorkerThread

interface PCMListener {

    /**
     * 在pcm转mp3之前
     * * 子线程，请不要在该方法下面做UI变化
     * * 请不要做太长时间的操作
     * please do not make UI changes under this method
     */
    @WorkerThread
    fun onBeforePCMToMp3(pcm:ShortArray):ShortArray{
        return pcm
    }

//    /**
//     * 在录的pcm 和 背景音乐pcm进行混合之前
//     * Before mixing the recorded PCM and background music PCM
//     * 只有`MixRecorder`有效
//     */
//    @WorkerThread
//    fun onBeforeMixBG(pcm:ByteArray):ByteArray{
//        return pcm
//    }

}