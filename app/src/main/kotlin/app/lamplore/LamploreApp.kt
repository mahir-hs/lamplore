package app.lamplore

import android.app.Application
import app.lamplore.audio.PlayerHub

class LamploreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        app.lamplore.data.Crash.install(this)
        App.init(this)
        PlayerHub.init(this)
    }
}
