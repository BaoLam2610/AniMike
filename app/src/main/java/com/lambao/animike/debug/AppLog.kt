package com.lambao.animike.debug

import android.util.Log
import com.lambao.animike.BuildConfig

// MVP-Debug Đợt 1 — wrapper mỏng quanh android.util.Log: vừa log ra Logcat như
// cũ, vừa append vào DebugInspector để tab "Log local" của màn Debug đọc lại.
// Thay cho mọi lời gọi Log.* trực tiếp trong app. Ở release build, phần append
// buffer bị bỏ qua (BuildConfig.DEBUG = false) nên không tốn RAM/CPU — Logcat
// vẫn hoạt động bình thường.
object AppLog {

    fun v(tag: String, message: String, throwable: Throwable? = null) =
        log(AppLogLevel.VERBOSE, tag, message, throwable)

    fun d(tag: String, message: String, throwable: Throwable? = null) =
        log(AppLogLevel.DEBUG, tag, message, throwable)

    fun i(tag: String, message: String, throwable: Throwable? = null) =
        log(AppLogLevel.INFO, tag, message, throwable)

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        log(AppLogLevel.WARN, tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log(AppLogLevel.ERROR, tag, message, throwable)

    private fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            AppLogLevel.VERBOSE -> Log.v(tag, message, throwable)
            AppLogLevel.DEBUG -> Log.d(tag, message, throwable)
            AppLogLevel.INFO -> Log.i(tag, message, throwable)
            AppLogLevel.WARN -> Log.w(tag, message, throwable)
            AppLogLevel.ERROR -> Log.e(tag, message, throwable)
        }
        if (BuildConfig.DEBUG) {
            DebugInspector.addAppLog(
                AppLogEntry(
                    id = DebugInspector.nextId(),
                    timestampMs = System.currentTimeMillis(),
                    level = level,
                    tag = tag,
                    message = message,
                    stackTrace = throwable?.let { Log.getStackTraceString(it) },
                ),
            )
        }
    }
}
