package fr.carnetdefaerun.core.model.spoiler

import java.security.MessageDigest

/**
 * Verrou anti-spoiler : décide si un bloc peut être montré (spécification : docs/PLAN.md §2.3,
 * sens de la tolérance légère : docs/DECISIONS.md D-028). Tout filtrage de contenu passe par ici.
 */
object SpoilerGate {

    fun isVisible(tag: SpoilerTag, profile: SpoilerProfile): Boolean =
        bg3Allows(tag, profile) && otherWorksAllow(tag.otherWorks, profile.otherWorksTolerance)

    /** Visible selon le profil, ou révélé explicitement par le joueur. */
    fun isVisible(tag: SpoilerTag, profile: SpoilerProfile, revealed: Boolean): Boolean =
        revealed || isVisible(tag, profile)

    private fun bg3Allows(tag: SpoilerTag, profile: SpoilerProfile): Boolean {
        val current = profile.currentAct
        return when {
            tag.act < current -> true
            tag.act == current -> tag.level <= maxLevelFor(profile.tolerance)
            else -> when (profile.tolerance) {
                Tolerance.NONE -> false
                Tolerance.LIGHT -> tag.level == SpoilerLevel.NONE
                Tolerance.FULL -> true
            }
        }
    }

    private fun otherWorksAllow(level: SpoilerLevel, tolerance: Tolerance): Boolean =
        level <= maxLevelFor(tolerance)

    private fun maxLevelFor(tolerance: Tolerance): SpoilerLevel = when (tolerance) {
        Tolerance.NONE -> SpoilerLevel.NONE
        Tolerance.LIGHT -> SpoilerLevel.LIGHT
        Tolerance.FULL -> SpoilerLevel.MAJOR
    }
}

/**
 * Poignée opaque d'un bloc masqué : un condensé de son identifiant, qui ne laisse lire ni titre
 * ni nom (les identifiants dérivent des noms anglais). Seul le dépôt sait la rapprocher du bloc.
 */
@JvmInline
value class RevealKey private constructor(val token: String) {
    companion object {
        fun of(blockId: String): RevealKey {
            val digest = MessageDigest.getInstance("SHA-256").digest(blockId.toByteArray(Charsets.UTF_8))
            return RevealKey(digest.take(12).joinToString("") { "%02x".format(it) })
        }
    }
}

/**
 * Résultat du filtrage tel qu'un écran le reçoit. Un bloc masqué ne transporte jamais son contenu,
 * son titre, son identifiant lisible ni son niveau ; l'acte n'est fourni que si le joueur a demandé
 * à le voir (D-022).
 */
sealed interface Gated<out T> {
    data class Visible<T>(val value: T) : Gated<T>
    data class Hidden(val key: RevealKey, val act: Act? = null) : Gated<Nothing>
}

fun <T> T.gated(
    blockId: String,
    tag: SpoilerTag,
    profile: SpoilerProfile,
    revealed: Boolean = false,
    showActOfHidden: Boolean = false,
): Gated<T> =
    if (SpoilerGate.isVisible(tag, profile, revealed)) Gated.Visible(this)
    else Gated.Hidden(RevealKey.of(blockId), if (showActOfHidden) tag.act else null)
