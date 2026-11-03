package app.murmur

import android.app.Application
import app.murmur.audio.PlayerHub

class MurmurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App.init(this)
        PlayerHub.init(this)
    }
}
