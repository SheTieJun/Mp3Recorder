
package me.shetj.recorder.ui

import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.recorder(onSuccess: Success? = null): Lazy<RecorderPopup> {
    return lazy {
        RecorderPopup(this, onSuccess = onSuccess)
    }
}
