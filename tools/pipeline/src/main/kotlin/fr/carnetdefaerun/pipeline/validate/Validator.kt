package fr.carnetdefaerun.pipeline.validate

import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.NameStatus
import fr.carnetdefaerun.core.model.content.SpellFacts
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag

/** Licences admises dans la base, et ce qu'elles exigent (docs/SOURCES.md). */
private val LICENCES = mapOf(
    "cc-by-nc-sa-4.0" to "attribution par fiche (page + révision)",
    "carnet" to "texte rédigé pour le carnet",
)

/**
 * Contrôles bloquants des données de jeu (PLAN §3.4) : identifiants, noms, étiquettes de spoiler
 * cohérentes, licences et attributions, liens. Chaque problème est décrit en une ligne.
 */
class Validator {
    /** Avertissements non bloquants (traductions à faire, champs inconnus) : comptés dans le rapport. */
    val warnings = mutableListOf<String>()

    fun validate(entries: List<Entry>): List<String> {
        val problems = mutableListOf<String>()
        val byId = entries.groupBy { it.id }
        byId.filterValues { it.size > 1 }.keys.forEach { problems += "$it : identifiant en double" }
        val index = entries.associateBy { it.id }

        for (e in entries) {
            val where = e.id.value
            if (e.name.fr.isBlank() || e.name.en.isBlank()) problems += "$where : nom manquant"
            if (e.name.status == NameStatus.UNOFFICIAL && e.name.fr == e.name.en.replace('\'', '’')) warnings += "$where : nom français à proposer"
            if (e.name.status == NameStatus.CORRECTED) problems += "$where : statut « corrigé » réservé aux corrections locales du joueur"

            // Étiquettes : jamais l'attente du pipeline, et la fiche protège au moins autant que son nom.
            if (e.tag == SpoilerTag.LOCKED || e.titleTag == SpoilerTag.LOCKED) problems += "$where : étiquette non calculée"
            if (!e.tag.protectsAtLeast(e.titleTag)) problems += "$where : la fiche est moins protégée que son nom"

            if (e.licence !in LICENCES) problems += "$where : licence inconnue « ${e.licence} »"
            if (e.licence == "cc-by-nc-sa-4.0") {
                if (e.attribution.isEmpty()) problems += "$where : attribution manquante"
                e.attribution.forEach {
                    if (it.revision <= 0 || !it.url.startsWith("https://bg3.wiki/wiki/")) problems += "$where : attribution incomplète (${it.title})"
                }
            }

            for (link in e.links) {
                val target = index[link.target]
                if (target == null) problems += "$where : lien vers ${link.target} absent de la base"
                else if (!link.tag.protectsAtLeast(e.tag) || !link.tag.protectsAtLeast(target.tag)) problems += "$where : lien vers ${link.target} moins protégé que ses extrémités"
            }

            for (block in e.blocks) {
                if (block.text.isBlank()) problems += "$where : bloc ${block.id} vide"
                if (!block.tag.protectsAtLeast(e.tag)) problems += "$where : bloc ${block.id} moins protégé que sa fiche"
                if (Regex("[ ][;:!?»]|«[ ]").containsMatchIn(block.text)) problems += "$where : bloc ${block.id}, espace ordinaire autour d'une ponctuation haute"
            }

            when (val f = e.facts) {
                is SpellFacts -> {
                    if (f.level !in 0..9) problems += "$where : niveau de sort hors bornes (${f.level})"
                    if (f.learnedBy.isEmpty()) problems += "$where : sort retenu sans classe ni race qui l'apprenne"
                }
                is ItemFacts -> f.sources.forEach { s ->
                    // L'acte d'un objet est celui de sa source la plus précoce : aucune source ne le précède.
                    if (s.act?.let { it < e.tag.act } == true) problems += "$where : source d'obtention antérieure à l'acte de l'objet"
                    if (s.tag.act < e.tag.act) problems += "$where : source moins protégée que l'objet"
                    if (s.vendor != null && s.tag.level == SpoilerLevel.NONE) {
                        problems += "$where : nom de marchand sans étiquette « léger » au moins"
                    }
                    if (s.act == null && s.tag.act != Act.ACT_3) problems += "$where : source d'acte inconnu insuffisamment protégée"
                }
                is LoreFacts -> Unit // contrôlé par LoreValidator
                else -> Unit
            }
            if (e.unknown.isNotEmpty()) warnings += "$where : champs inconnus ${e.unknown}"
        }
        return problems
    }
}
