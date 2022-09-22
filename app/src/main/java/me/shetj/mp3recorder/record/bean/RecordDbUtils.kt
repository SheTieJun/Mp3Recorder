/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.mp3recorder.record.bean

import me.shetj.base.S
import me.shetj.base.tools.file.FileUtils
import me.shetj.mp3recorder.record.bean.db.AppDatabase
import me.shetj.mp3recorder.record.bean.db.RecordDao
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shetj.base.ktx.doOnIO

class RecordDbUtils private constructor() {
    private val dbManager: RecordDao by lazy {
        AppDatabase.getInstance(S.app).recordDao()
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
