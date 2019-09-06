package me.shetj.mp3recorder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import me.shetj.recorder.util.LameUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.i("MainActivity",LameUtils.version())
    }
}
