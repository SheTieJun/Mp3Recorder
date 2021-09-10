package me.shetj.mp3recorder.record


import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Message
import me.shetj.base.base.BaseService
import me.shetj.mp3recorder.record.utils.RecordCallBack
import me.shetj.mp3recorder.record.utils.RecordUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RecordService : BaseService() {


    private var callBacks: RecordCallBack? = null
    private var work: Work? = null
    private var createRecordUtils: RecordUtils? = null

    private fun getWork(): Work {
        if (work == null) {
            work = Work()
        }
        return work!!
    }

    override fun onBind(intent: Intent): IBinder? {
        return getWork()
    }


    inner class Work : Binder() {

        val isRecording: Boolean
            get() = createRecordUtils!!.isRecording

        fun stop() {
            createRecordUtils!!.stopFullRecord()
        }

        fun reRecord() {
            createRecordUtils?.reset()
            createRecordUtils?.setTime(0)
            statOrPause()
        }

        //注册接口
        fun setCallBack(callBack: RecordCallBack?) {
            callBacks = callBack
        }

        fun recordComplete() {
            createRecordUtils?.stopFullRecord()
        }

        fun setTime(audioLength: Int) {
            createRecordUtils?.setTime(audioLength)
        }

        @JvmOverloads
        fun statOrPause(file:String? =null) {
            if (file == null) {
                createRecordUtils?.startOrPause()
            }else{
                createRecordUtils?.startOrPause(file,isContinue = true)
            }
        }

        fun hasRecord(): Boolean {
            return createRecordUtils!!.hasRecord()
        }

        fun pause() {
            createRecordUtils?.pause()
        }

        fun getRecordUtil(): RecordUtils? {
            return createRecordUtils
        }
    }


    override suspend fun init() {
        createRecordUtils = RecordUtils( object : RecordCallBack {
            override fun start() {
                callBacks?.start()
                startForeground(110,RecordingNotification.getNotification(1,this@RecordService))
            }

            override fun onRecording(time: Int, volume: Int) {
                callBacks?.onRecording(time,volume)
            }

            override fun pause() {
                callBacks?.pause()
                startForeground(110,RecordingNotification.getNotification(2,this@RecordService))
            }

            override fun onSuccess(file: String, time: Int) {
                callBacks?.onSuccess(file, time)
                stopForeground(true)
                RecordingNotification.cancel(this@RecordService)
            }

            override fun onProgress(time: Int) {
                callBacks?.onProgress(time)
            }

            override fun onMaxProgress(time: Int) {
                callBacks?.onMaxProgress(time)
            }

            override fun onError(e: Exception) {
                callBacks?.onError(e)
            }

            override fun autoComplete(file: String, time: Int) {
                callBacks?.autoComplete(file, time)
                stopForeground(true)
                RecordingNotification.cancel(this@RecordService)
            }

            override fun needPermission() {
                callBacks?.needPermission()
            }
        })
        //用秒，内部有*1000
        createRecordUtils?.setMaxTime(1200)
    }

    override fun onDestroy() {
        createRecordUtils?.clear()
        stopForeground(true)
        RecordingNotification.cancel(this)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        createRecordUtils?.apply {
            if (hasRecord()) {
                createRecordUtils?.stopFullRecord()
            }
        }
        super.onTaskRemoved(rootIntent)
    }


}
