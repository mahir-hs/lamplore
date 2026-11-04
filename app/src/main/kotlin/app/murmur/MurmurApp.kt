package app.murmur

import android.app.Application
import app.murmur.audio.PlayerHub

class MurmurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app.murmur.data.Crash.install(this)
        App.init(this)
        PlayerHub.init(this)
    }
}
