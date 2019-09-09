package me.shetj.mp3recorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.mp3recorder.record.activity.MyRecordActivity
import me.shetj.recorder.util.LameUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("MainActivity",LameUtils.version())
        btn_demo.setOnClickListener {
           ArmsUtils.startActivity(this,MyRecordActivity::class.java)
        }
    }
}
