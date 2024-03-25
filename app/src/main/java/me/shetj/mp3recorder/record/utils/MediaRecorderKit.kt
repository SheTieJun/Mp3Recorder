
package me.shetj.mp3recorder.record.utils

import android.media.MediaRecorder
import android.os.Build
import me.shetj.base.BaseKit
import me.shetj.base.tools.file.EnvironmentStorage
import java.io.File

object MediaRecorderKit {

    var mMediaRecorder: MediaRecorder? = null
    var savePath: String? = null


    fun startRecord() {
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mMediaRecorder = MediaRecorder(BaseKit.app)
            }else{
                mMediaRecorder = MediaRecorder()
            }
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
            /**
             *  采样频率越高， 声音越接近原始数据。
             *  采样位数越高，声音越接近原始数据。
             *  比特率越高，传送的数据越大，音质越好
             */
            mMediaRecorder!!.setAudioEncodingBitRate(128) // 设置比特率
            mMediaRecorder!!.setAudioChannels(2) // 设置双声道
            mMediaRecorder!!.setAudioSamplingRate(44100) // 设置采样率
            mMediaRecorder!!.setMaxDuration(1000 * 60 * 60 * 24) // 设置最大录音时长
            mMediaRecorder!!.setMaxFileSize(10*1024*1024)
            savePath = EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".m4a"
            mMediaRecorder!!.setOutputFile(savePath)
            val nextSavePath = File(EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".m4a")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMediaRecorder!!.setNextOutputFile(nextSavePath)
            }
            /* ③准备 */
            mMediaRecorder!!.prepare()
            /* ④开始 */
            mMediaRecorder!!.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun pause(){
        mMediaRecorder?.pause()
    }

    fun resume(){
        mMediaRecorder?.resume()
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