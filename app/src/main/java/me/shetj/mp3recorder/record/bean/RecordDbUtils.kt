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
