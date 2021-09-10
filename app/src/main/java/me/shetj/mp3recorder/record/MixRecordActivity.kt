package me.shetj.mp3recorder.record

import android.os.Bundle
import android.text.TextUtils
import me.shetj.base.ktx.showToast
import me.shetj.base.mvvm.BaseBindingActivity
import me.shetj.base.mvvm.BaseViewModel
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.mp3recorder.R
import me.shetj.mp3recorder.databinding.ActivityMixRecordBinding
import me.shetj.mp3recorder.mp3RecorderNoContext
import me.shetj.mp3recorder.record.utils.LocalMusicUtils
import me.shetj.player.AudioPlayer
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.setPlayListener
import me.shetj.recorder.setRecordListener
import timber.log.Timber

/**
 * mix录音的demo
 */
class MixRecordActivity : BaseBindingActivity<BaseViewModel,ActivityMixRecordBinding>() {

    private var musicUrl: String? =null
    private var mixRecorder: BaseRecorder?=null
    private var position = 0
    private var mp3Url :String ?=null
    private val audioPlayer = AudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mix_record)
        LocalMusicUtils.loadFileData(this)!!.subscribe { music -> musicUrl = music[0].url }
        mViewBinding.btStart.setOnClickListener {
            //如果在播放先暂停播放
            audioPlayer.pause()
            //开始录音
            mixRecord()
        }

        mViewBinding.btPause.setOnClickListener {
            recordPauseOrResume()
        }

        mViewBinding.btStartBg.setOnClickListener {
            startOrPause()
        }

        mViewBinding.btStop.setOnClickListener {
            stopRecord()
        }

        mViewBinding.btChangeBg.setOnClickListener {
            if (mixRecorder?.state == RecordState.RECORDING) {
                changeMusic()
            }else{
                ArmsUtils.makeText(   "请先开始录音")
            }
        }

       mViewBinding.btAudition.setOnClickListener {
            mp3Url?.let {
                //先暂停再开始播放
                stopRecord()
                audioPlayer.playOrPause(mp3Url!!,null)
            }
        }

    }

    private fun changeMusic() {
        try {
            position = when (position) {
                0 -> 1
                1->2
                2->3
                else -> 0
            }
            musicUrl = LocalMusicUtils.loadFileData(this)!!.blockingFirst()[position].url
            /**
             * 切换背景音乐
             */
            mixRecorder?.setBackgroundMusic(musicUrl!!)
            /**
             * 开始播放背景音乐，demo是直接开始播放背景音乐
             */
            mixRecorder?.startPlayMusic()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun stopRecord() {
        mixRecorder?.complete()
    }

    private fun recordPauseOrResume() {
        when (mixRecorder?.state) {
            RecordState.PAUSED -> {
                mixRecorder?.resume()
            }
            RecordState.RECORDING -> {
                mixRecorder?.pause()
            }
            else -> {
                ArmsUtils.makeText(   "请先开始录音")
            }
        }
    }


    private fun startOrPause() {
        if (mixRecorder?.state != RecordState.RECORDING) {
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
            mixRecorder = mp3RecorderNoContext(
                BaseRecorder.RecorderType.MIX,
                BaseRecorder.AudioSource.MIC,
                channel = BaseRecorder.AudioChannel.STEREO)
                .setBackgroundMusic(musicUrl!!)//设置默认的背景音乐
                .setRecordListener (  onRecording = { time, volume ->
                    Timber.i("time = $time  ,volume = $volume")
                },onSuccess = { _,file, _ ->
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
        if (mixRecorder?.state != RecordState.RECORDING) {
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
