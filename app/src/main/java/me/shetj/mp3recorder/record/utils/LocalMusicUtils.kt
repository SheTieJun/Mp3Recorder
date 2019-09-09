package me.shetj.mp3recorder.record.utils

import android.content.Context
import android.provider.MediaStore
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.schedulers.Schedulers
import me.shetj.mp3recorder.record.bean.Music
import java.util.*

/**
 * 本地音乐获取
 */
object  LocalMusicUtils {

    /**
     * 查询本地的音乐文件
     */
    @JvmStatic
    fun loadFileData(context: Context): Flowable<List<Music>>? {
       return Flowable.create(FlowableOnSubscribe<List<Music>> { emitter ->
            val resolver = context.contentResolver
            val cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null)
            cursor!!.moveToFirst()
           val musicList = ArrayList<Music>()
            if (cursor.moveToFirst()) {
                do {
                    val title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                    val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                    val url = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                    if (duration in 60000..2000000) {
                        val music = Music()
                        music.name = title
                        music.url = url
                        music.duration = duration
                        music.imgUrl = album
                        musicList.add(music)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            if (musicList.size > 0) {
                emitter.onNext(musicList)
            } else {
                emitter.onError(Throwable("本地没有音乐~"))
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())

    }
}