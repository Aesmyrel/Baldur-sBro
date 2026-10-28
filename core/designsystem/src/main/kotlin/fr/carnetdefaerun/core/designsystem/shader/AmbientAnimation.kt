package fr.carnetdefaerun.core.designsystem.shader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Jeton unique d'animation décorative : un seul shader animé visible à la fois (charte §7).
 * Un composant le prend quand il est à l'écran, le rend dès qu'il en sort ou a fini.
 */
@Stable
class AmbientAnimationSlot {
    private var owner by mutableStateOf<Any?>(null)

    fun isAvailableTo(who: Any): Boolean = owner == null || owner === who

    fun acquire(who: Any): Boolean {
        if (!isAvailableTo(who)) return false
        owner = who
        return true
    }

    fun release(who: Any) {
        if (owner === who) owner = null
    }
}

val LocalAmbientAnimation = staticCompositionLocalOf { AmbientAnimationSlot() }
