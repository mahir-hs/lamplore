package app.lamplore.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs

enum class Verdict { GOOD, SLOW, NO }

/** One thing the phone might be asked to do, and whether it can. */
data class Capability(val title: String, val verdict: Verdict, val detail: String)

data class DeviceReport(
    val model: String,
    val chip: String,
    val androidVersion: String,
    val is64Bit: Boolean,
    val cores: Int,
    val ramGb: Float,
    val freeStorageGb: Float,
    val capabilities: List<Capability>,
) {
    /** One plain line describing the phone. */
    val summary: String
        get() = buildString {
            append(model)
            if (chip.isNotBlank()) append(", ").append(chip)
            append(", ").append("%.1f".format(ramGb)).append(" GB memory, ")
            append(cores).append(" cores, ")
            append(if (is64Bit) "64-bit" else "32-bit").append(", Android ").append(androidVersion).append(".")
        }
}

/**
 * Looks at the phone and says, in plain terms, what it can run well: better voices, and the AI Director's
 * two model sizes. These are estimates from memory, chip and storage; the measured numbers from
 * "Test the AI on this phone" replace them when you run it.
 */
object DeviceCheck {
    fun run(ctx: Context): DeviceReport {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val ramGb = mem.totalMem / 1_073_741_824f
        val free = runCatching { StatFs(ctx.filesDir.path).availableBytes / 1_073_741_824f }.getOrDefault(0f)
        val is64 = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val cores = Runtime.getRuntime().availableProcessors()
        val chip = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL.takeIf { it.isNotBlank() && it != "unknown" } ?: "" else ""

        val caps = mutableListOf<Capability>()

        caps += when {
            ramGb >= 2f -> Capability("Better voices (Piper)", Verdict.GOOD, "Small and fast. Stories start almost at once.")
            else -> Capability("Better voices (Piper)", Verdict.SLOW, "Works, but this phone has little memory. Close other apps first.")
        }
        caps += when {
            ramGb >= 6f && cores >= 6 -> Capability("Kokoro voices", Verdict.SLOW, "Most natural, but slower than real time, so a story needs a short head start before it plays.")
            ramGb >= 4f -> Capability("Kokoro voices", Verdict.SLOW, "Works with a long head start before each story. Piper voices are a better fit.")
            else -> Capability("Kokoro voices", Verdict.NO, "Needs about 4 GB of memory. Use Piper voices instead.")
        }
        caps += aiCapability("AI Director, small model", AiModels.SMALL, is64, ramGb, free, goodRam = 6f, minRam = 3f)
        caps += aiCapability("AI Director, large model", AiModels.LARGE, is64, ramGb, free, goodRam = 8f, minRam = 6f)
        caps += when {
            free >= 3f -> Capability("Free storage", Verdict.GOOD, "%.1f GB free. Room for voices, models and story audio.".format(free))
            free >= 1f -> Capability("Free storage", Verdict.SLOW, "%.1f GB free. Enough for one voice or the small AI model, not both.".format(free))
            else -> Capability("Free storage", Verdict.NO, "%.1f GB free. Clear some space before downloading voices or models.".format(free))
        }
        return DeviceReport(Build.MODEL, chip, Build.VERSION.RELEASE, is64, cores, ramGb, free, caps)
    }

    private fun aiCapability(title: String, model: AiModel, is64: Boolean, ramGb: Float, free: Float, goodRam: Float, minRam: Float): Capability {
        val sizeGb = model.sizeMb / 1024f
        return when {
            !is64 -> Capability(title, Verdict.NO, "Needs a 64-bit phone.")
            ramGb < minRam -> Capability(title, Verdict.NO, "Needs about %.0f GB of memory; this phone has %.1f GB.".format(minRam, ramGb))
            free < sizeGb + 0.3f -> Capability(title, Verdict.NO, "Needs %d MB of free storage for the download.".format(model.sizeMb))
            ramGb >= goodRam -> Capability(title, Verdict.GOOD, "Should run well. Download is ${model.sizeMb} MB.")
            else -> Capability(title, Verdict.SLOW, "Should run, but slowly, and other apps may be closed to make room. Download is ${model.sizeMb} MB.")
        }
    }
}
