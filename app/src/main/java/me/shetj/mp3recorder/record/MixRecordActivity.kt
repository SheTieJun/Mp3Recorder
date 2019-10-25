package me.shetj.mp3recorder.record

import android.media.MediaRecorder
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_mix_record.*
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.file.SDCardUtils
import me.shetj.kt.simpleRecorderBuilder
import me.shetj.mixRecorder.MixRecorder
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.record.utils.LocalMusicUtils
import me.shetj.player.AudioPlayer
import me.shetj.player.SimPlayerListener
import me.shetj.player.SimRecordListener
import me.shetj.recorder.BaseRecorder
import me.shetj.recorder.RecordState
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * mix录音的demo
 */
class MixRecordActivity : AppCompatActivity() {

    private var stringBuffer: StringBuffer?=null
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
            stop()
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
                stop()
                audioPlayer.playOrPause(mp3Url!!,null)
            }
        }

        seek_bar.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mixRecorder?.setVolume(progress/100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        stringBuffer = StringBuffer()
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

    private fun stop() {
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
            ArmsUtils.makeText(   "请先开始录音")
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

    private fun showMsg(info:String) {
        AndroidSchedulers.mainThread().scheduleDirect {
            ArmsUtils.makeText(info)
        }
    }

    private fun mixRecord() {
        val  filePath = SDCardUtils.getPath("record") + "/" + System.currentTimeMillis() +  "bg.mp3"
        if (mixRecorder == null) {
            Timber.i("musicUrl = %s", musicUrl)
            val listener = object : SimRecordListener() {
                override fun onSuccess(file: String, time: Long) {
                    super.onSuccess(file, time)
                    Timber.i("file= %s", file)
                    mp3Url = file
                    stringBuffer?.append(file+"\n")
                    tv_msg_foot.text = stringBuffer.toString()
                }

                override fun onRecording(time: Long, volume: Int) {
                    super.onRecording(time, volume)
                    Timber.i("time = $time  ,volume = $volume")
                }
            }
            mixRecorder = simpleRecorderBuilder()
                .setBackgroundMusic(musicUrl!!)//设置默认的背景音乐
                .setRecordListener(listener)
                .setPermissionListener(listener)
                .setWax(1f)
                .setBackgroundMusicListener(object :SimPlayerListener(){
                    override fun onStart(url: String, duration: Int) {
                        super.onStart(url, duration)
                        showMsg("开始播放")
                    }

                    override fun onPause() {
                        super.onPause()
                        showMsg("onPause")
                    }

                    override fun onResume() {
                        super.onResume()
                        showMsg("onResume")
                    }

                    override fun onCompletion() {
                        super.onCompletion()
                        showMsg("结束")
                    }

                    override fun onProgress(current: Int, size: Int) {
                        super.onProgress(current, size)
                        Timber.i("current = $current,size = $size")
                    }
                })
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
        stop()
        super.onDestroy()
    }
}
