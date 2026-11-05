package app.lamplore.data

import app.lamplore.audio.AmbienceKind

/** The subject drawn on a story's cover, so the picture says what the story is about. */
enum class Motif {
    TORTOISE_HARE, LION, CROW_PITCHER, SUN_WIND, MOUNTAIN, HORSE, POT, LANTERN,
    FOX_GRAPES, WOLF, ANT, GOOSE, SWAN, ELEPHANT, LIGHTHOUSE, TRAIN, CABIN, BOAT, MOON, BOOK;

    companion object {
        private val keywords = listOf(
            Motif.TORTOISE_HARE to listOf("tortoise", "turtle", "hare", "rabbit"),
            Motif.LION to listOf("lion", "tiger", "jungle"),
            Motif.WOLF to listOf("wolf", "wolves"),
            Motif.FOX_GRAPES to listOf("fox", "grape", "vineyard"),
            Motif.HORSE to listOf("horse", "pony", "stallion", "farm"),
            Motif.ELEPHANT to listOf("elephant"),
            Motif.GOOSE to listOf("goose", "geese", "golden egg"),
            Motif.SWAN to listOf("swan", "duck", "pond"),
            Motif.ANT to listOf("ant ", "ants", "bee ", "bees", "insect"),
            Motif.LIGHTHOUSE to listOf("lighthouse"),
            Motif.TRAIN to listOf("train", "railway", "station"),
            Motif.BOAT to listOf("boat", "ship", "sail", "canoe", "lake"),
            Motif.CABIN to listOf("cabin", "cottage", "hut", "rain on the roof"),
            Motif.MOUNTAIN to listOf("mountain", "peak", "climb", "hill"),
            Motif.POT to listOf("soup", "kitchen", "cook", "stew", "bread"),
            Motif.LANTERN to listOf("lantern", "lamp", "candle"),
            Motif.SUN_WIND to listOf("sun ", "wind", "breeze"),
            Motif.MOON to listOf("moon", "star", "night sky", "dream"),
        )

        /** Picks a motif for a story that has none (your own stories) from words in its title, then its text. */
        fun guess(title: String, body: String): Motif {
            val t = " ${title.lowercase()} "
            keywords.firstOrNull { (_, ws) -> ws.any { it in t } }?.let { return it.first }
            val b = " ${body.lowercase().take(1500)} "
            val counts = keywords.map { (m, ws) -> m to ws.sumOf { w -> Regex(Regex.escape(w)).findAll(b).count() } }
            val best = counts.maxByOrNull { it.second }
            return if (best != null && best.second >= 2) best.first else BOOK
        }
    }
}

fun sceneForCategory(category: String): AmbienceKind = when (category) {
    Categories.FABLES, Categories.CLASSICS -> AmbienceKind.FOREST
    Categories.WISDOM -> AmbienceKind.DESERT
    Categories.CALM -> AmbienceKind.NIGHT
    else -> AmbienceKind.CALM
}
