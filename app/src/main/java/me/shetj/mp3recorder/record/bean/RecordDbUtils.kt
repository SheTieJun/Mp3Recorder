package me.shetj.mp3recorder.record.bean

import io.reactivex.rxjava3.schedulers.Schedulers
import me.shetj.base.S
import me.shetj.base.tools.file.FileUtils
import me.shetj.mp3recorder.record.bean.db.AppDatabase
import me.shetj.mp3recorder.record.bean.db.RecordDao
import java.io.File

class RecordDbUtils private constructor() {
    private val dbManager: RecordDao by lazy {
        AppDatabase.getInstance(S.app).recordDao()
    }

    /**
     * 获取全部的录音
     */
    val allRecord = dbManager.getAllRecord()


    /**
     * 获取最后录制的录音
     */
    val lastRecord = dbManager.getLastRecord()


    fun save(recordNew: Record) {
        dbManager.insertRecord(recordNew).subscribeOn(Schedulers.io()).subscribe()
    }

    /**
     * 更新录音
     */
    fun update(message: Record) {
        dbManager.insertRecord(message).subscribeOn(Schedulers.io()).subscribe()
    }

    /**
     * 删除
     */
    fun del(record: Record) {
        dbManager.deleteRecord(record).subscribeOn(Schedulers.io()).subscribe()
        FileUtils.deleteFile(File(record.audio_url!!))

    }

    fun clear() {
        dbManager.deleteAll().subscribeOn(Schedulers.io()).subscribe()
    }

    companion object {
        private var instance: RecordDbUtils? = null

        fun getInstance(): RecordDbUtils {
            if (instance == null) {
                synchronized(RecordDbUtils::class.java) {
                    if (instance == null) {
                        instance = RecordDbUtils()
                    }
                }
            }
            return instance!!
        }
    }
}
