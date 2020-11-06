package me.shetj.mp3recorder.record.utils

import android.R
import android.content.Context
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import me.shetj.base.tools.file.EnvironmentStorage
import kotlin.Throws
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaMetadataRetriever
import java.io.*
import java.lang.Exception

object Util {
    fun isBrightColor(color: Int): Boolean {
        if (R.color.transparent == color) {
            return true
        }
        val rgb = intArrayOf(Color.red(color), Color.green(color), Color.blue(color))
        val brightness = Math.sqrt(
            rgb[0] * rgb[0] * 0.241 + rgb[1] * rgb[1] * 0.691 + rgb[2] * rgb[2] * 0.068
        ).toInt()
        return brightness >= 200
    }

    fun getDarkerColor(color: Int): Int {
        val factor = 0.8f
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(
            a,
            Math.max((r * factor).toInt(), 0),
            Math.max((g * factor).toInt(), 0),
            Math.max((b * factor).toInt(), 0)
        )
    }

    fun formatSeconds(seconds: Int): String {
        return (getTwoDecimalsValue(seconds / 3600) + ":"
                + getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60))
    }

    fun formatSeconds2(seconds: Int): String {
        var seconds = seconds
        if (seconds > 3600) {
            seconds = 3600
        }
        return (getTwoDecimalsValue(seconds / 60) + "分"
                + getTwoDecimalsValue(seconds % 60) + "秒")
    }

    fun formatSeconds3(seconds: Int): String {
        var seconds = seconds
        if (seconds > 3600) {
            seconds = 3600
        }
        return (getTwoDecimalsValue(seconds / 60) + ":"
                + getTwoDecimalsValue(seconds % 60))
    }

    private fun getTwoDecimalsValue(value: Int): String {
        return if (value in 0..9) {
            "0$value"
        } else {
            value.toString() + ""
        }
    }

    fun combineMp3(path: String, path1: String): String? {
        return Flowable.zip(
            getFenLiData(path),
            getFenLiData(path1),
            { path0: String?, path2: String? -> heBingMp3(path0, path2) })
            .subscribeOn(Schedulers.io())
            .blockingFirst()
    }

    /**
     * 返回合并后的文件的路径名,默认放在第一个文件的目录下
     */
    fun heBingMp3(path: String?, path1: String?): String? {
        return try {
            val file = File(path)
            val file1 = File(path1)
            val hebing =
                EnvironmentStorage.getPath(packagePath = "record") + "/" + System.currentTimeMillis() + ".mp3"
            val file2 = File(hebing)
            var `in` = FileInputStream(file)
            var out = FileOutputStream(file2)
            val bs = ByteArray(1024 * 4)
            var len = 0
            //先读第一个
            while (`in`.read(bs).also { len = it } != -1) {
                out.write(bs, 0, len)
            }
            `in`.close()
            out.close()
            //再读第二个
            `in` = FileInputStream(file1)
            out = FileOutputStream(file2, true) //在文件尾打开输出流
            len = 0
            val bs1 = ByteArray(1024 * 4)
            while (`in`.read(bs1).also { len = it } != -1) {
                out.write(bs1, 0, len)
            }
            `in`.close()
            out.close()
            if (file.exists()) file.delete()
            if (file1.exists()) file1.delete()
            file2.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun getFenLiData(path: String): Flowable<String> {
        return Flowable.just(path).map { s: String? ->
            val fenLiData = fenLiData(path)
            fenLiData
        }.subscribeOn(Schedulers.io())
    }

    /**
     * 返回分离出MP3文件中的数据帧的文件路径
     *
     * @作者 胡楠启
     */
    @Throws(IOException::class)
    fun fenLiData(path: String): String {
        val file = File(path) // 原文件
        val file1 = File(path + "01") // 分离ID3V2后的文件,这是个中间文件，最后要被删除
        val file2 = File(path + "001") // 分离id3v1后的文件
        val rf = RandomAccessFile(file, "rw") // 随机读取文件
        val fos = FileOutputStream(file1)
        val ID3 = ByteArray(3)
        rf.read(ID3)
        val ID3str = String(ID3)
        // 分离ID3v2
        if (ID3str == "ID3") {
            rf.seek(6)
            val ID3size = ByteArray(4)
            rf.read(ID3size)
            val size1: Int = ID3size[0].toInt() and 0x7f shl 21
            val size2: Int = ID3size[1].toInt() and 0x7f shl 14
            val size3: Int = ID3size[2].toInt() and 0x7f shl 7
            val size4: Int = ID3size[3].toInt() and 0x7f
            val size = size1 + size2 + size3 + size4 + 10
            rf.seek(size.toLong())
            var lens = 0
            val bs = ByteArray(1024 * 4)
            while (rf.read(bs).also { lens = it } != -1) {
                fos.write(bs, 0, lens)
            }
            fos.close()
            rf.close()
        } else { // 否则完全复制文件
            var lens = 0
            rf.seek(0)
            val bs = ByteArray(1024 * 4)
            while (rf.read(bs).also { lens = it } != -1) {
                fos.write(bs, 0, lens)
            }
            fos.close()
            rf.close()
        }
        val raf = RandomAccessFile(file1, "rw")
        val TAG = ByteArray(3)
        raf.seek(raf.length() - 128)
        raf.read(TAG)
        val tagstr = String(TAG)
        if (tagstr == "TAG") {
            val fs = FileOutputStream(file2)
            raf.seek(0)
            val bs = ByteArray((raf.length() - 128).toInt())
            raf.read(bs)
            fs.write(bs)
            raf.close()
            fs.close()
        } else { // 否则完全复制内容至file2
            val fs = FileOutputStream(file2)
            raf.seek(0)
            val bs = ByteArray(1024 * 4)
            var len = 0
            while (raf.read(bs).also { len = it } != -1) {
                fs.write(bs, 0, len)
            }
            raf.close()
            fs.close()
        }
        if (file1.exists()) // 删除中间文件
        {
            file1.delete()
        }
        return file2.absolutePath
    }

    /**
     * 针对6.0动态请求权限问题
     * 判断是否允许此权限
     *
     * @param permissions  权限
     * @return hasPermission
     */
    fun hasPermission(context: Context?, vararg permissions: String?): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context!!, permission!!)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun getAudioLength(filename: String?): Int {
      return  filename?.let { getAudioLength(it).toInt()/1000 }?:0
    }


    private fun getAudioLength(filename: String): String {
        val mmr = MediaMetadataRetriever();
        var duration = "1"
        try {
            mmr.setDataSource(filename);
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        } catch (ex: Exception) {
        } finally {
            mmr.release()
        }
        return duration
    }
}