package app.lamplore.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.lamplore.App
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * A stand-alone sound mixer: any number of the bundled field recordings and music beds playing together,
 * each with its own volume, with a sleep timer that fades everything out. Layers are created lazily
 * (a player exists only while its slider is above zero). A foreground service ([MixerService]) keeps it
 * playing with the screen off.
 */
object Mixer {
    /** One slider: a bundled loop. */
    data class Channel(val file: String, val label: String, val music: Boolean)

    val channels: List<Channel> =
        AmbienceKind.entries.filter { it.file != null }.map { Channel(it.file!!, it.label, false) } +
            MusicMood.entries.filter { it.file != null }.map { Channel("${it.file}", "${it.label} music", true) }

    /** 0..1 per file. Missing = 0. */
    val levels = mutableStateMapOf<String, Float>()
    var playing by mutableStateOf(false); private set
    /** Seconds left on the sleep timer, or -1 when no timer is running. */
    var timerLeft by mutableStateOf(-1); private set
    var timerMinutes by mutableStateOf(0)
    val presets = mutableStateListOf<Preset>()

    data class Preset(val name: String, val levels: Map<String, Float>)

    private val players = HashMap<String, ExoPlayer>()
    private var master = 1f
    private var timerJob: Job? = null
    private var fadeJob: Job? = null
    private var focus: AudioFocusRequest? = null
    private val prefs get() = App.context.getSharedPreferences("lamplore_mixer", Context.MODE_PRIVATE)

    fun init() {
        runCatching {
            val a = JSONArray(prefs.getString("presets", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val l = o.getJSONObject("l")
                presets.add(Preset(o.getString("n"), l.keys().asSequence().associateWith { l.getDouble(it).toFloat() }))
            }
        }
        runCatching {
            val l = JSONObject(prefs.getString("last", "{}") ?: "{}")
            l.keys().forEach { levels[it] = l.getDouble(it).toFloat() }
        }
        timerMinutes = prefs.getInt("timer", 0)
    }

    /** Sets a slider. Moving any slider up from silence starts the mixer. */
    fun setLevel(file: String, v: Float) {
        levels[file] = v.coerceIn(0f, 1f)
        persistLast()
        if (playing) apply() else if (v > 0f) play()
    }

    fun play() {
        if (levels.values.none { it > 0f }) return
        if (!takeFocus()) return
        master = 1f
        playing = true
        MixerService.start(App.context)
        apply()
        startTimer()
    }

    fun pause() {
        playing = false
        timerJob?.cancel()
        fadeJob?.cancel()
        timerLeft = -1
        players.values.forEach { it.pause() }
        giveFocus()
        MixerService.stop(App.context)
    }

    fun toggle() { if (playing) pause() else play() }

    /** Stops and silences every channel. */
    fun clear() {
        pause()
        levels.clear()
        persistLast()
        releaseAll()
    }

    fun setTimer(minutes: Int) {
        timerMinutes = minutes
        prefs.edit().putInt("timer", minutes).apply()
        if (playing) startTimer()
    }

    fun savePreset(name: String) {
        val n = name.trim().ifBlank { return }
        presets.removeAll { it.name == n }
        presets.add(0, Preset(n, levels.filterValues { it > 0f }.toMap()))
        persistPresets()
    }

    fun deletePreset(p: Preset) {
        presets.remove(p)
        persistPresets()
    }

    fun load(p: Preset) {
        levels.clear()
        levels.putAll(p.levels)
        persistLast()
        if (playing) apply() else play()
    }

    // ---- internals -----------------------------------------------------------------------------

    private fun apply() {
        for (c in channels) {
            val v = (levels[c.file] ?: 0f) * master
            val p = players[c.file]
            if (v <= 0f) {
                p?.pause()
                continue
            }
            val player = p ?: create(c.file)
            // a gentler curve than linear, so the low end of each slider is usable
            player.volume = v * v
            if (!player.isPlaying) player.play()
        }
    }

    private fun create(file: String): ExoPlayer {
        val p = ExoPlayer.Builder(App.context)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
            .build()
        p.repeatMode = Player.REPEAT_MODE_ONE
        p.setMediaItem(MediaItem.fromUri("asset:///sounds/$file.opus"))
        p.prepare()
        players[file] = p
        return p
    }

    private fun startTimer() {
        timerJob?.cancel()
        fadeJob?.cancel()
        master = 1f
        if (timerMinutes <= 0) { timerLeft = -1; return }
        timerLeft = timerMinutes * 60
        timerJob = App.scope.launch {
            while (timerLeft > 0) {
                delay(1000)
                timerLeft--
                // the last 30 seconds fade out
                if (timerLeft in 0..30) { master = timerLeft / 30f; apply() }
            }
            pause()
            master = 1f
        }
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> App.scope.launch { if (playing) pause() }
        }
    }

    private fun takeFocus(): Boolean {
        val am = App.context.getSystemService(AudioManager::class.java)
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focus = req
        return am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun giveFocus() {
        focus?.let { App.context.getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }

    private fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
    }

    private fun persistLast() {
        val o = JSONObject()
        levels.filterValues { it > 0f }.forEach { (k, v) -> o.put(k, v.toDouble()) }
        prefs.edit().putString("last", o.toString()).apply()
    }

    private fun persistPresets() {
        val a = JSONArray()
        presets.forEach { p -> a.put(JSONObject().put("n", p.name).put("l", JSONObject().also { o -> p.levels.forEach { (k, v) -> o.put(k, v.toDouble()) } })) }
        prefs.edit().putString("presets", a.toString()).apply()
    }
}
