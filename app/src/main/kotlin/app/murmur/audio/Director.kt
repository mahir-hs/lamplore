package app.murmur.audio

import app.murmur.data.Categories
import app.murmur.data.Story
import kotlin.math.roundToInt

enum class Role { NARRATOR, MALE, FEMALE }

/** One stretch of the story read with a single voice, pace and background sound. */
data class Segment(
    val text: String,
    val role: Role,
    val speed: Float,
    val ambience: AmbienceKind,
)

data class Plan(val segments: List<Segment>) {
    val roles: List<Role> get() = segments.map { it.role }.distinct()
    val scenes: List<AmbienceKind> get() = segments.map { it.ambience }.distinct().filter { it != AmbienceKind.NONE }
    val minSpeed: Float get() = segments.minOf { it.speed }
    val maxSpeed: Float get() = segments.maxOf { it.speed }

    fun summary(): String {
        val v = when (roles.size) { 1 -> "1 voice"; else -> "${roles.size} voices" }
        val s = if (scenes.isEmpty()) "no background" else scenes.joinToString(", ") { it.label.lowercase() }
        val sp = if (minSpeed == maxSpeed) "${fmt(minSpeed)}x" else "${fmt(minSpeed)}-${fmt(maxSpeed)}x"
        return "$v · $s · $sp"
    }

    private fun fmt(f: Float) = "%.2f".format(f).trimEnd('0').trimEnd('.')
}

/**
 * Story Director. Reads the text and decides, segment by segment, who speaks (narrator or a
 * male/female character for dialogue), how fast, and which background sound fits the scene.
 * Built-in text analysis: instant, offline, no model download. Sits behind this object so a
 * neural model can replace it later.
 */
object Director {
    private val quoteRx = Regex("[\"“]([^\"“”]{2,})[\"”]")
    private val sentenceRx = Regex("(?<=[.!?…])\\s+")

    private fun words(vararg w: String) = w.toList()

    private val scenes: Map<AmbienceKind, Pair<List<String>, List<String>>> = mapOf(
        // kind to (strong words = 2 points, weak words = 1 point)
        AmbienceKind.RAIN to (words("rain", "raining", "rained", "drizzle", "downpour") to words("wet", "puddle", "puddles", "umbrella", "damp", "showers")),
        AmbienceKind.STORM to (words("storm", "storms", "thunder", "lightning", "tempest", "gale") to words("raging", "wild", "fury", "lashed", "howled")),
        AmbienceKind.WIND to (words("wind", "winds", "breeze", "gust", "gusts") to words("blew", "blowing", "blow", "swayed", "fluttered", "cloak")),
        AmbienceKind.OCEAN to (words("sea", "ocean", "waves", "shore", "ship", "whale", "tide") to words("sail", "sailed", "boat", "harbor", "harbour", "fish", "water", "deep", "coast", "island")),
        AmbienceKind.STREAM to (words("river", "stream", "brook", "creek", "spring", "fountain") to words("flowing", "flowed", "ripple", "ripples", "bathe", "drank", "splash")),
        AmbienceKind.FOREST to (words("forest", "woods", "meadow", "garden", "orchard", "grove") to words("trees", "tree", "grass", "birds", "bird", "field", "fields", "flowers", "leaves", "path", "hill", "hills", "morning", "spring")),
        // Sleeping alone does not make it night (the hare naps in the afternoon sun).
        AmbienceKind.NIGHT to (words("night", "midnight", "stars", "moon", "moonlight", "crickets", "dusk", "nightfall") to words("dark", "darkness", "evening", "bed", "bedtime", "dream", "dreamed", "lantern", "candle")),
        AmbienceKind.FIRE to (words("fire", "flames", "flame", "campfire", "hearth", "lantern") to words("candle", "glow", "glowing", "warm", "warmth", "embers", "cooked", "kitchen", "cozy", "cosy", "light")),
        AmbienceKind.DESERT to (words("desert", "sand", "dunes", "caravan", "camel", "camels", "oasis") to words("hot", "hottest", "heat", "thirsty", "thirst", "dry", "sun", "blazing", "dust", "dusty", "parched")),
        AmbienceKind.CAVE to (words("cave", "cavern", "well", "prison", "dungeon", "tunnel", "belly") to words("underground", "echo", "echoed", "drip", "dripping", "pit", "stone", "walls", "swallowed", "trapped", "depths")),
        AmbienceKind.HEARTBEAT to (words("afraid", "fear", "danger", "trap", "chased", "attack", "enemy", "terror") to words("trembled", "trembling", "ran", "run", "roar", "roared", "pounded", "shook", "struggled", "escape", "frightened", "scared", "hunter", "hunters")),
        AmbienceKind.CALM to (words("peace", "peaceful", "calm", "prayer", "prayed", "pray", "mercy", "grateful") to words("gentle", "gently", "still", "stillness", "silence", "silent", "soft", "softly", "kind", "kindness", "thank", "thanks", "patience", "forgive", "forgiveness")),
    )

    private val fastWords = words("ran", "run", "running", "raced", "rushed", "sprang", "leapt", "jumped", "shot", "suddenly", "quickly", "fast", "hurry", "hurried", "chased", "dashed", "burst", "roared", "shouted", "cried")
    private val slowWords = words("slowly", "slow", "gently", "gentle", "quietly", "quiet", "softly", "soft", "rest", "rested", "sleep", "asleep", "still", "calm", "peace", "prayer", "prayed", "whispered", "breath", "breathe", "patient", "patience", "waited", "wept")

    private val maleCues = words("he", "his", "him", "himself", "man", "men", "king", "father", "son", "boy", "brother", "brothers", "prophet", "husband", "uncle", "grandfather", "lord", "master", "sir", "hunter", "shepherd", "merchant", "old man", "young man", "sulayman", "yusuf", "yunus", "yaqub", "dawud", "musa", "ibrahim", "ismail", "nuh", "adam")
    private val femaleCues = words("she", "her", "hers", "herself", "woman", "women", "queen", "mother", "daughter", "girl", "sister", "sisters", "wife", "aunt", "grandmother", "lady", "princess", "maryam", "asiya", "hajar", "khadijah", "aisha", "fatimah")

    private fun wordRx(list: List<String>) = Regex("\\b(" + list.joinToString("|") { Regex.escape(it) } + ")\\b", RegexOption.IGNORE_CASE)
    private val sceneRx: Map<AmbienceKind, Pair<Regex, Regex>> = scenes.mapValues { (_, p) -> wordRx(p.first) to wordRx(p.second) }
    private val fastRx = wordRx(fastWords)
    private val slowRx = wordRx(slowWords)
    private val maleRx = wordRx(maleCues)
    private val femaleRx = wordRx(femaleCues)

    private data class Span(val text: String, val quote: Boolean)

    // "The mouse trembled." -> "mouse"; "a tiny old man" -> "man" (only known adjectives are skipped, never verbs)
    private val subjectRx = Regex("\\b(?:the|a|an)\\s+([a-z]+)(?:\\s+([a-z]+))?(?:\\s+([a-z]+))?", RegexOption.IGNORE_CASE)
    private val adjectives = setOf(
        "old", "young", "little", "tiny", "great", "big", "small", "wise", "poor", "kind", "brave", "clever", "proud",
        "gentle", "hungry", "thirsty", "humble", "mighty", "wicked", "good", "evil", "beautiful", "ugly", "rich", "weary",
        "tired", "frightened", "angry", "happy", "sad", "lonely", "strange", "mysterious", "elderly", "noble", "royal",
    )

    private fun nounFrom(m: MatchResult): String {
        val w = m.groupValues.drop(1).filter { it.isNotEmpty() }.map { it.lowercase() }
        var i = 0
        while (i < w.size - 1 && w[i] in adjectives) i++
        return w[i]
    }
    private val speechTagRx = Regex(
        "^\\W*(?:he|she|they|it|[A-Z][a-z]+|the\\s+\\w+(?:\\s+\\w+)?)\\s+" +
            "(?:said|says|asked|replied|answered|thundered|whispered|cried|shouted|called(?:\\s+out)?|added|muttered|" +
            "roared|exclaimed|continued|went\\s+on|sighed|began|agreed|protested|pleaded|declared|announced|growled|" +
            "snapped|murmured|insisted|warned)\\b[^.!?]{0,30}[.!?,;:]?\\s*$",
        RegexOption.IGNORE_CASE,
    )

    private fun splitSpans(para: String): List<Span> {
        val spans = mutableListOf<Span>()
        var cursor = 0
        for (m in quoteRx.findAll(para)) {
            val before = para.substring(cursor, m.range.first).trim()
            if (before.isNotEmpty()) spans += Span(before, false)
            spans += Span(m.groupValues[1].trim(), true)
            cursor = m.range.last + 1
        }
        val tail = para.substring(cursor).trim()
        if (tail.isNotEmpty()) spans += Span(tail, false)
        return spans
    }

    private fun attribution(spans: List<Span>, si: Int): Pair<String, String> {
        val after = spans.getOrNull(si + 1)?.takeIf { !it.quote }?.text?.split(sentenceRx)?.firstOrNull() ?: ""
        val before = spans.getOrNull(si - 1)?.takeIf { !it.quote }?.text?.split(sentenceRx)?.lastOrNull() ?: ""
        return before to after
    }

    private fun subjectNoun(text: String): String? = subjectRx.find(text)?.let { nounFrom(it) }

    /** Pass 1: learn each character's gender from lines that do carry a pronoun ("... the mouse ... she said"). */
    private fun learnCast(paragraphs: List<String>): Map<String, Role> {
        val votes = HashMap<String, IntArray>() // noun -> [male, female]
        for (para in paragraphs) {
            val spans = splitSpans(para)
            spans.forEachIndexed { si, sp ->
                if (!sp.quote) return@forEachIndexed
                val (before, after) = attribution(spans, si)
                val cue = genderOf(after) ?: genderOf(before) ?: return@forEachIndexed
                val noun = subjectNoun(before) ?: subjectNoun(after) ?: return@forEachIndexed
                if (noun in maleCues || noun in femaleCues) return@forEachIndexed
                val v = votes.getOrPut(noun) { IntArray(2) }
                if (cue == Role.MALE) v[0]++ else v[1]++
            }
        }
        return votes.filter { (_, v) -> v[0] != v[1] }.mapValues { (_, v) -> if (v[0] > v[1]) Role.MALE else Role.FEMALE }
    }

    private fun castLookup(text: String, cast: Map<String, Role>): Role? {
        for (m in subjectRx.findAll(text)) {
            cast[nounFrom(m)]?.let { return it }
        }
        return null
    }

    fun plan(story: Story): Plan {
        val baseSpeed = when (story.category) {
            Categories.CALM -> 0.85f
            Categories.FAITH -> 0.9f
            else -> 0.95f
        }
        var scene = when {
            "sleep" in story.moods -> AmbienceKind.NIGHT
            story.category == Categories.CALM -> AmbienceKind.CALM
            story.category == Categories.FABLES -> AmbienceKind.FOREST
            else -> AmbienceKind.CALM
        }
        val paragraphs = story.body.replace("\r", "").split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableListOf<Segment>()

        // Opening scene: let the first paragraph set the stage if it has a clear signal.
        paragraphs.firstOrNull()?.let { first ->
            val best = sceneScores(first).filter { it.key != AmbienceKind.HEARTBEAT }.maxByOrNull { it.value }
            if (best != null && best.value >= 2) scene = best.key
        }
        out += Segment("${story.title}.", Role.NARRATOR, baseSpeed, scene)

        var sinceChange = 99
        var lastSpeaker: Role? = null
        var prevSpeaker: Role? = null
        val cast = learnCast(paragraphs)

        paragraphs.forEachIndexed { pi, para ->
            // --- scene ---
            val scores = sceneScores(para)
            val tension = (scores[AmbienceKind.HEARTBEAT] ?: 0) >= 3
            val best = scores.filter { it.key != AmbienceKind.HEARTBEAT }.maxByOrNull { it.value }
            if (best != null && best.key != scene && best.value >= 2 &&
                best.value >= (scores[scene] ?: 0) + 1 && (sinceChange >= 1 || best.value >= 4)
            ) {
                scene = best.key
                sinceChange = 0
            } else sinceChange++
            val ambience = if (tension) AmbienceKind.HEARTBEAT else scene

            // --- pace ---
            val fast = fastRx.findAll(para).count()
            val slow = slowRx.findAll(para).count()
            val excl = para.count { it == '!' }
            var speed = baseSpeed + 0.04f * (fast - slow).coerceIn(-2, 2) + 0.03f * excl.coerceAtMost(2)
            if (pi == paragraphs.lastIndex && paragraphs.size > 2) speed -= 0.05f
            speed = (speed.coerceIn(0.8f, 1.15f) * 100).roundToInt() / 100f

            // --- voices: quotes first (they may span sentences), then attribute each quote ---
            val spans = splitSpans(para)
            val pieces = mutableListOf<Pair<String, Role>>()
            spans.forEachIndexed { si, sp ->
                if (!sp.quote) {
                    // Drop short speech tags next to a quote ("he said quietly."): the voice change already says who spoke.
                    val nextIsQuote = spans.getOrNull(si + 1)?.quote == true
                    val prevIsQuote = spans.getOrNull(si - 1)?.quote == true
                    val isTag = (nextIsQuote || prevIsQuote) && sp.text.split(Regex("\\s+")).size <= 6 && speechTagRx.matches(sp.text)
                    if (!isTag) pieces += sp.text to Role.NARRATOR
                    return@forEachIndexed
                }
                val (before, after) = attribution(spans, si)
                val cue = genderOf(after) ?: genderOf(before) ?: castLookup(before, cast) ?: castLookup(after, cast)
                val speaker = cue ?: run {
                    val straightAfterQuote = spans.getOrNull(si - 1)?.quote == true
                    if (straightAfterQuote && prevSpeaker != null && prevSpeaker != lastSpeaker) prevSpeaker!!
                    else lastSpeaker ?: Role.MALE
                }
                if (speaker != lastSpeaker) { prevSpeaker = lastSpeaker; lastSpeaker = speaker }
                pieces += sp.text to speaker
            }

            // merge neighbours with the same role; split long narrator runs at sentence ends so they stream
            val merged = mutableListOf<Pair<StringBuilder, Role>>()
            for ((t, r) in pieces) {
                val last = merged.lastOrNull()
                if (last != null && last.second == r && last.first.length + t.length < 900) last.first.append(' ').append(t)
                else merged += StringBuilder(t) to r
            }
            for ((sb, r) in merged) {
                val text = sb.toString()
                val quoteSpeed = if (r != Role.NARRATOR && text.endsWith("!")) (speed + 0.05f).coerceAtMost(1.15f) else speed
                for (chunk in splitLong(text, 900)) out += Segment(chunk, r, quoteSpeed, ambience)
            }
        }
        return Plan(out)
    }

    private fun splitLong(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (s in text.split(sentenceRx)) {
            if (sb.isNotEmpty() && sb.length + s.length + 1 > max) { out += sb.toString(); sb.clear() }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(s)
        }
        if (sb.isNotEmpty()) out += sb.toString()
        return out
    }

    private fun sceneScores(text: String): Map<AmbienceKind, Int> =
        sceneRx.mapValues { (_, rx) -> rx.first.findAll(text).count() * 2 + rx.second.findAll(text).count() }

    private fun genderOf(text: String): Role? {
        if (text.isBlank()) return null
        val m = maleRx.findAll(text).count()
        val f = femaleRx.findAll(text).count()
        return when {
            m == 0 && f == 0 -> null
            f > m -> Role.FEMALE
            else -> Role.MALE
        }
    }
}
