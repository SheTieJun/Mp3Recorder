package me.shetj.mp3recorder.record.bean.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.shetj.mp3recorder.record.bean.Record


@Database(entities = [Record::class],version = 2,exportSchema = false)
abstract class AppDatabase  : RoomDatabase() {

    abstract fun recordDao():RecordDao

    companion object{

       @Volatile private var INSTANCE : AppDatabase ?=null

        fun getInstance(context: Context):AppDatabase {
            return INSTANCE ?: synchronized(this){
              INSTANCE?:buildDataBase(context).also {
                  INSTANCE = it
              }
            }
        }

        private fun buildDataBase(context: Context) = Room.databaseBuilder(context.applicationContext,
            AppDatabase::class.java,"record.db")
                .build()
    }


}