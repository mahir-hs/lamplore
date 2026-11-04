package app.murmur.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.murmur.data.Downloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps downloads going after you leave the app. It only exists while something is downloading and
 * shows one quiet progress notification.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val first = notification()
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(ID, first)
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                if (Downloads.progress.isEmpty()) { stopSelf(); break }
                nm.notify(ID, notification())
                delay(1000)
            }
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val n = Downloads.progress.size
        val avg = if (n == 0) 0f else Downloads.progress.values.sum() / n
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return b.setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Murmur")
            .setContentText(Downloads.summary().ifBlank { if (n <= 1) "Downloading for offline listening" else "Downloading $n items" })
            .setProgress(100, (avg * 100).toInt(), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun CoroutineScope.cancel() = (coroutineContext[Job])?.cancel()

    companion object {
        private const val CHANNEL = "downloads"
        private const val ID = 4107

        /** Starts the service while there is something to download; it stops itself when the queue is empty. */
        fun sync(ctx: Context) {
            if (Downloads.progress.isNotEmpty()) {
                runCatching { ctx.startForegroundService(Intent(ctx, DownloadService::class.java)) }
            }
        }
    }
}
