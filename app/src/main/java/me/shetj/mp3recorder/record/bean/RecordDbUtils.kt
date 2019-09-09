package me.shetj.mp3recorder.record.bean


import me.shetj.base.tools.file.FileUtils
import me.shetj.simxutils.DbManager
import me.shetj.simxutils.DbUtils
import me.shetj.simxutils.ex.DbException
import java.io.File
import java.util.*

class RecordDbUtils private constructor() {
    private val dbManager: DbManager = DbUtils.getDbManager("record", 3)

    /**
     * 获取全部的录音
     */
    val allRecord: List<Record>
        get() {
            try {
                val all = dbManager.selector(Record::class.java)
                        .where("user_id", "=", "1").orderBy("id", true)
                        .findAll()
                if (all != null) {
                    return all
                }
            } catch (e: DbException) {
                e.printStackTrace()
            }

            return ArrayList()
        }


    /**
     * 获取最后录制的录音
     */
    val lastRecord: Record?
        get() {
            try {
                val record = dbManager.selector(Record::class.java)
                        .where("user_id", "=", "1").orderBy("id", true)
                        .findFirst()
                if (record != null) {
                    return record
                }
            } catch (e: DbException) {
                e.printStackTrace()
            }

            return null
        }


    fun save(recordNew: Record) {
        try {
            dbManager.save(recordNew)
        } catch (e: DbException) {
            e.printStackTrace()
        }

    }

    /**
     * 更新录音
     */
    fun update(message: Record) {
        try {
            dbManager.saveOrUpdate(message)
        } catch (e: DbException) {
            e.printStackTrace()
        }

    }

    /**
     * 删除
     */
    fun del(record: Record) {
        try {
            dbManager.delete(record)
            FileUtils.deleteFile(File(record.audio_url!!))
        } catch (e: DbException) {
            e.printStackTrace()
        }

    }

    fun clear() {
        try {
            dbManager.delete(Record::class.java)
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
