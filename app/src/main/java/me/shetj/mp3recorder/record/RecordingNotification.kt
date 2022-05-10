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
package me.shetj.mp3recorder.record

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import me.shetj.mp3recorder.R


/**
 * 录音通知栏
 */
object RecordingNotification {

    const val RECORD_NOTIFICATION_ID = 110
    const val RECORD_NOTIFICATION_RECORD_ING = 1
    const val RECORD_NOTIFICATION_RECORD_PAUSE = RECORD_NOTIFICATION_RECORD_ING + 1
    const val RECORD_NOTIFICATION_RECORD_COMPLETE = RECORD_NOTIFICATION_RECORD_PAUSE + 1
    fun notify(context: Context) {
        //默认没有播放 需要用户点击开始播放
        notify(context, 3)
    }

    /**
     * @param type  [type]== 1 表示 正在录音   [type] == 2 表示 暂停录音  [type] == 3 表示 完成录音
     * @param bitmap 展示图片
     */
    @SuppressLint("ObsoleteSdkInt")
    fun notify(context: Context, type: Int) {

        val notification = getNotification(type, context)
        notify(context, notification)
    }

    fun getNotification(type: Int, context: Context): Notification {
        val content = when (type) {
            RECORD_NOTIFICATION_RECORD_ING -> "正在录音..."
            RECORD_NOTIFICATION_RECORD_PAUSE -> "录音已暂停"
            RECORD_NOTIFICATION_RECORD_COMPLETE -> "录音已完成"
            else -> "录音工具"
        }
        val intents =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val intentGo = PendingIntent.getActivity(
            context, 110,
            intents,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, createNotificationChannel(context))
            .setSmallIcon(R.mipmap.record)
            .setContentTitle("录音Mp3Recorder")
            .setContentText(content)
            .setOngoing(false)
            .setContentIntent(intentGo)
            .setSound(null)
            .setColor(ContextCompat.getColor(context.applicationContext, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_MAX)
        return builder.build()
    }


    private fun notify(context: Context, notification: Notification) {
        NotificationManagerCompat.from(context).notify(RECORD_NOTIFICATION_ID, notification)
    }

    /**
     * Cancels any notifications of this type previously shown using
     * [.notify].
     */
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(RECORD_NOTIFICATION_ID)
    }

    /**
     * 创建通知栏渠道ID
     */
    private fun createNotificationChannel(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "录音机"
            val channelName = "录音机"
            val channelDescription = "录音机"
            /*
                NotificationManager.IMPORTANCE_NONE          关闭通知
                NotificationManager.IMPORTANCE_MIN              开启通知，不会弹出，但没有提示音，状态栏中无显示
                NotificationManager.IMPORTANCE_LOW             开启通知，不会弹出，不发出提示音，状态栏中显示
                NotificationManager.IMPORTANCE_DEFAULT     开启通知，不会弹出，发出提示音，状态栏中显示
                NotificationManager. IMPORTANCE_HIGH           开启通知，会弹出，发出提示音，状态栏中显示
             */
            val channelImportance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(channelId, channelName, channelImportance)
            // 设置描述 最长30字符
            notificationChannel.description = channelDescription
            // 该渠道的通知是否使用震动
            notificationChannel.enableVibration(false)
            // 设置显示模式
            notificationChannel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            // 不要呼吸灯
            notificationChannel.enableLights(false)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            return channelId
        } else {
            return ""
        }
    }


}
