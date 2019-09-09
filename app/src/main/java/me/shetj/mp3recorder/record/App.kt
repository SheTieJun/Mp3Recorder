package me.shetj.mp3recorder.record

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import me.shetj.base.s
import me.shetj.mp3recorder.BuildConfig

/**
 *
 * <b>@packageName：</b> com.shetj.diyalbume<br>
 * <b>@author：</b> shetj<br>
 * <b>@createTime：</b> 2017/12/4<br>
 * <b>@company：</b><br>
 * <b>@email：</b> 375105540@qq.com<br>
 * <b>@describe</b><br>
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        s.init(this, BuildConfig.DEBUG )
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

}