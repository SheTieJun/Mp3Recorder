
package me.shetj.mp3recorder.record.bean.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.shetj.mp3recorder.record.bean.Record


@Dao
interface RecordDao {

    @Insert(onConflict = REPLACE)
    fun insertRecord(record: Record)


    @Delete()
    fun deleteRecord(record: Record)


    @Query("SELECT * FROM record order by id DESC")
    fun getAllRecord() : Flow<List<Record>>


    @Query("select * from record order by id DESC limit 1 ")
    suspend fun getLastRecord():Record


    @Query("DELETE FROM record")
    suspend fun deleteAll()
}