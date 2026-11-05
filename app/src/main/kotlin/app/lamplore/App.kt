package app.lamplore

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.lamplore.audio.AmbienceKind
import app.lamplore.audio.MusicMood
import app.lamplore.audio.PlayerHub
import app.lamplore.data.Categories
import app.lamplore.data.Reflection
import app.lamplore.data.SeedStories
import app.lamplore.data.Story
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** App-wide state + persistence (SharedPreferences, all private to the app). */
object App {
    private lateinit var prefs: SharedPreferences
    lateinit var context: Context
        private set
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val stories = mutableStateListOf<Story>()
    val favorites = mutableStateListOf<String>()
    val completed = mutableStateListOf<String>()
    val reported = mutableStateListOf<String>()
    val reflections = mutableStateListOf<Reflection>()
    val goalDays = mutableStateListOf<Long>()

    /** False until the first-run voice setup is finished or skipped. Existing installs count as done. */
    var onboarded by mutableStateOf(true)
        private set

    var voiceName by mutableStateOf("")
    var speed by mutableFloatStateOf(1f)
    var pitch by mutableFloatStateOf(1f)
    var ambience by mutableStateOf(AmbienceKind.NONE)
    var ambienceVolume by mutableFloatStateOf(0.35f)

    /** Story Director: when true, the app picks this itself per scene; false = the manual setting above applies. */
    var autoVoice by mutableStateOf(true)
    var autoSpeed by mutableStateOf(true)
    var autoSound by mutableStateOf(true)
    var autoMusic by mutableStateOf(true)
    var music by mutableStateOf(MusicMood.CALM)
    var musicVolume by mutableFloatStateOf(0.22f)

    /** Incognito: nothing is added to history, activity, "seen" marks or Continue lists. */
    var incognito by mutableStateOf(false)

    /** Show the words being read under the picture. */
    var subtitles by mutableStateOf(true)
    var todaySeconds by mutableIntStateOf(0)
    var doneToday by mutableStateOf(false)
    var goalText by mutableStateOf("")

    private val listen = mutableMapOf<String, Int>()
    private val doneDates = mutableSetOf<String>()

    fun init(c: Context) {
        context = c.applicationContext
        prefs = context.getSharedPreferences("murmur", Context.MODE_PRIVATE)

        onboarded = prefs.getBoolean("onboarded", prefs.contains("speed"))
        voiceName = prefs.getString("voice", "") ?: ""
        speed = prefs.getFloat("speed", 0.9f)
        pitch = prefs.getFloat("pitch", 1f)
        ambience = runCatching { AmbienceKind.valueOf(prefs.getString("ambience", "NONE") ?: "NONE") }
            .getOrDefault(AmbienceKind.NONE)
        ambienceVolume = prefs.getFloat("ambVol", 0.35f)
        autoVoice = prefs.getBoolean("autoVoice", true)
        autoSpeed = prefs.getBoolean("autoSpeed", true)
        autoSound = prefs.getBoolean("autoSound", true)
        autoMusic = prefs.getBoolean("autoMusic", true)
        music = runCatching { MusicMood.valueOf(prefs.getString("music", "CALM") ?: "CALM") }.getOrDefault(MusicMood.CALM)
        musicVolume = prefs.getFloat("musicVol", 0.22f)
        subtitles = prefs.getBoolean("subtitles", true)
        incognito = prefs.getBoolean("incognito", false)

        favorites.addAll(prefs.getStringSet("fav", emptySet()) ?: emptySet())
        completed.addAll(prefs.getStringSet("completed", emptySet()) ?: emptySet())
        reported.addAll(prefs.getStringSet("reported", emptySet()) ?: emptySet())

        runCatching {
            val o = JSONObject(prefs.getString("listen", "{}") ?: "{}")
            o.keys().forEach { listen[it] = o.getInt(it) }
        }
        doneDates.addAll(prefs.getStringSet("done", emptySet()) ?: emptySet())
        todaySeconds = listen[today()] ?: 0
        doneToday = today() in doneDates

        runCatching {
            val a = JSONArray(prefs.getString("reflections", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                reflections.add(Reflection(o.getString("d"), o.getString("t"), o.optString("s").ifBlank { null }))
            }
        }

        stories.addAll(SeedStories.all)
        stories.addAll(loadUserStories())
        app.lamplore.data.Listen.init()
        app.lamplore.data.History.init()
        app.lamplore.data.Subs.init()
        app.lamplore.data.Lists.init()
        app.lamplore.data.Searches.init()
        app.lamplore.data.Bookmarks.init()
        app.lamplore.data.Lock.init()
        app.lamplore.ui.Looks.init(context)
        app.lamplore.data.Subs.schedule(context)
        app.lamplore.data.Backup.autoIfDue(context)
        app.lamplore.data.Downloads.init()
        app.lamplore.data.RemoteContent.refresh()
        app.lamplore.ai.AiModels.init(context)

        goalText = prefs.getString("goalText", "") ?: ""
        val week = prefs.getLong("goalWeek", 0L)
        if (week == mondayEpoch()) {
            (prefs.getStringSet("goalDays", emptySet()) ?: emptySet()).forEach { goalDays.add(it.toLong()) }
        } else {
            goalText = ""
            prefs.edit().putString("goalText", "").putStringSet("goalDays", emptySet()).putLong("goalWeek", mondayEpoch()).apply()
        }
    }

    // ---- dates -------------------------------------------------------------------------------
    fun today(): String = LocalDate.now().toString()
    fun mondayEpoch(): Long = LocalDate.now().let { it.minusDays((it.dayOfWeek.value - 1).toLong()).toEpochDay() }

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    // ---- settings ----------------------------------------------------------------------------
    fun saveSettings() {
        prefs.edit()
            .putString("voice", voiceName)
            .putFloat("speed", speed)
            .putFloat("pitch", pitch)
            .putString("ambience", ambience.name)
            .putFloat("ambVol", ambienceVolume)
            .putBoolean("autoVoice", autoVoice)
            .putBoolean("autoSpeed", autoSpeed)
            .putBoolean("autoSound", autoSound)
            .putBoolean("autoMusic", autoMusic)
            .putString("music", music.name)
            .putFloat("musicVol", musicVolume)
            .putBoolean("subtitles", subtitles)
            .putBoolean("incognito", incognito)
            .apply()
        PlayerHub.applyParams()
    }

    // ---- library state -------------------------------------------------------------------------
    val visibleStories: List<Story> get() = stories.filter { it.id !in reported }

    fun toggleFavorite(id: String) {
        if (id in favorites) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("fav", favorites.toSet()).apply()
    }

    fun markCompleted(id: String) {
        if (id !in completed) completed.add(id)
        prefs.edit().putStringSet("completed", completed.toSet()).remove("pos_$id").remove("frac_$id").apply()
    }

    fun unmarkCompleted(id: String) {
        completed.remove(id)
        prefs.edit().putStringSet("completed", completed.toSet()).apply()
    }

    fun report(id: String) {
        if (id !in reported) reported.add(id)
        prefs.edit().putStringSet("reported", reported.toSet()).apply()
    }

    fun saveProgress(id: String, index: Int, posMs: Long, fraction: Float = -1f) {
        val e = prefs.edit().putString("pos_$id", "$index:$posMs")
        if (fraction >= 0f) e.putFloat("frac_$id", fraction)
        e.apply()
    }

    /** How far through a story or book you are, 0..1 (0 when not started or finished). */
    fun progressFraction(id: String): Float = if (prefs.contains("pos_$id")) prefs.getFloat("frac_$id", 0f) else 0f

    fun savedProgress(id: String): Pair<Int, Long>? {
        val s = prefs.getString("pos_$id", null) ?: return null
        val p = s.split(":")
        return runCatching { p[0].toInt() to p[1].toLong() }.getOrNull()
    }

    fun clearProgress(id: String) {
        prefs.edit().remove("pos_$id").remove("frac_$id").apply()
    }

    fun hasProgress(id: String) = prefs.contains("pos_$id")

    // ---- user stories ------------------------------------------------------------------------
    private fun loadUserStories(): List<Story> = runCatching {
        val a = JSONArray(prefs.getString("user_stories", "[]") ?: "[]")
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Story(
                id = o.getString("id"),
                title = o.getString("title"),
                author = o.getString("author"),
                category = o.getString("category").let { if (it == "Faith & wisdom") Categories.WISDOM else it },
                moods = setOf("calm"),
                summary = o.getString("body").take(110).replace("\n", " ") + "...",
                body = o.getString("body"),
                rights = o.getString("rights"),
                isUser = true,
            )
        }
    }.getOrDefault(emptyList())

    private fun saveUserStories() {
        val a = JSONArray()
        stories.filter { it.isUser }.forEach {
            a.put(
                JSONObject()
                    .put("id", it.id).put("title", it.title).put("author", it.author)
                    .put("category", it.category).put("body", it.body).put("rights", it.rights),
            )
        }
        prefs.edit().putString("user_stories", a.toString()).apply()
    }

    fun addUserStory(title: String, author: String, category: String, body: String, rights: String): Story {
        val s = Story(
            id = "u_${System.currentTimeMillis()}",
            title = title.trim(),
            author = author.trim().ifBlank { "Me" },
            category = category,
            moods = setOf("calm"),
            summary = body.trim().take(110).replace("\n", " ") + "...",
            body = body.trim(),
            rights = rights,
            isUser = true,
        )
        stories.add(s)
        saveUserStories()
        return s
    }

    fun deleteUserStory(id: String) {
        stories.removeAll { it.id == id && it.isUser }
        saveUserStories()
        File(context.filesDir, "audio").listFiles()
            ?.filter { it.name == id || it.name.startsWith("${id}__") }
            ?.forEach { it.deleteRecursively() }
    }

    // ---- wellbeing -----------------------------------------------------------------------------
    fun addListening(seconds: Int) {
        if (incognito) return
        val t = today()
        listen[t] = (listen[t] ?: 0) + seconds
        todaySeconds = listen[t] ?: 0
        val o = JSONObject()
        listen.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString("listen", o.toString()).apply()
    }

    fun secondsOn(d: LocalDate): Int = listen[d.toString()] ?: 0
    fun allSeconds(): Int = listen.values.sum()
    fun firstListenDate(): LocalDate? = listen.keys.minOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun active(d: LocalDate): Boolean =
        (listen[d.toString()] ?: 0) >= 60 || d.toString() in doneDates

    fun streak(): Int {
        var d = LocalDate.now()
        if (!active(d)) d = d.minusDays(1)
        var n = 0
        while (active(d)) { n++; d = d.minusDays(1) }
        return n
    }

    fun markDone() {
        doneDates.add(today())
        doneToday = true
        prefs.edit().putStringSet("done", doneDates.toSet()).apply()
    }

    fun addReflection(text: String, storyTitle: String?) {
        reflections.add(0, Reflection(today(), text.trim(), storyTitle))
        val a = JSONArray()
        reflections.forEach { a.put(JSONObject().put("d", it.date).put("t", it.text).put("s", it.storyTitle ?: "")) }
        prefs.edit().putString("reflections", a.toString()).apply()
    }

    fun setGoal(text: String) {
        goalText = text.trim()
        prefs.edit().putString("goalText", goalText).putLong("goalWeek", mondayEpoch()).apply()
    }

    fun toggleGoalToday() {
        val e = LocalDate.now().toEpochDay()
        if (e in goalDays) goalDays.remove(e) else goalDays.add(e)
        prefs.edit().putStringSet("goalDays", goalDays.map { it.toString() }.toSet()).apply()
    }

    fun clearGoal() {
        goalText = ""
        goalDays.clear()
        prefs.edit().putString("goalText", "").putStringSet("goalDays", emptySet()).apply()
    }

    // ---- storage -------------------------------------------------------------------------------
    fun audioBytes(): Long =
        File(context.filesDir, "audio").walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearAudio() {
        PlayerHub.stopAll()
        File(context.filesDir, "audio").deleteRecursively()
    }
}
