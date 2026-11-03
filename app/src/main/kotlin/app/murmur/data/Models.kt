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
) {
    val words: Int get() = body.split(Regex("\\s+")).count { it.isNotBlank() }
    val minutes: Int get() = maxOf(1, Math.round(words / 150f))
}

data class Reflection(val date: String, val text: String, val storyTitle: String?)

object Categories {
    const val FAITH = "Faith & wisdom"
    const val FABLES = "Fables"
    const val CALM = "Sleep & calm"
    const val YOURS = "Yours"
    val all = listOf(FABLES, FAITH, CALM, YOURS)
    val forUpload = listOf(FABLES, FAITH, CALM, YOURS)
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
