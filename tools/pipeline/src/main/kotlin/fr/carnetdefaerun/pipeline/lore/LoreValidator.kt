package fr.carnetdefaerun.pipeline.lore

import fr.carnetdefaerun.core.model.content.CanonNature
import fr.carnetdefaerun.core.model.content.Consultation
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.Reference

/**
 * Contrôles bloquants du codex (PLAN §9, phase 2b) : chaque paragraphe a au moins une référence
 * existante et une nature, le canon de D&D a son édition, les étiquettes sont cohérentes (une section
 * ne protège pas moins que sa fiche, la fiche pas moins que son titre), la typographie est française.
 */
class LoreValidator(private val references: Map<String, Reference>) {

    fun validateReferences(): List<String> = buildList {
        references.values.forEach { r ->
            if (r.consultation == Consultation.SECONDARY && (r.via == null || r.via !in references)) add("référence ${r.id} : source secondaire « via » manquante ou inconnue")
            if (r.url != null && r.revision == null && r.kind.name == "WIKI") add("référence ${r.id} : page de wiki sans révision")
        }
    }

    fun validate(entry: Entry, file: String): List<String> {
        val problems = mutableListOf<String>()
        val facts = entry.facts as? LoreFacts ?: return listOf("$file : fiche de lore sans faits de lore")
        if (!entry.id.value.startsWith("lore.")) problems += "$file : identifiant hors de l'espace « lore. »"
        if (!entry.tag.protectsAtLeast(entry.titleTag)) problems += "$file : la fiche est moins protégée que son titre"
        if (facts.sections.isEmpty()) problems += "$file : aucune section"
        checkTypography(facts.summary, "$file, accroche", problems)
        for (section in facts.sections) {
            val where = "$file, ${section.id} « ${section.title} »"
            if (!section.tag.protectsAtLeast(entry.tag)) problems += "$where : section moins protégée que la fiche"
            if (section.paragraphs.isEmpty()) problems += "$where : section vide"
            checkTypography(section.title, where, problems)
            section.paragraphs.forEachIndexed { i, p ->
                val w = "$where, paragraphe ${i + 1}"
                if (p.refs.isEmpty()) problems += "$w : aucune référence"
                p.refs.filter { it.ref !in references }.forEach { problems += "$w : référence inconnue « ${it.ref} »" }
                if (p.nature == CanonNature.CANON_DND && p.edition == null) problems += "$w : canon de D&D sans édition"
                checkTypography(p.text, w, problems)
            }
            // Un groupe de variantes rassemble au moins deux versions, dans la même section.
            section.paragraphs.mapNotNull { it.variantGroup }.groupingBy { it }.eachCount()
                .filterValues { it < 2 }.keys.forEach { problems += "$where : groupe de variantes « $it » à une seule version" }
        }
        return problems
    }

    private fun checkTypography(text: String, where: String, problems: MutableList<String>) {
        if (Regex("[  ][;:!?»]|«[  ]").containsMatchIn(text)) problems += "$where : espace fine insécable attendue autour de la ponctuation"
        if (Regex("(?<=\\p{L})[;!?]").containsMatchIn(text)) problems += "$where : ponctuation haute collée au mot"
        if (Regex("(?<=\\p{L})'(?=\\p{L})").containsMatchIn(text)) problems += "$where : apostrophe droite (attendue : ’)"
    }
}
