package me.shetj.recorder.ui

import android.app.Activity
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

internal
fun formatSeconds(seconds: Long,max:Long): String {
    var secondsInternal = seconds
    if (secondsInternal > max) {
        secondsInternal = max
    }
    return (getTwoDecimalsValue(secondsInternal / 60) + ":"
            + getTwoDecimalsValue(secondsInternal % 60))
}


private fun getTwoDecimalsValue(value: Long): String {
    return if (value in 0..9) {
        "0$value"
    } else {
        value.toString() + ""
    }
}

fun AppCompatActivity.hasPermission(vararg permissions: String, isRequest: Boolean = false,): Boolean {
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (isRequest) {
                ActivityCompat.requestPermissions(this as Activity, permissions, 100)
            }
            return false
        }
    }
    return true
}
