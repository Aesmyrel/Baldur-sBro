package fr.carnetdefaerun.core.model.content

/**
 * Sous-type d'une entrée pour les filtres et regroupements de liste : école d'un sort, catégorie d'un objet,
 * nature d'une capacité, acte d'un lieu, thème d'une fiche de lore, classe ou race parente.
 * Calculé une fois par le pipeline (colonne de la base) et par les doubles de test, avec la même règle.
 */
val Entry.listSubtype: String?
    get() = when (val f = facts) {
        is SpellFacts -> f.school?.name
        is ItemFacts -> f.category.name
        is FeatureFacts -> f.kind.name
        is PlaceFacts -> f.act?.name
        is LoreFacts -> f.theme.name
        is SubclassFacts -> f.parentClass.value
        is SubraceFacts -> f.parentRace.value
        is ConditionFacts, is ClassFacts, is RaceFacts, is BackgroundFacts, is FeatFacts -> null
    }
