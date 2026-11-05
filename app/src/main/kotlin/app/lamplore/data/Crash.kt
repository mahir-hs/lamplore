package app.lamplore.data

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Keeps the last crash in a small file so Diagnostics can show it, without sending anything anywhere. */
object Crash {
    private fun file(ctx: Context) = File(ctx.filesDir, "last-crash.txt")

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                file(app).writeText(
                    "Lamplore crash\n" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()) +
                        "\nThread: ${t.name}\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n" +
                        e.stackTraceToString().take(8000),
                )
            }
            previous?.uncaughtException(t, e)
        }
    }

    fun last(ctx: Context): String? = file(ctx).takeIf { it.exists() }?.readText()
    fun clear(ctx: Context) { file(ctx).delete() }
}
