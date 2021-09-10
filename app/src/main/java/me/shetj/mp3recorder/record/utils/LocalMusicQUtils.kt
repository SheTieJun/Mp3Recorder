package me.shetj.mp3recorder.record.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableOnSubscribe
import io.reactivex.rxjava3.schedulers.Schedulers
import me.shetj.mp3recorder.record.bean.MusicQ
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * 本地音乐获取
 */
object LocalMusicQUtils {

    /**
     * 查询本地的音乐文件
     */
    @JvmStatic
    fun loadFileData(context: Context): Flowable<List<MusicQ>>? {
        return Flowable.create(FlowableOnSubscribe<List<MusicQ>> { emitter ->
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
            )

// Show only videos that are at least 5 minutes in duration.
            val selection = "${MediaStore.Video.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(
                TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES).toString()
            )

// Display videos in alphabetical order based on their display name.
            val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null
            )!!
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val musicList = ArrayList<MusicQ>()
            if (cursor.moveToFirst()) {
                do {
                    val title =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
//                    val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
//                    val url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val id = cursor.getLong(idColumn)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val duration =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val album =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    if (duration in 60000..2000000) {
                        val music = MusicQ()
                        music.name = title
                        music.url = contentUri
                        music.duration = duration
                        music.imgUrl = album
                        musicList.add(music)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            if (musicList.size > 0) {
                emitter.onNext(musicList)
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io())

    }
}