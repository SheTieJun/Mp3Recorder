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
package me.shetj.recorder.core

/**
 * **@author：** shetj<br></br>
 * **@createTime：** 2019/6/21<br></br>
 * **@email：** 375105540@qq.com<br></br>
 * **@describe**  <br></br>
 */
open class SimRecordListener : RecordListener, PermissionListener {
    override fun needPermission() {
    }

    override fun onStart() {
    }

    override fun onResume() {
    }

    override fun onReset() {
    }

    override fun onRecording(time: Long, volume: Int) {
    }

    override fun onPause() {
    }

    override fun onRemind(duration: Long) {
    }

    override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
    }

    override fun onMaxChange(time: Long) {
    }

    override fun onError(e: Exception) {
    }

    override fun onMuteRecordChange(mute: Boolean) {
    }
}
