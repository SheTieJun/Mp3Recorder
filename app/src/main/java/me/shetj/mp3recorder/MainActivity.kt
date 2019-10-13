package me.shetj.mp3recorder

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.record.MixRecordActivity
import me.shetj.mp3recorder.record.activity.MyRecordActivity
import me.shetj.recorder.util.LameUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("MainActivity",LameUtils.version())


        RxPermissions(this).request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
        ,Manifest.permission.RECORD_AUDIO).subscribe()

        btn_demo.setOnClickListener {
           ArmsUtils.startActivity(this,MyRecordActivity::class.java)
        }
        btn_demo2.setOnClickListener {
            ArmsUtils.startActivity(this,MixRecordActivity::class.java)
        }
    }
}
