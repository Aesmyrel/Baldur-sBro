package fr.carnetdefaerun.feature.builds

import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.rules.RulesData
import fr.carnetdefaerun.core.rules.from
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore

/** Données réelles et builds d'exemple partagés par les tests de l'atelier. */
object BuildFixtures {
    val rules: RulesData by lazy { RulesData.from(GameData.entries) }

    /** Noms visibles au Prologue, triés comme la porte de contenu les trie. */
    fun catalog(entries: List<Entry> = GameData.entries, profile: SpoilerProfile = SpoilerProfile(Act.PROLOGUE)): Catalog {
        val repository = ContentRepository(InMemoryContentStore(entries))
        fun list(type: EntryType) = repository.list(type, profile).let { l -> l.copy(visible = l.visible.sortedBy { Folding.fold(it.name.fr) }) }
        return Catalog.of(list(EntryType.CLASS), list(EntryType.SUBCLASS), list(EntryType.FEAT), list(EntryType.RACE), list(EntryType.SUBRACE), list(EntryType.BACKGROUND))
    }

    private fun id(value: String) = ContentId(value)
    private fun asi(vararg increases: Pair<Ability, Int>) = FeatChoice(id("feat.ability-improvement"), mapOf(*increases))

    /** Le build de la maquette : guerrier 2 puis magicien 10, école d'évocation. */
    val battleMage = Build(
        name = "L’Évocateur en harnois",
        race = id("race.elf"),
        subrace = id("subrace.high-elf"),
        background = id("background.sage"),
        baseScores = Ability.entries.associateWith { 8 } +
            mapOf(Ability.DEXTERITY to 14, Ability.CONSTITUTION to 15, Ability.INTELLIGENCE to 15, Ability.WISDOM to 10),
        majorBonus = Ability.INTELLIGENCE,
        minorBonus = Ability.DEXTERITY,
        levels = List(2) { BuildLevel(id("class.fighter")) } +
            BuildLevel(id("class.wizard")) + BuildLevel(id("class.wizard"), id("subclass.evocation-school")) + BuildLevel(id("class.wizard")) +
            BuildLevel(id("class.wizard"), feat = asi(Ability.INTELLIGENCE to 2)) + List(3) { BuildLevel(id("class.wizard")) } +
            BuildLevel(id("class.wizard"), feat = asi(Ability.INTELLIGENCE to 1, Ability.CONSTITUTION to 1)) + List(2) { BuildLevel(id("class.wizard")) },
    )

    /** Occultiste et ensorceleur : Magie de pacte et emplacements à la fois. */
    val pact = Build(
        name = "Le Pacte orageux",
        race = id("race.tiefling"),
        subrace = id("subrace.asmodeus-tiefling"),
        baseScores = Ability.entries.associateWith { 8 } + mapOf(Ability.CHARISMA to 15, Ability.CONSTITUTION to 15, Ability.DEXTERITY to 14),
        majorBonus = Ability.CHARISMA,
        minorBonus = Ability.CONSTITUTION,
        levels = listOf(BuildLevel(id("class.warlock"), id("subclass.the-fiend"))) + List(2) { BuildLevel(id("class.warlock")) } +
            BuildLevel(id("class.warlock"), feat = asi(Ability.CHARISMA to 2)) + BuildLevel(id("class.warlock")) +
            BuildLevel(id("class.sorcerer"), id("subclass.storm-sorcery")) + BuildLevel(id("class.sorcerer")),
    )
}
