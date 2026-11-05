package app.lamplore.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Keeps the sound mixer playing with the screen off. Exists only while the mixer plays. */
class MixerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Mixer.pause()
            stopSelf()
            return START_NOT_STICKY
        }
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "Sound mixer", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, MixerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        val n = b.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Lamplore sounds")
            .setContentText("Mixing ${Mixer.levels.values.count { it > 0f }} sounds")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(0, "Stop", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "mixer"
        private const val ID = 4108
        private const val ACTION_STOP = "app.lamplore.MIXER_STOP"

        fun start(ctx: Context) {
            runCatching { ctx.startForegroundService(Intent(ctx, MixerService::class.java)) }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, MixerService::class.java))
        }
    }
}
