package me.shetj.mp3recorder.record


import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import me.shetj.base.base.BaseService
import me.shetj.mp3recorder.record.bean.Record
import me.shetj.mp3recorder.record.utils.RecordCallBack
import me.shetj.mp3recorder.record.utils.RecordUtils

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


    override fun init() {
        createRecordUtils = RecordUtils( object : RecordCallBack {
            override fun start() {
                callBacks?.start()
                RecordingNotification.notify(this@RecordService, 1)
            }

            override fun onRecording(time: Int, volume: Int) {
                callBacks?.onRecording(time,volume)
            }

            override fun pause() {
                callBacks?.pause()
                RecordingNotification.notify(this@RecordService, 2)
            }

            override fun onSuccess(file: String, time: Int) {
                callBacks?.onSuccess(file, time)
                RecordingNotification.notify(this@RecordService, 3)
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
            Log.i("RecordService", "onTaskRemoved")
            if (hasRecord()) {
                createRecordUtils?.stopFullRecord()
            }
        }
        super.onTaskRemoved(rootIntent)
    }
}
