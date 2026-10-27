package fr.carnetdefaerun.core.model.content

import kotlinx.serialization.Serializable

/**
 * Identifiant stable d'une entrée, dérivé du nom anglais : `spell.fireball`, `lore.deity.selune`.
 * Il ne change jamais, même si le nom affiché change ; la base utilisateur s'y réfère.
 */
@Serializable
@JvmInline
value class ContentId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "Identifiant invalide : « $value »" }
    }

    val kind: String get() = value.substringBefore('.')

    override fun toString() = value

    companion object {
        val PATTERN = Regex("^[a-z][a-z0-9]*(\\.[a-z0-9]+(-[a-z0-9]+)*)+$")
    }
}

/** Fiabilité d'un nom français (docs/SOURCES.md §4). */
@Serializable
enum class NameStatus {
    /** Vérifié dans la VF de BG3. */
    VF_BG3,

    /** Traduction officielle de D&D ; pour un terme du jeu, non vérifiée dans BG3. */
    VF_DND,

    /** Aucune source fiable : traduction proposée par le projet. */
    UNOFFICIAL,

    /** Corrigé par le joueur dans l'appli (stocké localement, prioritaire). */
    CORRECTED,
}
