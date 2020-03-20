package me.shetj.mp3recorder

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding3.view.clicks
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.record.MixRecordActivity
import me.shetj.mp3recorder.record.activity.mix.MyMixRecordActivity
import me.shetj.recorder.util.LameUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("MainActivity",LameUtils.version())
        RxPermissions(this).request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
        ,Manifest.permission.RECORD_AUDIO).subscribe()

        btn_demo.clicks()
            .compose(RxPermissions(this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
                ,Manifest.permission.RECORD_AUDIO))
            .subscribe {
                ArmsUtils.startActivity(this,
                    MyMixRecordActivity::class.java)
            }

        btn_demo2.clicks()
            .compose(RxPermissions(this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
                ,Manifest.permission.RECORD_AUDIO))
            .subscribe {
            ArmsUtils.startActivity(this,MixRecordActivity::class.java)
        }

        btn_demo3.clicks()
            .compose(RxPermissions(this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
                ,Manifest.permission.RECORD_AUDIO))
            .subscribe {
                ArmsUtils.startActivity(this,MyMixRecordActivity::class.java)
            }
    }
}
