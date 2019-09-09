package me.shetj.mp3recorder.record.utils

/**
 * @author shetj
 */
interface LifecycleListener {

    /**
     * Callback for when [android.app.Fragment.onStart]} or [ ][android.app.Activity.onStart] is called.
     */
    fun onStart()

    /**
     * Callback for when [android.app.Fragment.onStop]} or [ ][android.app.Activity.onStop]} is called.
     */
    fun onStop()

    /**
     * Callback for when [android.app.Fragment.onResume]} or [ ][android.app.Activity.onResume] is called.
     */
    fun onResume()

    /**
     * Callback for when [android.app.Fragment.onPause]} or [ ][android.app.Activity.onPause]} is called.
     */
    fun onPause()

    /**
     * Callback for when [android.app.Fragment.onDestroy]} or [ ][android.app.Activity.onDestroy] is called.
     */
    fun onDestroy()
}