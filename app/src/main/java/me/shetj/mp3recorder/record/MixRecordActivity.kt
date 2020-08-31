package me.shetj.mp3recorder.record

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding4.view.clicks
import kotlinx.android.synthetic.main.activity_mix_record.*
import me.shetj.base.ktx.showToast
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.kt.setPlayListener
import me.shetj.kt.setRecordListener
import me.shetj.kt.simRecorderNoContext
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.utils.LocalMusicUtils
import me.shetj.player.AudioPlayer
import me.shetj.recorder.simRecorder.BaseRecorder
import me.shetj.recorder.simRecorder.RecordState
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * mix录音的demo
 */
class MixRecordActivity : AppCompatActivity() {

    private var musicUrl: String? =null
    private var mixRecorder: BaseRecorder?=null
    private var position = 0
    private var mp3Url :String ?=null
    private val audioPlayer = AudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mix_record)
        LocalMusicUtils.loadFileData(this)!!.subscribe { music -> musicUrl = music[0].url }
        bt_start.setOnClickListener {
            //如果在播放先暂停播放
            audioPlayer.pause()
            //开始录音
            mixRecord()
        }

        bt_pause.setOnClickListener {
            recordPauseOrResume()
        }

        bt_start_bg.setOnClickListener {
            startOrPause()
        }

        bt_stop.setOnClickListener {
            stopRecord()
        }

        bt_change_bg.setOnClickListener {
            changeMusic()
        }

        bt_change_bg
            .clicks()
            .throttleFirst(1000,TimeUnit.MILLISECONDS)
            .subscribe {
                if (mixRecorder?.state == RecordState.RECORDING) {
                    changeMusic()
                }else{
                    ArmsUtils.makeText(   "请先开始录音")
                }
            }

        bt_audition.setOnClickListener {
            mp3Url?.let {
                //先暂停再开始播放
                stopRecord()
                audioPlayer.playOrPause(mp3Url!!,null)
            }
        }

    }

    private fun changeMusic() {
        position = when (position) {
            0 -> 1
            1->2
            2->3
            else -> 0
        }
        musicUrl=   LocalMusicUtils.loadFileData(this)!!.blockingFirst()[position].url
        /**
         * 切换背景音乐
         */
        mixRecorder?.setBackgroundMusic(musicUrl!!)
        /**
         * 开始播放背景音乐，demo是直接开始播放背景音乐
         */
        mixRecorder?.startPlayMusic()
    }

    private fun stopRecord() {
        mixRecorder?.let {
            if (mixRecorder!!.isRecording) {
                mixRecorder?.stop()
                ArmsUtils.makeText("停止录音")
            }
        }
    }

    private fun recordPauseOrResume() {
        when {
            mixRecorder?.state == RecordState.PAUSED -> {
                mixRecorder?.onResume()
            }
            mixRecorder?.state == RecordState.RECORDING -> {
                mixRecorder?.onPause()
            }
            else ->{
                ArmsUtils.makeText(   "请先开始录音")
            }
        }
    }

    private fun startOrPause() {
        if (mixRecorder!=null && !mixRecorder!!.isRecording) {
            ArmsUtils.makeText(  "请先开始录音")
            return
        }
        mixRecorder?.let {
            if (it.isPauseMusic()){
                it.resumeMusic()
            }else{
                it.pauseMusic()
            }
        }
    }

    private fun mixRecord() {
        val  filePath = EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() +  "bg.mp3"
        if (mixRecorder == null) {
//            mixRecorder = simpleRecorderBuilder(BaseRecorder.RecorderType.MIX,BaseRecorder.AudioSource.VOICE_COMMUNICATION)
            mixRecorder = simRecorderNoContext(BaseRecorder.RecorderType.MIX,
                BaseRecorder.AudioSource.MIC,
                channel = BaseRecorder.AudioChannel.STEREO)
                .setBackgroundMusic(musicUrl!!)//设置默认的背景音乐
                .setRecordListener(onRecording = { time, volume ->
                    Timber.i("time = $time  ,volume = $volume")
                },onSuccess = { file, _ ->
                    "录制成功：$file".showToast()
                    Timber.i("file= %s", file)
                })
                .setPlayListener(onProgress = {current: Int, duration: Int ->
                    Timber.i("current = $current  ,duration = $duration")
                })
                .setWax(1f)
                .setMaxTime(1800 * 1000) //设置最大时间
        }
        mixRecorder!!.setOutputFile(filePath)//设置输出文件
        if (!mixRecorder!!.isRecording) {
            mixRecorder!!.start()
            mixRecorder!!.startPlayMusic()
            ArmsUtils.makeText("开始录音")
        }
    }

    override fun onDestroy() {
        stopRecord()
        super.onDestroy()
    }
}
