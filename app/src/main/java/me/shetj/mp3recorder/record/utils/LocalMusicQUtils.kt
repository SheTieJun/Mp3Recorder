package me.shetj.mp3recorder.record.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.shetj.mp3recorder.record.bean.MusicQ
import java.util.*


/**
 * 本地音乐获取
 */
object LocalMusicQUtils {

    /**
     * 查询本地的音乐文件
     */
    @SuppressLint("Range")
    @JvmStatic
    fun loadFileData(context: Context): Flow<List<MusicQ>> {
        return flow {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ARTIST
            )
            val musicList = ArrayList<MusicQ>()
            createCursor(
                contentResolver = context.contentResolver,
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection = projection,
                orderBy = MediaStore.Images.Media.DATE_ADDED,
                orderAscending = false,
                limit = 1000,
                offset = 0
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                if (cursor.moveToFirst()) {
                    do {
                        val title =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
                        val id = cursor.getLong(idColumn)
                        val contentUri: Uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                        val duration =
                            cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                        val album =
                            cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                        val music = MusicQ()
                        music.name = title
                        music.url = contentUri
                        music.duration = duration
                        music.imgUrl = album
                        musicList.add(music)
                    } while (cursor.moveToNext())
                }
            }
            emit(musicList)
        }
    }




    private fun createCursor(
        contentResolver: ContentResolver,
        collection: Uri,
        projection: Array<String>,
        orderBy: String,
        orderAscending: Boolean,
        limit: Int = 20,
        offset: Int = 0
    ): Cursor? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            val selection = createSelectionBundle(orderBy, orderAscending, limit, offset)
            contentResolver.query(collection, projection, selection, CancellationSignal())
        }

        else -> {
            val orderDirection = if (orderAscending) "ASC" else "DESC"
            var order = when (orderBy) {
                "ALPHABET" -> "${MediaStore.Audio.Media.TITLE}, ${MediaStore.Audio.Media.ARTIST} $orderDirection"
                else -> "${MediaStore.Audio.Media.DATE_ADDED} $orderDirection"
            }
            order += " LIMIT $limit OFFSET $offset"
            contentResolver.query(collection, projection, null, null, order)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createSelectionBundle(
        orderBy: String,
        orderAscending: Boolean,
        limit: Int = 20,
        offset: Int = 0
    ): Bundle = Bundle().apply {
        putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        when (orderBy) {
            "ALPHABET" -> putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.TITLE)
            )

            else -> putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
            )
        }
        val orderDirection =
            if (orderAscending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, orderDirection)
    }

}