package app.lamplore.data

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * An optional lock on the app: fingerprint, face or the phone's PIN. Playback carries on in the background
 * while locked; only the screens are covered. It can only be turned on when the phone has a screen lock.
 */
object Lock {
    private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    var enabled by mutableStateOf(false)
        private set

    /** Seconds away before the lock asks again (0 = every time). */
    var graceSeconds by mutableStateOf(0)
        private set

    var locked by mutableStateOf(false)

    private var leftAt = 0L
    private val prefs get() = app.lamplore.App.context.getSharedPreferences("murmur_extra", Context.MODE_PRIVATE)

    fun init() {
        enabled = prefs.getBoolean("lock_on", false)
        graceSeconds = prefs.getInt("lock_grace", 0)
        locked = enabled // a cold start is always locked
    }

    fun available(ctx: Context): Boolean = BiometricManager.from(ctx).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun enable(on: Boolean) { enabled = on; prefs.edit().putBoolean("lock_on", on).apply(); if (!on) locked = false }
    fun changeGrace(s: Int) { graceSeconds = s; prefs.edit().putInt("lock_grace", s).apply() }

    fun onLeave() { leftAt = SystemClock.elapsedRealtime() }

    fun onReturn() {
        if (enabled && !locked && SystemClock.elapsedRealtime() - leftAt >= graceSeconds * 1000L) locked = true
    }

    /** Ask the phone to confirm it is you. */
    fun prompt(activity: FragmentActivity, onOk: () -> Unit) {
        val bp = BiometricPrompt(
            activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { locked = false; onOk() }
            },
        )
        bp.authenticate(
            BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Lamplore").setAllowedAuthenticators(AUTH).build(),
        )
    }
}
