package fr.carnetdefaerun.core.model.settings

import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import kotlinx.serialization.Serializable

/** Modes de difficulté de BG3 (libellés VF à confirmer en jeu, voir docs/DECISIONS.md). */
@Serializable
enum class Difficulty { EXPLORER, BALANCED, TACTICIAN, HONOUR, CUSTOM }

@Serializable
enum class AppTheme { DARK, OLED, PARCHMENT }

/** Préférences et progression déclarée du joueur. Valeurs par défaut : début de partie, zéro spoiler. */
@Serializable
data class UserPreferences(
    val currentAct: Act = Act.PROLOGUE,
    val tolerance: Tolerance = Tolerance.NONE,
    val otherWorksTolerance: Tolerance = Tolerance.NONE,
    val difficulty: Difficulty = Difficulty.BALANCED,
    val theme: AppTheme = AppTheme.DARK,
    /** Facteur appliqué au texte de lecture (en plus de la taille de police du système). */
    val readingScale: Float = 1f,
    /** Mouvement réduit demandé dans l'appli (s'ajoute au réglage système et à l'économiseur de batterie). */
    val reduceMotion: Boolean = false,
    /** Mode session : écran toujours allumé tant qu'il est actif. */
    val sessionActive: Boolean = false,
    /** Afficher l'acte d'un contenu masqué (désactivé par défaut, D-022). */
    val showActOfHidden: Boolean = false,
) {
    val spoilerProfile: SpoilerProfile get() = SpoilerProfile(currentAct, tolerance, otherWorksTolerance)

    companion object {
        val READING_SCALES = listOf(0.9f, 1f, 1.1f, 1.25f, 1.4f)
    }
}
