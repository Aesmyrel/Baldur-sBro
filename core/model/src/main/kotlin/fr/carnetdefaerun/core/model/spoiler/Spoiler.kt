package fr.carnetdefaerun.core.model.spoiler

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Moment de la partie. L'ordre des constantes est l'ordre chronologique. */
@Serializable
enum class Act {
    @SerialName("prologue") PROLOGUE,
    @SerialName("acte1") ACT_1,
    @SerialName("acte2") ACT_2,
    @SerialName("acte3") ACT_3,
    @SerialName("epilogue") EPILOGUE,
}

/** Sensibilité d'une information (docs/PLAN.md §2.1). */
@Serializable
enum class SpoilerLevel {
    /** Descriptif, ne révèle rien. */
    @SerialName("aucun") NONE,

    /** Laisse deviner, donne un indice. */
    @SerialName("leger") LIGHT,

    /** Révèle un rebondissement ou une issue. */
    @SerialName("majeur") MAJOR,
}

/** Tolérance choisie par le joueur, pour BG3 et, séparément, pour les autres œuvres. */
@Serializable
enum class Tolerance {
    @SerialName("aucun") NONE,
    @SerialName("leger") LIGHT,
    @SerialName("complet") FULL,
}

/**
 * Étiquette portée par chaque bloc affichable : acte où l'info est connue en jouant,
 * niveau de spoiler pour BG3 et niveau de spoiler pour les autres œuvres.
 */
@Serializable
data class SpoilerTag(
    // Aucune valeur par défaut : une étiquette absente ou mal nommée doit être une erreur de lecture,
    // jamais un « visible par tous » silencieux.
    val act: Act,
    val level: SpoilerLevel,
    val otherWorks: SpoilerLevel,
) {
    /** Étiquette la plus protectrice des deux : sert à vérifier qu'un titre protège ce qu'il résume. */
    fun strictest(other: SpoilerTag) = SpoilerTag(
        act = maxOf(act, other.act),
        level = maxOf(level, other.level),
        otherWorks = maxOf(otherWorks, other.otherWorks),
    )

    /** Vrai si cette étiquette protège au moins autant que [other] sur chaque axe. */
    fun protectsAtLeast(other: SpoilerTag) =
        act >= other.act && level >= other.level && otherWorks >= other.otherWorks

    companion object {
        /** Information connue dès le début de la partie et sans aucune révélation. */
        val SAFE = SpoilerTag(Act.PROLOGUE, SpoilerLevel.NONE, SpoilerLevel.NONE)

        /** Étiquette la plus stricte : valeur d'attente du pipeline, remplacée avant toute écriture. */
        val LOCKED = SpoilerTag(Act.EPILOGUE, SpoilerLevel.MAJOR, SpoilerLevel.MAJOR)
    }
}

/** Progression et réglages du joueur. Par défaut : début de partie, aucune tolérance. */
@Serializable
data class SpoilerProfile(
    val currentAct: Act = Act.PROLOGUE,
    val tolerance: Tolerance = Tolerance.NONE,
    val otherWorksTolerance: Tolerance = Tolerance.NONE,
)
