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
package me.shetj.mp3recorder

import android.Manifest
import me.shetj.base.ktx.hasPermission
import me.shetj.base.ktx.showToast
import me.shetj.base.ktx.start
import me.shetj.base.mvvm.BaseBindingActivity
import me.shetj.base.mvvm.BaseViewModel
import me.shetj.mp3recorder.databinding.ActivityMainBinding
import me.shetj.mp3recorder.record.activity.mix.MyMixRecordActivity
import me.shetj.recorder.ui.RecorderPopup

class MainActivity : BaseBindingActivity<BaseViewModel, ActivityMainBinding>() {

    private val recorderPopup: RecorderPopup by lazy {
        RecorderPopup(this, needPlay = false, maxTime = (10 * 1000).toLong()) {
            it.showToast()
        }
    }

    override fun onActivityCreate() {
        super.onActivityCreate()
        hasPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            isRequest = true
        )
        mViewBinding.btnDemo3.setOnClickListener {
            if (hasPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO, isRequest = true
                )
            ) {
                start<MyMixRecordActivity>()
            }
        }

        mViewBinding.btnDemo4.setOnClickListener {
            recorderPopup.showPop()
        }
    }

    override fun onBackPressed() {
        if (recorderPopup.onBackPress()) {
            super.onBackPressed()
        }
    }
}
