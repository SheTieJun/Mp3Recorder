package me.shetj.mp3recorder.stress

import android.Manifest.permission
import android.os.Bundle
import android.os.SystemClock
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.shetj.base.ktx.hasPermission
import me.shetj.base.mvvm.viewbind.BaseBindingActivity
import me.shetj.base.mvvm.viewbind.BaseViewModel
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.mp3recorder.databinding.ActivityLameStressTestBinding
import me.shetj.ndk.lame.LameUtils
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.FileUtils
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.recorder
import me.shetj.recorder.simRecorder.buildSim
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

class LameStressTestActivity :
    BaseBindingActivity<ActivityLameStressTestBinding, BaseViewModel>() {

    private var runningJob: Job? = null
    private var stressDir: File? = null // 测试输出目录，退出时清理

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "多个录音同时录音测试"
    }

    override fun initBaseView() {
        mBinding.btnMic.setOnClickListener {
            if (runningJob?.isActive == true) return@setOnClickListener
            if (!hasPermission(permission.RECORD_AUDIO, isRequest = true)) return@setOnClickListener
            val count = readPositiveInt(mBinding.etCount, 3)
            val seconds = readPositiveInt(mBinding.etSeconds, 5)
            startJob {
                append("开始测试：多路麦克风录音 count=$count seconds=$seconds")
                runMicStress(count, seconds)
                append("结束测试：多路麦克风录音")
            }
        }

        mBinding.btnLame.setOnClickListener {
            if (runningJob?.isActive == true) return@setOnClickListener
            val count = readPositiveInt(mBinding.etCount, 3)
            val seconds = readPositiveInt(mBinding.etSeconds, 5)
            startJob {
                append("开始测试：多实例Lame编码 count=$count seconds=$seconds")
                runLameStress(count, seconds)
                append("结束测试：多实例Lame编码")
            }
        }
    }

    private fun startJob(block: suspend () -> Unit) {
        mBinding.tvLog.text = ""
        mBinding.btnMic.isEnabled = false
        mBinding.btnLame.isEnabled = false
        runningJob = lifecycleScope.launch {
            try {
                block()
            } finally {
                mBinding.btnMic.isEnabled = true
                mBinding.btnLame.isEnabled = true
            }
        }
    }

    private fun append(msg: String) {
        runOnUiThread {
            mBinding.tvLog.append(msg)
            mBinding.tvLog.append("\n")
        }
    }

    private fun readPositiveInt(editText: EditText, defaultValue: Int): Int {
        val value = editText.text?.toString()?.trim()?.toIntOrNull()
        return value?.takeIf { it > 0 } ?: defaultValue
    }

    private suspend fun runMicStress(count: Int, seconds: Int) {
        val outputDir = File(EnvironmentStorage.getPath(packagePath = "stress"))
        outputDir.mkdirs()
        stressDir = outputDir

        val recorders = ArrayList<BaseRecorder>(count)
        val startErrors = ArrayList<Pair<Int, String>>(count)

        repeat(count) { index ->
            val listener = object : RecordListener {
                override fun onStart() {
                    append("mic[$index] onStart")
                }

                override fun onResume() {
                    append("mic[$index] onResume")
                }

                override fun onReset() {
                    append("mic[$index] onReset")
                }

                override fun onRecording(time: Long, volume: Int) {
                }

                override fun onPause() {
                    append("mic[$index] onPause")
                }

                override fun onRemind(duration: Long) {
                    append("mic[$index] onRemind duration=$duration")
                }

                override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
                    append("mic[$index] onSuccess time=$time file=$file")
                }

                override fun onMaxChange(time: Long) {
                    append("mic[$index] onMaxChange time=$time")
                }

                override fun onError(e: Exception) {
                    append("mic[$index] onError ${e.message}")
                }
            }

            val permissionListener = object : PermissionListener {
                override fun needPermission() {
                    append("mic[$index] needPermission")
                }
            }

            val recorder = recorder {
                isDebug = true
                samplingRate = 48000
                audioSource = android.media.MediaRecorder.AudioSource.MIC
                audioChannel = 1
                mp3BitRate = 128
                mp3Quality = 5
                enableAudioEffect = false
                recordListener = listener
                this.permissionListener = permissionListener
            }.buildSim(this)

            val outFile = File(outputDir, "mic_${System.currentTimeMillis()}_${index}.mp3")
            recorder.setOutputFile(outFile.absolutePath, false)
            recorders.add(recorder)
        }

        recorders.forEachIndexed { index, recorder ->
            try {
                recorder.start()
            } catch (e: Exception) {
                startErrors.add(index to (e.message ?: e.javaClass.simpleName))
            }
            delay(50)
        }

        if (startErrors.isNotEmpty()) {
            startErrors.forEach { (index, err) ->
                append("mic[$index] start异常 $err")
            }
        }

        delay(seconds * 1000L)

        recorders.forEachIndexed { index, recorder ->
            try {
                recorder.complete()
            } catch (e: Exception) {
                append("mic[$index] complete异常 ${e.message}")
            }
        }

        delay(800)

        recorders.forEachIndexed { index, recorder ->
            try {
                recorder.destroy()
            } catch (e: Exception) {
                append("mic[$index] destroy异常 ${e.message}")
            }
        }
    }

    private data class LameResult(
        val index: Int,
        val ok: Boolean,
        val message: String,
        val filePath: String
    )

    private suspend fun runLameStress(count: Int, seconds: Int) {
        val outputDir = File(EnvironmentStorage.getPath(packagePath = "stress"))
        outputDir.mkdirs()
        stressDir = outputDir
        val startAt = SystemClock.elapsedRealtime()

        val results = coroutineScope {
            (0 until count).map { index ->
                async(Dispatchers.Default) {
                    val sampleRate = 44100
                    val lame = LameUtils()
                    val outFile = File(outputDir, "lame_${System.currentTimeMillis()}_${index}.mp3")

                    var out: FileOutputStream? = null
                    try {
                        lame.init(
                            inSampleRate = sampleRate,
                            inChannel = 1,
                            outSampleRate = sampleRate,
                            outBitrate = 128,
                            quality = 5,
                            lowpassFreq = -1,
                            highpassFreq = -1,
                            vbr = false,
                            enableLog = false
                        )
                        out = FileOutputStream(outFile)

                        val pcm = ShortArray(1024)
                        val mp3Buf = ByteArray(8192)
                        val flushBuf = ByteArray(8192)
                        val endAt = SystemClock.elapsedRealtime() + seconds * 1000L

                        var phase = 0.0
                        val step = 2.0 * PI * 440.0 / sampleRate
                        var total = 0L

                        while (SystemClock.elapsedRealtime() < endAt) {
                            for (i in pcm.indices) {
                                pcm[i] = (sin(phase) * Short.MAX_VALUE * 0.2).toInt().toShort()
                                phase += step
                                if (phase > 2.0 * PI) {
                                    phase -= 2.0 * PI
                                }
                            }

                            val encoded = lame.encode(pcm, pcm, pcm.size, mp3Buf)
                            if (encoded < 0) {
                                return@async LameResult(index, false, "encode=$encoded", outFile.absolutePath)
                            }
                            if (encoded > 0) {
                                out.write(mp3Buf, 0, encoded)
                                total += encoded
                            }
                        }

                        val flushed = lame.flush(flushBuf)
                        if (flushed < 0) {
                            return@async LameResult(index, false, "flush=$flushed", outFile.absolutePath)
                        }
                        if (flushed > 0) {
                            out.write(flushBuf, 0, flushed)
                            total += flushed
                        }

                        LameResult(index, true, "bytes=$total", outFile.absolutePath)
                    } catch (e: Exception) {
                        LameResult(index, false, e.message ?: e.javaClass.simpleName, outFile.absolutePath)
                    } finally {
                        try {
                            out?.flush()
                        } catch (_: Exception) {
                        }
                        try {
                            out?.close()
                        } catch (_: Exception) {
                        }
                        try {
                            lame.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            }.awaitAll()
        }

        val okCount = results.count { it.ok }
        val cost = SystemClock.elapsedRealtime() - startAt
        append("lame结果 ok=$okCount/$count costMs=$cost")
        results.forEach { result ->
            val fileSize = runCatching { File(result.filePath).length() }.getOrDefault(0L)
            append("lame[${result.index}] ok=${result.ok} size=$fileSize ${result.message} file=${result.filePath}")
        }
    }

    override fun onDestroy() {
        runningJob?.cancel()
        stressDir?.let { FileUtils.deleteFile(it.absolutePath) }
        super.onDestroy()
    }
}
