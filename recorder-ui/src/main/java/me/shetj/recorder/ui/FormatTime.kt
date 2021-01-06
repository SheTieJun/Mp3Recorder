package me.shetj.recorder.ui

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