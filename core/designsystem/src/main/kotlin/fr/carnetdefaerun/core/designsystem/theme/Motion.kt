package fr.carnetdefaerun.core.designsystem.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat

/** Principes de mouvement : durées courtes (150–400 ms), interruptibles ; fondus de 100 ms en mouvement réduit. */
@Immutable
data class CarnetMotion(val reduced: Boolean) {
    val short: Int get() = if (reduced) 100 else 160
    val medium: Int get() = if (reduced) 100 else 280
    val long: Int get() = if (reduced) 100 else 400

    /** Animations décoratives (shaders, halo, reflet d'or) : coupées en mouvement réduit. */
    val ambient: Boolean get() = !reduced

    companion object {
        /** Courbe « encre » : départ vif, arrivée posée. */
        val Ink: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** Courbe « page » pour les transitions de lecture. */
        val Page: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
    }
}

/**
 * Mouvement réduit si l'échelle d'animation système est à 0, si l'économiseur de batterie est actif,
 * ou si le joueur l'a demandé dans l'appli ([userOverride] = true). Suit les changements en direct.
 */
@Composable
fun rememberReducedMotion(userOverride: Boolean): Boolean {
    if (LocalInspectionMode.current) return userOverride
    val context = LocalContext.current
    var systemReduced by remember { mutableStateOf(systemWantsReducedMotion(context)) }
    DisposableEffect(context) {
        val update = { systemReduced = systemWantsReducedMotion(context) }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = update()
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer,
        )
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = update()
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    return userOverride || systemReduced
}

private fun systemWantsReducedMotion(context: Context): Boolean {
    val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    return scale == 0f || powerSave
}
