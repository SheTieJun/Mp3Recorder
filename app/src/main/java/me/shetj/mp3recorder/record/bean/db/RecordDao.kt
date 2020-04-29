package me.shetj.mp3recorder.record.bean.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Flowable
import me.shetj.mp3recorder.record.bean.Record


@Dao
interface RecordDao {

    @Insert(onConflict = REPLACE)
    fun insertRecord(record: Record): Completable


    @Delete()
    fun deleteRecord(record: Record):Completable


    @Query("SELECT * FROM record order by id")
    fun getAllRecord() : Flowable<List<Record>>


    @Query("select * from record order by id DESC limit 1 ")
    fun getLastRecord():Flowable<Record>


    @Query("DELETE FROM record")
    fun deleteAll():Completable
}