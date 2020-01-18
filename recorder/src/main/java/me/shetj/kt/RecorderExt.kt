package me.shetj.kt

import me.shetj.recorder.mixRecorder.MixRecorder
import me.shetj.player.PermissionListener
import me.shetj.player.PlayerListener
import me.shetj.player.RecordListener
import me.shetj.recorder.simRecorder.BaseRecorder
import me.shetj.recorder.BuildConfig
import me.shetj.recorder.simRecorder.MP3Recorder

/**
 * 通过背景音乐录制的背景音乐
 */
@JvmOverloads
fun simRecorderBuilder(audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
                       isDebug: Boolean = BuildConfig.DEBUG,
                       mMaxTime: Int = 1800 * 1000,
                       mp3Quality: Int = 5,
                       permissionListener: PermissionListener ? =null,
                       recordListener: RecordListener ?= null,
                       wax: Float = 1f

): MP3Recorder {
    return MP3Recorder(audioSource.type, isDebug)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax)
}


/**
 * 混合音频的录制
 */
@JvmOverloads
fun mixRecorderBuilder(audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
                       channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.MONO,
                       mMaxTime: Int = 1800 * 1000,
                       mp3Quality: Int = 5,
                       permissionListener: PermissionListener ? =null,
                       recordListener: RecordListener ?= null,
                       wax: Float = 1f

): MixRecorder {
    return MixRecorder(audioSource.type,channel.type)
        .setMaxTime(mMaxTime)
        .setMp3Quality(mp3Quality)
        .setPermissionListener(permissionListener)
        .setRecordListener(recordListener)
        .setWax(wax)
}

/**
 * mix 表示混合
 * sim 不支持单双声道录制
 */
@JvmOverloads
fun simpleRecorderBuilder(simpleName: BaseRecorder.RecorderType = BaseRecorder.RecorderType.MIX,
                          audioSource: BaseRecorder.AudioSource = BaseRecorder.AudioSource.MIC,
                          mMaxTime: Int = 1800 * 1000,
                          mp3Quality: Int = 5,
                          channel: BaseRecorder.AudioChannel = BaseRecorder.AudioChannel.MONO,
                          permissionListener: PermissionListener ? =null,
                          recordListener: RecordListener ?= null,
                          wax: Float = 1f
): BaseRecorder {
    return when(simpleName){
        BaseRecorder.RecorderType.MIX ->
            mixRecorderBuilder(audioSource = audioSource,
                channel = channel,
                mMaxTime = mMaxTime,
                mp3Quality = mp3Quality,
                permissionListener = permissionListener,
                recordListener = recordListener,
                wax = wax)
        BaseRecorder.RecorderType.SIM  ->
            simRecorderBuilder(audioSource = audioSource,
                mMaxTime = mMaxTime,
                mp3Quality = mp3Quality,
                permissionListener = permissionListener,
                recordListener = recordListener,
                wax = wax)
    }
}

/**
 * 设置录制成功回调，不管是最大还是收到
 */
fun BaseRecorder.onSuccess(onSuccess:(file: String, time: Long)->Unit = { _: String, _: Long-> }): BaseRecorder {
    return setRecordListener(onSuccess = onSuccess,autoComplete = onSuccess)
}

/**
 * 设置背景音乐播放,暂停和重新开始播放的监听
 */
fun BaseRecorder.onPlayChange(onPause:()->Unit = {},
                                                            onResume:()->Unit = {}): BaseRecorder {
    return setPlayListener(onPause = onPause,onResume = onResume)
}

/**
 * 设置背景音乐播放的监听
 */
fun BaseRecorder.setPlayListener(onStart:(url: String, duration: Int)->Unit = { _: String, _: Int ->},
                                                               onPause:()->Unit = {},
                                                               onResume:()->Unit = {},
                                                               onStop:()->Unit = {},
                                                               onCompletion:()->Unit = {},
                                                               onError:(throwable: Exception)->Unit = {},
                                                               onProgress:(current: Int, duration: Int)->Unit = { _: Int, _: Int->}): BaseRecorder {
    setBackgroundMusicListener(object :PlayerListener{
        override fun onStart(url: String, duration: Int) {
            onStart(url, duration)
        }

        override fun onPause() {
            onPause()
        }

        override fun onResume() {
            onResume()
        }

        override fun onStop() {
            onStop()
        }

        override fun onCompletion() {
            onCompletion()
        }

        override fun onError(throwable: Exception) {
            onError(throwable)
        }

        override fun onProgress(current: Int, duration: Int) {
            onProgress(current, duration)
        }
    })
    return  this
}

/**
 * 设置录制监听
 */
fun BaseRecorder.setRecordListener(onStart:()->Unit = {},
                                                                 onResume:()->Unit = {},
                                                                 onReset:()->Unit = {},
                                                                 onRecording:(time: Long, volume: Int)->Unit = { _: Long, _: Int -> },
                                                                 onPause:()->Unit = {},
                                                                 onRemind:(mDuration: Long)->Unit = {},
                                                                 onSuccess:(file: String, time: Long)->Unit = { _: String, _: Long-> },
                                                                 setMaxProgress:(time: Long)->Unit = {},
                                                                 onError:(e: Exception)->Unit = {},
                                                                 autoComplete:(file: String, time: Long)->Unit = { _: String, _: Long -> },
                                                                 needPermission:()->Unit = {}): BaseRecorder {

    setRecordListener(object :RecordListener{
        override fun onStart() {
            onStart()
        }

        override fun onResume() {
            onResume()
        }

        override fun onReset() {
            onReset()
        }

        override fun onRecording(time: Long, volume: Int) {
            onRecording(time, volume)
        }

        override fun onPause() {
            onPause()
        }

        override fun onRemind(mDuration: Long) {
            onRemind(mDuration)
        }

        override fun onSuccess(file: String, time: Long) {
            onSuccess(file, time)
        }

        override fun setMaxProgress(time: Long) {
            setMaxProgress(time)
        }

        override fun onError(e: Exception) {
            onError(e)
        }

        override fun autoComplete(file: String, time: Long) {
            autoComplete(file, time)
        }
    })
    setPermissionListener(object :PermissionListener{
        override fun needPermission() {
            needPermission()
        }
    })
    return this
}