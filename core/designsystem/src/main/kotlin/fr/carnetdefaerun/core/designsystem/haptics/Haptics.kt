package fr.carnetdefaerun.core.designsystem.haptics

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Retours haptiques de l'appli. On passe par les constantes du système, qui respectent
 * automatiquement les réglages de vibration du téléphone.
 */
class CarnetHaptics(private val view: View) {
    /** Case cochée, sceau apposé. */
    fun confirm() = view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

    /** Action annulée ou refusée. */
    fun reject() = view.performHapticFeedback(HapticFeedbackConstants.REJECT)

    /** Cran d'un sélecteur, changement d'acte. */
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)

    /** Début d'un geste (brume qu'on commence à dissiper). */
    fun gestureStart() = view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)

    /** Fin d'un geste (spoiler révélé). */
    fun gestureEnd() = view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)

    fun click() = view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
}

@Composable
fun rememberCarnetHaptics(): CarnetHaptics {
    val view = LocalView.current
    return remember(view) { CarnetHaptics(view) }
}
