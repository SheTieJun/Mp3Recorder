
package me.shetj.mp3recorder.record.bean

import me.shetj.base.tools.file.FileUtils
import me.shetj.mp3recorder.record.bean.db.AppDatabase
import me.shetj.mp3recorder.record.bean.db.RecordDao
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shetj.base.BaseKit

class RecordDbUtils private constructor() {
    private val dbManager: RecordDao by lazy {
        AppDatabase.getInstance(BaseKit.app).recordDao()
    }

    /**
     * 获取全部的录音
     */
    val allRecord = dbManager.getAllRecord()


    suspend fun save(recordNew: Record) {
        withContext(Dispatchers.IO) {
            dbManager.insertRecord(recordNew)
        }

    }

    /**
     * 更新录音
     */
    suspend fun update(message: Record) {
        withContext(Dispatchers.IO) {
            dbManager.insertRecord(message)
        }
    }

    /**
     * 删除
     */
    suspend fun del(record: Record) {
        withContext(Dispatchers.IO) {
            dbManager.deleteRecord(record)
            FileUtils.deleteFile(File(record.audio_url!!))
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            dbManager.deleteAll()
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
