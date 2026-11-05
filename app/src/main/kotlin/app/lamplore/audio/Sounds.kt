package app.lamplore.audio

/**
 * Background sounds. Real field recordings bundled as Opus loops in assets/sounds (credits in
 * docs/licenses/SOUNDS.md). Kinds without a file are "silence + music" scenes.
 */
enum class AmbienceKind(val label: String, val file: String?) {
    NONE("Off", null),
    RAIN("Rain", "rain"),
    STORM("Thunderstorm", "storm"),
    WIND("Wind", "wind"),
    OCEAN("Ocean", "ocean"),
    STREAM("Stream", "stream"),
    FOREST("Forest birds", "forest"),
    NIGHT("Night crickets", "night"),
    FIRE("Campfire", "fire"),
    DESERT("Desert wind", "desert"),
    CAVE("Cave drips", "cave"),
    HEARTBEAT("Tension", null),
    CALM("Deep calm", null),
}

/** Instrumental music beds (Kevin MacLeod, CC BY 4.0). */
enum class MusicMood(val label: String, val file: String?) {
    NONE("Off", null),
    CALM("Calm", "m_calm"),
    SLEEP("Sleep", "m_sleep"),
    HOPEFUL("Hopeful", "m_hopeful"),
    ADVENTURE("Adventure", "m_adventure"),
    TENSION("Tension", "m_tension"),
    SAD("Sad", "m_sad"),
    WARM("Warm", "m_warm"),
}
