package me.shetj.recorder

import android.widget.SeekBar
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.VolumeConfig


/**
 * 设置录制成功回调，不管是最大还是收到
 */
fun BaseRecorder.onSuccess(onSuccess: (isAutoComplete: Boolean, file: String, time: Long) -> Unit = { _: Boolean, _: String, _: Long -> }): BaseRecorder {
    return setRecordListener(onSuccess = onSuccess)
}

/**
 * 设置背景音乐播放,暂停和重新开始播放的监听
 */
fun BaseRecorder.onPlayChange(
    onPause: () -> Unit = {},
    onResume: () -> Unit = {}
): BaseRecorder {
    return setPlayListener(onPause = onPause, onResume = onResume)
}

/**
 * 设置背景音乐播放的监听
 */
fun BaseRecorder.setPlayListener(
    onStart: (duration: Int) -> Unit = { _: Int -> },
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: (throwable: Exception) -> Unit = {},
    onProgress: (current: Int, duration: Int) -> Unit = { _: Int, _: Int -> }
): BaseRecorder {
    setBackgroundMusicListener(object : PlayerListener {
        override fun onStart(duration: Int) {
            onStart(duration)
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
    return this
}

/**
 * 设置录制监听
 */
fun BaseRecorder.setRecordListener(
    onStart: () -> Unit = {},
    onResume: () -> Unit = {},
    onReset: () -> Unit = {},
    onRecording: (time: Long, volume: Int) -> Unit = { _: Long, _: Int -> },
    onPause: () -> Unit = {},
    onRemind: (mDuration: Long) -> Unit = {},
    onSuccess: (isAutoComplete: Boolean, file: String, time: Long) -> Unit = { _: Boolean, _: String, _: Long -> },
    setMaxProgress: (time: Long) -> Unit = {},
    onError: (e: Exception) -> Unit = {},
    needPermission: () -> Unit = {}
): BaseRecorder {

    setRecordListener(object : RecordListener {
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

        override fun onRemind(duration: Long) {
            onRemind(duration)
        }

        override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
            onSuccess(isAutoComplete, file, time)
        }


        override fun onMaxChange(time: Long) {
            setMaxProgress(time)
        }

        override fun onError(e: Exception) {
            onError(e)
        }

    })
    setPermissionListener(object : PermissionListener {
        override fun needPermission() {
            needPermission()
        }
    })
    return this
}

inline fun BaseRecorder.setVolumeSeekBar(
    mSeekBar: SeekBar,
    volumeConfig: VolumeConfig,
    crossinline onSeek: (seekBar: SeekBar, progress: Int) -> Unit
) {
    mSeekBar.max = volumeConfig.getMaxVoice()
    mSeekBar.progress = volumeConfig.getCurVolume()
    mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            seekBar?.let {
                onSeek(seekBar, progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            seekBar?.let {
                onSeek(seekBar, seekBar.progress)
            }
        }
    })
}