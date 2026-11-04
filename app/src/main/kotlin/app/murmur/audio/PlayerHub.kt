package app.murmur.audio

import android.content.ComponentName
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.murmur.App
import app.murmur.data.Activity
import app.murmur.data.Downloads
import app.murmur.data.History
import app.murmur.data.Listen
import app.murmur.data.Story
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Single owner of playback. Asks the Director for a plan (who speaks, pace, background sound per
 * segment), generates each segment with its voice (cached on disk), streams them into ExoPlayer
 * as they become ready, and applies the plan's speed and sound as segments change.
 */
object PlayerHub {
    private lateinit var app: Context
    lateinit var engine: VoiceEngine
        private set
    lateinit var sherpa: SherpaEngine
        private set
    private lateinit var bed: SoundBed

    private var exo: ExoPlayer? = null
    val player: ExoPlayer get() = exo ?: build()

    var current by mutableStateOf<Story?>(null)
    var playing by mutableStateOf(false)
    var status by mutableStateOf("")
    var part by mutableIntStateOf(0)
    var parts by mutableIntStateOf(0)
    var fraction by mutableFloatStateOf(0f)
    var generating by mutableStateOf(false)
    var finished by mutableStateOf(false)
    var sleepLeftMs by mutableLongStateOf(0L)

    // Director state for the UI
    var plan by mutableStateOf<Plan?>(null)
    var nowRole by mutableStateOf(Role.NARRATOR)
    var nowSpeed by mutableFloatStateOf(1f)
    var nowAmbience by mutableStateOf(AmbienceKind.NONE)
    var nowMusic by mutableStateOf(MusicMood.NONE)
    var roleVoices by mutableStateOf<Map<Role, VoiceInfo>>(emptyMap())

    var previewStatus by mutableStateOf("")

    /** The sentence being read right now (for subtitles); empty for recorded audio. */
    var caption by mutableStateOf("")
        private set
    private val sentenceCache = HashMap<Int, List<String>>()

    /** Sentences, with long ones cut at commas so a subtitle stays short enough to read at a glance. */
    private fun splitSentences(t: String): List<String> =
        t.trim().split(Regex("(?<=[.!?\u2026][\"\u201d\u2019']?)\\s+")).map { it.trim() }.filter { it.isNotEmpty() }.flatMap { s ->
            if (s.length <= 120) listOf(s) else {
                val out = mutableListOf<String>()
                var cur = StringBuilder()
                for (piece in s.split(Regex("(?<=[,;:])\\s+"))) {
                    if (cur.isNotEmpty() && cur.length + piece.length > 110) { out += cur.toString(); cur = StringBuilder() }
                    if (cur.isNotEmpty()) cur.append(' ')
                    cur.append(piece)
                }
                if (cur.isNotEmpty()) out += cur.toString()
                out
            }
        }

    private fun updateCaption(p: ExoPlayer) {
        val k = base + p.currentMediaItemIndex
        val seg = segments.getOrNull(k) ?: return
        val sents = sentenceCache.getOrPut(k) { splitSentences(seg.text) }
        val dur = p.duration
        if (sents.isEmpty() || dur <= 0) return
        // the audio ends with a pause; speech fills the rest, spread over the sentences by length
        val speechMs = (dur - seg.pause * 1000.0).coerceAtLeast(dur * 0.7)
        val total = sents.sumOf { it.length }.coerceAtLeast(1)
        val at = (p.currentPosition / speechMs).coerceIn(0.0, 0.999) * total
        var acc = 0
        var chosen = sents.last()
        for (s in sents) { acc += s.length; if (at < acc) { chosen = s; break } }
        if (chosen != caption) caption = chosen
    }

    /** True while a recorded stream (audiobook or podcast) is playing instead of Murmur's own voice. */
    var streaming by mutableStateOf(false)
        private set

    private var segments: List<Segment> = emptyList()
    private var voiceFor: Map<Role, String> = emptyMap()
    private var base = 0
    private var genJob: Job? = null
    private var ticker: Job? = null
    private var sleepEnd = 0L
    private var serviceStarted = false
    private var forceStart = false
    private var previewJob: Job? = null
    private var previewPlayer: MediaPlayer? = null

    fun init(context: Context) {
        app = context.applicationContext
        PackManager.init(app)
        sherpa = SherpaEngine()
        engine = CompositeEngine(TtsGenerator(app), sherpa)
        bed = SoundBed(app)
    }

    /** Reads a short sentence in a voice (the selected one by default) so you can hear it. */
    fun preview(voiceName: String = App.voiceName) {
        previewJob?.cancel()
        previewPlayer?.release()
        previewPlayer = null
        exo?.pause()
        previewJob = App.scope.launch {
            val voice = engine.resolveVoice(voiceName)
            val f = File(app.filesDir, "preview/v2_${safe(voice)}.wav")
            if (!f.exists() || f.length() < 1024) {
                previewStatus = "Preparing voice..."
                val ok = engine.synth(
                    "Hello. This is how I will read your stories, slowly and calmly.",
                    voice,
                    f,
                    0.9f,
                    0f,
                )
                if (!ok) {
                    previewStatus = "Could not prepare this voice."
                    return@launch
                }
            }
            previewStatus = "Playing..."
            previewPlayer = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnCompletionListener {
                    it.release()
                    previewPlayer = null
                    previewStatus = ""
                }
                prepare()
                start()
            }
        }
    }

    private fun build(): ExoPlayer {
        val http = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Murmur/0.4")
        val p = ExoPlayer.Builder(app)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(androidx.media3.datasource.DefaultDataSource.Factory(app, http)),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo = p
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                updateAmbience()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                status = if (streaming) "Could not load this audio. Check your connection and try again." else status
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                applyParams()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && generating) status = "Preparing next part..."
                if (state == Player.STATE_ENDED && !generating) {
                    current?.let { App.markCompleted(it.id) }
                    finished = true
                    status = "Finished"
                    updateAmbience()
                }
            }
        })
        applyParams()
        startTicker()
        return p
    }

    private fun ensureService() {
        if (serviceStarted) return
        serviceStarted = true
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        MediaController.Builder(app, token).buildAsync()
    }

    /** Estimated seconds per segment (actual once generated). Used for whole-story progress and seeking. */
    private var estSec: DoubleArray = DoubleArray(0)
    val totalSeconds: Double get() = estSec.sum()
    var positionSeconds by mutableFloatStateOf(0f)

    /** Jump anywhere in the story. Parts not generated yet can't be reached; we go to the last ready part. */
    fun seekToFraction(f: Float) {
        val p = exo ?: return
        if ((segments.isEmpty() && !streaming) || p.mediaItemCount == 0) return
        var target = (f.coerceIn(0f, 1f) * totalSeconds)
        var k = 0
        val n = if (streaming) estSec.size else segments.size
        while (k < n - 1 && target > estSec[k]) { target -= estSec[k]; k++ }
        val idx = (k - base).coerceIn(0, p.mediaItemCount - 1)
        val offset = if (k - base == idx) (target * 1000).toLong() else 0L
        p.seekTo(idx, offset)
    }

    private fun currentSegment(): Segment? {
        val p = exo ?: return null
        if (p.mediaItemCount == 0) return null
        return segments.getOrNull(base + p.currentMediaItemIndex)
    }

    /** Apply speed and background sound for the segment now playing (Director or manual choice). */
    fun applyParams() {
        val seg = currentSegment()
        nowRole = seg?.role ?: Role.NARRATOR
        if (streaming) {
            nowSpeed = streamSpeed
            nowAmbience = AmbienceKind.NONE
            nowMusic = MusicMood.NONE
            exo?.playbackParameters = PlaybackParameters(streamSpeed, 1f)
            updateAmbience()
            return
        }
        nowSpeed = if (App.autoSpeed && seg != null) seg.speed else App.speed
        nowAmbience = if (App.autoSound && seg != null) seg.ambience else App.ambience
        nowMusic = when {
            !App.autoMusic -> App.music
            seg == null -> plan?.music ?: MusicMood.NONE
            seg.ambience == AmbienceKind.HEARTBEAT -> MusicMood.TENSION
            else -> plan?.music ?: MusicMood.CALM
        }
        // Piper/Kokoro bake the pace into the speech; only the system voice is time-stretched by the player.
        val stretch = if (seg != null && baked(voiceFor[seg.role] ?: "")) 1f else nowSpeed
        exo?.playbackParameters = PlaybackParameters(stretch, (App.pitch * (seg?.pitch ?: 1f)).coerceIn(0.5f, 2f))
        updateAmbience()
    }

    /** Playback speed for recorded audio (podcasts are often listened to faster). */
    var streamSpeed by mutableFloatStateOf(1f)

    fun changeStreamSpeed(v: Float) {
        streamSpeed = v
        applyParams()
    }

    private fun playStream(story: Story, fromStart: Boolean) {
        val p = player
        stopInternal()
        ensureService()
        streaming = true
        current = story
        finished = false
        fraction = 0f
        plan = null
        segments = emptyList()
        parts = story.tracks.size
        estSec = DoubleArray(story.tracks.size) { story.tracks[it].seconds.toDouble() }
        positionSeconds = 0f
        if (fromStart) App.clearProgress(story.id)
        val saved = if (fromStart) null else App.savedProgress(story.id)
        base = 0
        val start = (saved?.first ?: 0).coerceIn(0, story.tracks.size - 1)
        part = start
        p.clearMediaItems()
        story.tracks.forEachIndexed { i, t ->
            p.addMediaItem(
                MediaItem.Builder()
                    .setMediaId("${story.id}#$i")
                    .setUri(Downloads.local(story.id, i, t.url)?.let { Uri.fromFile(it) } ?: Uri.parse(t.url))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(if (story.tracks.size > 1) "${story.title}: ${t.title}" else story.title).setArtist(story.author).setAlbumTitle(story.category).build())
                    .build(),
            )
        }
        applyParams()
        p.prepare()
        p.seekTo(start, saved?.second ?: 0L)
        p.playWhenReady = true
        status = "Loading..."
        Listen.remember(story)
    }

    /** Start a recorded book at a chosen chapter. */
    fun playFrom(story: Story, index: Int) {
        val p = player
        if (current?.id == story.id && streaming && p.mediaItemCount > index) {
            p.seekTo(index, 0)
            p.play()
        } else {
            App.saveProgress(story.id, index, 0)
            play(story)
        }
    }

    fun updateAmbience() {
        if (!::bed.isInitialized) return
        bed.update(nowAmbience, nowMusic, App.ambienceVolume, App.musicVolume, exo?.isPlaying == true)
    }

    private fun baked(voice: String) = voice.startsWith("sherpa:")

    private fun safe(s: String) = s.replace(Regex("[^A-Za-z0-9_-]"), "_")

    /** Narrator = the chosen voice. Characters = other voices, same voice pack first, matched by gender. */
    private suspend fun assignVoices(): Map<Role, String> {
        val narrator = engine.resolveVoice(App.voiceName)
        val all = engine.voices()
        fun info(n: String) = all.firstOrNull { it.name == n } ?: VoiceInfo(n, "Default voice", true)
        if (!App.autoVoice) {
            roleVoices = mapOf(Role.NARRATOR to info(narrator))
            return Role.entries.associateWith { narrator }
        }
        val nar = all.firstOrNull { it.name == narrator }
        val ordered = all.sortedBy { v ->
            when {
                v.family == (nar?.family ?: "system") -> 0
                v.family != "system" -> 1
                else -> 2
            }
        }
        fun pick(g: Gender, vararg taken: String): String =
            ordered.firstOrNull { it.gender == g && it.name != narrator && it.name !in taken && it.offline }?.name
                ?: ordered.firstOrNull { it.gender == g && it.name != narrator && it.name !in taken }?.name
                ?: narrator
        val male = pick(Gender.MALE)
        val female = pick(Gender.FEMALE)
        // Young and big characters get their own voice when there is one (a second woman, a second man),
        // otherwise they share the usual one and the pitch shift alone sets them apart.
        val child = pick(Gender.FEMALE, female).let { if (it == narrator) female else it }
        val deep = pick(Gender.MALE, male).let { if (it == narrator) male else it }
        val map = mapOf(Role.NARRATOR to narrator, Role.MALE to male, Role.FEMALE to female, Role.CHILD to child, Role.DEEP to deep)
        roleVoices = map.mapValues { (_, n) -> info(n) }
        return map
    }

    fun play(story: Story, fromStart: Boolean = false) {
        val p = player
        if (current?.id == story.id && p.mediaItemCount > 0 && !fromStart && !finished) {
            p.play()
            return
        }
        if (story.isStream) { playStream(story, fromStart); return }
        streaming = false
        stopInternal()
        ensureService()
        sentenceCache.clear()
        caption = ""
        current = story
        finished = false
        fraction = 0f
        status = "Reading the story..."

        val pl = Director.plan(story)
        plan = pl
        segments = pl.segments
        parts = segments.size
        estSec = DoubleArray(segments.size) { segments[it].text.length / 17.0 }
        positionSeconds = 0f
        val saved = if (fromStart) null else App.savedProgress(story.id)
        base = (saved?.first ?: 0).coerceIn(0, segments.size - 1)
        part = base
        if (fromStart) App.clearProgress(story.id)
        forceStart = false

        genJob = App.scope.launch {
            generating = true
            voiceFor = assignVoices()
            val narrator = voiceFor[Role.NARRATOR] ?: ""
            status = "Loading voice..."
            engine.warmUp(narrator)
            val dir = File(app.filesDir, "audio/${story.id}")

            // Adaptive buffering: measure how fast voices generate on this phone, then start only when
            // every remaining segment is predicted to be ready before playback reaches it.
            var genAudioSec = 0.0
            var genWallSec = 0.0
            var bufferedSec = 0.0
            var started = false

            fun canStart(done: Int): Boolean {
                if (genWallSec <= 0.0) return true
                val rate = genAudioSec / genWallSec
                var finish = 0.0
                var needAt = bufferedSec
                for (j in done + 1 until segments.size) {
                    val d = segments[j].text.length / 17.0
                    finish += d / rate
                    if (finish > needAt + 1.0) return false
                    needAt += d
                }
                return true
            }

            for (i in base until segments.size) {
                val seg = segments[i]
                val voice = voiceFor[seg.role] ?: narrator
                // pace and pause are part of the audio, so they are part of the cache name
                val spd = if (App.autoSpeed) seg.speed else App.speed
                val bakedSpeed = if (baked(voice)) spd else 1f
                val f = File(dir, "seg_${i}_${safe(voice)}_s${(bakedSpeed * 100).toInt()}_p${(seg.pause * 100).toInt()}.wav")
                if (!f.exists() || f.length() < 1024) {
                    // While playing, say nothing: the player listener shows "Preparing next part..." only if
                    // playback actually catches up with generation.
                    if (!started) status = "Preparing voice (${i + 1}/${segments.size})..."
                    f.delete()
                    val t0 = System.nanoTime()
                    val ok = engine.synth(seg.text, voice, f, bakedSpeed, seg.pause)
                    if (!ok) {
                        status = "Voice engine error. Check Settings > Text-to-speech in Android."
                        generating = false
                        return@launch
                    }
                    val secs = wavSeconds(f)
                    genWallSec += (System.nanoTime() - t0) / 1e9
                    genAudioSec += secs
                    bufferedSec += secs
                    if (secs > 0) estSec[i] = secs
                } else {
                    val secs = wavSeconds(f)
                    bufferedSec += secs
                    if (secs > 0) estSec[i] = secs
                }
                val item = MediaItem.Builder()
                    .setMediaId("${story.id}#$i")
                    .setUri(Uri.fromFile(f))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(story.title)
                            .setArtist(story.author)
                            .setAlbumTitle(story.category)
                            .build(),
                    )
                    .build()
                p.addMediaItem(item)
                val idx = i - base
                if (idx == 0) {
                    p.playWhenReady = false
                    p.prepare()
                    if (saved != null && saved.second > 0) p.seekTo(0, saved.second)
                    applyParams()
                }
                if (!started) {
                    val allDone = i == segments.size - 1
                    if (forceStart || allDone || canStart(i)) {
                        started = true
                        if (p.playbackState == Player.STATE_ENDED) p.seekTo(0, saved?.second ?: 0L)
                        p.playWhenReady = true
                        status = ""
                    } else {
                        val pct = (((i - base + 1) * 100) / (segments.size - base)).coerceIn(0, 99)
                        status = "Preparing for smooth listening... $pct%"
                    }
                } else if (p.playbackState == Player.STATE_ENDED) {
                    p.seekTo(idx, 0)
                    p.play()
                    status = ""
                } else if (status.startsWith("Preparing")) {
                    status = ""
                }
            }
            generating = false
            if (p.playbackState == Player.STATE_ENDED) {
                current?.let { App.markCompleted(it.id) }
                finished = true
                status = "Finished"
            }
        }
    }

    /** Start playing now even if the voice is still preparing (playback may pause while it catches up). */
    fun startNow() {
        forceStart = true
        val p = exo ?: return
        if (p.mediaItemCount > 0) {
            p.playWhenReady = true
            if (status.startsWith("Preparing for smooth")) status = ""
        }
    }

    private fun wavSeconds(f: File): Double {
        if (f.length() <= 44) return 0.0
        return runCatching {
            f.inputStream().use { s ->
                val h = ByteArray(28)
                if (s.read(h) < 28) return 0.0
                val rate = (h[24].toInt() and 0xFF) or ((h[25].toInt() and 0xFF) shl 8) or
                    ((h[26].toInt() and 0xFF) shl 16) or ((h[27].toInt() and 0xFF) shl 24)
                if (rate <= 0) 0.0 else (f.length() - 44) / (rate * 2.0)
            }
        }.getOrDefault(0.0)
    }

    fun togglePlay() {
        val p = player
        if (p.isPlaying) p.pause() else {
            if (p.playbackState == Player.STATE_ENDED && !generating) current?.let { play(it, fromStart = true) }
            else p.play()
        }
    }

    fun seekBy(ms: Long) {
        val p = player
        val target = p.currentPosition + ms
        if (ms > 0 && p.duration > 0 && target > p.duration && p.hasNextMediaItem()) p.seekToNextMediaItem()
        else p.seekTo(target.coerceAtLeast(0))
    }

    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun previous() {
        val p = player
        if (p.currentPosition > 3000 || !p.hasPreviousMediaItem()) p.seekTo(0) else p.seekToPreviousMediaItem()
    }

    fun restart() { current?.let { play(it, fromStart = true) } }

    fun setSleep(minutes: Int) {
        if (minutes <= 0) {
            sleepEnd = 0L
            sleepLeftMs = 0L
        } else {
            sleepEnd = SystemClock.elapsedRealtime() + minutes * 60_000L
            sleepLeftMs = minutes * 60_000L
        }
    }

    fun saveProgress() {
        val s = current ?: return
        val p = exo ?: return
        if (p.mediaItemCount == 0 || finished) return
        App.saveProgress(s.id, base + p.currentMediaItemIndex, p.currentPosition, fraction)
    }

    private fun stopInternal() {
        saveProgress()
        genJob?.cancel()
        genJob = null
        engine.stop()
        exo?.let {
            it.playWhenReady = false // stop() keeps this flag; a new story must not auto-play its first part
            it.stop()
            it.clearMediaItems()
        }
        generating = false
        playing = false
        updateAmbience()
    }

    fun stopAll() {
        stopInternal()
        current = null
        streaming = false
        plan = null
        segments = emptyList()
        status = ""
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = App.scope.launch {
            var acc = 0
            var lastIdx = -1
            while (isActive) {
                delay(250)
                val p = exo ?: continue
                if (p.mediaItemCount > 0) {
                    val idx = p.currentMediaItemIndex
                    if (idx != lastIdx) {
                        lastIdx = idx
                        applyParams()
                    }
                    part = base + idx
                    if (!streaming && App.subtitles) updateCaption(p) else if (caption.isNotEmpty()) caption = ""
                    if (streaming) {
                        if (p.duration > 0 && idx < estSec.size) estSec[idx] = p.duration / 1000.0
                        if (p.isPlaying && status == "Loading...") status = ""
                    }
                    val before = (0 until part.coerceAtMost(estSec.size)).sumOf { estSec[it] }
                    val pos = before + p.currentPosition / 1000.0
                    positionSeconds = pos.toFloat()
                    val total = totalSeconds
                    fraction = if (total > 0) (pos / total).toFloat().coerceIn(0f, 1f) else 0f
                }
                if (p.isPlaying) {
                    acc += 250
                    if (acc >= 10_000) {
                        acc = 0
                        App.addListening(10)
                        current?.let { Activity.record(it, 10); History.record(it, 10) }
                        saveProgress()
                    }
                }
                if (sleepEnd > 0L) {
                    val left = sleepEnd - SystemClock.elapsedRealtime()
                    if (left <= 0L) {
                        sleepEnd = 0L
                        sleepLeftMs = 0L
                        p.pause()
                        saveProgress()
                    } else sleepLeftMs = left
                }
            }
        }
    }
}
