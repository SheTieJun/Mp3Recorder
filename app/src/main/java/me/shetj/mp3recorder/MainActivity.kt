package me.shetj.mp3recorder

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding4.view.clicks
import kotlinx.android.synthetic.main.activity_main.*
import me.shetj.base.ktx.hasPermission
import me.shetj.base.ktx.start
import me.shetj.mp3recorder.record.MixRecordActivity
import me.shetj.mp3recorder.record.activity.mix.MyMixRecordActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hasPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            isRequest = true
        )

        btn_demo.clicks()
            .map {
                hasPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO, isRequest = true
                )
            }
            .subscribe {
                start<MyMixRecordActivity>()
            }

        btn_demo2.clicks()
            .map {
                hasPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO, isRequest = true
                )
            }
            .subscribe {
                start<MixRecordActivity>()
            }

        btn_demo3.clicks()
            .map {
                hasPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO, isRequest = true
                )
            }
            .subscribe {
                start<MyMixRecordActivity>()
            }
    }
}
