package me.shetj.mp3recorder.record.bean


import io.reactivex.schedulers.Schedulers
import me.shetj.base.s
import me.shetj.base.tools.file.FileUtils
import me.shetj.mp3recorder.record.bean.db.AppDatabase
import me.shetj.mp3recorder.record.bean.db.RecordDao
import me.shetj.simxutils.DbManager
import me.shetj.simxutils.DbUtils
import me.shetj.simxutils.ex.DbException
import java.io.File
import java.util.*

class RecordDbUtils private constructor() {
    private val dbManager: RecordDao by lazy {
        AppDatabase.getInstance(s.app).recordDao()
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
        try {
            dbManager.insertRecord(recordNew).subscribeOn(Schedulers.io()).subscribe()
        } catch (e: DbException) {
            e.printStackTrace()
        }

    }

    /**
     * 更新录音
     */
    fun update(message: Record) {
        try {
            dbManager.insertRecord(message).subscribeOn(Schedulers.io()).subscribe()
        } catch (e: DbException) {
            e.printStackTrace()
        }

    }

    /**
     * 删除
     */
    fun del(record: Record) {
        try {
            dbManager.deleteRecord(record).subscribeOn(Schedulers.io()).subscribe()
            FileUtils.deleteFile(File(record.audio_url!!))
        } catch (e: DbException) {
            e.printStackTrace()
        }

    }

    fun clear() {
        try {
            dbManager.deleteAll().subscribeOn(Schedulers.io()).subscribe()
        } catch (e: DbException) {
            e.printStackTrace()
        }

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
