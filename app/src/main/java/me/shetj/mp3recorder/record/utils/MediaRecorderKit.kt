
package me.shetj.mp3recorder.record.utils

import android.media.MediaRecorder
import me.shetj.base.tools.file.EnvironmentStorage

/**
 *
 * <b>@author：</b> shetj<br>
 * <b>@createTime：</b> 2021/11/11<br>
 * <b>@email：</b> 375105540@qq.com<br>
 * <b>@describe</b> MediaRecorder demo <br>
 */
object MediaRecorderKit {

    var mMediaRecorder: MediaRecorder? = null
    var savePath: String? = null


    fun startRecord() {
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null){
            mMediaRecorder = MediaRecorder()
        }
        try {
            /* ②setAudioSource/setVedioSource */
            // 设置麦克风
            mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            savePath = EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".m4a"
            /* ③准备 */
            mMediaRecorder!!.setOutputFile(savePath)
            mMediaRecorder!!.prepare()
            /* ④开始 */
            mMediaRecorder!!.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }


    fun stopRecord() {
        if (mMediaRecorder == null){
            return
        }
        try {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.release()
        } catch ( e:RuntimeException) {
            mMediaRecorder!!.reset()
            mMediaRecorder!!.release()
        }finally {
            mMediaRecorder = null
        }
    }
}