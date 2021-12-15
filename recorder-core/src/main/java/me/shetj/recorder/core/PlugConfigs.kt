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
@file:Suppress("DEPRECATION")

package me.shetj.recorder.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 用来判断是否连接上了耳机
 */
class PlugConfigs(private val context: WeakReference<Context>, var connected: Boolean = false) {

    private var force = false // 强制返回true
    private val isRegister = AtomicBoolean(false)
    private var audioManager: AudioManager? = null

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == Intent.ACTION_HEADSET_PLUG) {
                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        connected = false
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        connected = true
                    }
                }
            }
        }
    }
    private val intentFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)

    fun registerReceiver() {
        if (isRegister.compareAndSet(false, true)) {
            if (audioManager == null) {
                audioManager =
                    context.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            }
            connected = audioManager?.isWiredHeadsetOn ?: false
            context.get()?.registerReceiver(mReceiver, intentFilter)
        }
    }

    fun unregisterReceiver() {
        if (isRegister.compareAndSet(true, false)) {
            context.get()?.unregisterReceiver(mReceiver)
            audioManager = null
        }
    }

    fun setForce(force: Boolean) {
        this.force = force
    }

    fun needBGBytes(): Boolean {
        return force or connected
    }

    companion object {
        @Volatile
        private var sInstance: PlugConfigs? = null

        fun getInstance(context: Context): PlugConfigs {
            return sInstance ?: synchronized(PlugConfigs::class.java) {
                return PlugConfigs(WeakReference(context.applicationContext)).also {
                    it.connected = it.audioManager?.isWiredHeadsetOn ?: false
                    sInstance = it
                }
            }
        }

        fun onDestroy() {
            sInstance?.unregisterReceiver()
            sInstance = null
        }
    }
}
