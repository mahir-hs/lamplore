package app.murmur.data

data class Story(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val moods: Set<String>,
    val summary: String,
    val body: String,
    val rights: String,
    val isUser: Boolean = false,
    val motif: Motif? = null,
    val scene: app.murmur.audio.AmbienceKind? = null,
    val plates: List<Plate> = emptyList(),
    /** Recorded audio streamed from the internet (LibriVox books, podcast episodes). Empty = read aloud by Murmur. */
    val tracks: List<Track> = emptyList(),
) {
    val isStream: Boolean get() = tracks.isNotEmpty()
    val artMotif: Motif get() = motif ?: Motif.guess(title, body)
    val artScene: app.murmur.audio.AmbienceKind get() = scene ?: sceneForCategory(category)
    val words: Int get() = body.split(Regex("\\s+")).count { it.isNotBlank() }
    val minutes: Int get() = if (isStream) maxOf(1, tracks.sumOf { it.seconds } / 60) else maxOf(1, Math.round(words / 150f))
}

/** One recorded file: a book chapter or a podcast episode. `seconds` may be 0 when unknown. */
data class Track(val title: String, val url: String, val seconds: Int)

data class Reflection(val date: String, val text: String, val storyTitle: String?)

object Categories {
    const val WISDOM = "Wisdom tales"
    const val FABLES = "Fables"
    const val CLASSICS = "Classic tales"
    const val CALM = "Sleep & calm"
    const val YOURS = "Yours"
    const val AUDIOBOOKS = "Audiobooks"
    const val PODCASTS = "Podcasts"
    val all = listOf(FABLES, CLASSICS, WISDOM, CALM, YOURS)
    val forUpload = listOf(FABLES, CLASSICS, WISDOM, CALM, YOURS)
}

object Moods {
    val all = listOf(
        "calm" to "Calm down",
        "sleep" to "Sleep",
        "learn" to "Learn",
        "inspire" to "Be inspired",
        "patience" to "Patience",
    )
}

object Rights {
    const val PUBLIC_DOMAIN = "Public-domain tale, retold"
    const val ORIGINAL = "Original by Murmur"
    const val RETOLD = "Retold narrative (review before publishing)"
    const val USER_OWN = "I wrote this"
    const val USER_PD = "Public domain"
    const val USER_PERMISSION = "I have permission"
    val userChoices = listOf(USER_OWN, USER_PD, USER_PERMISSION)
}

/**
 * A real illustration from a public-domain book, bundled in assets/plates. The crop (fractions of the
 * image) is what the cover shows; the player shows the whole plate with its credit.
 */
data class Plate(
    val asset: String,
    val credit: String,
    val l: Float = 0f,
    val t: Float = 0f,
    val r: Float = 1f,
    val b: Float = 1f,
)

object Books {
    const val CRANE = "Walter Crane, The Baby's Own Aesop (1887), engraved by Edmund Evans"
}
