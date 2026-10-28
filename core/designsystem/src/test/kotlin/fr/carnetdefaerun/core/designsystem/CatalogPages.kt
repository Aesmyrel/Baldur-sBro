package fr.carnetdefaerun.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.component.AbilityTile
import fr.carnetdefaerun.core.designsystem.component.ActChoice
import fr.carnetdefaerun.core.designsystem.component.ActSelector
import fr.carnetdefaerun.core.designsystem.component.AlertCard
import fr.carnetdefaerun.core.designsystem.component.CalcRow
import fr.carnetdefaerun.core.designsystem.component.CanonBadge
import fr.carnetdefaerun.core.designsystem.component.CanonKind
import fr.carnetdefaerun.core.designsystem.component.CarnetNavigationBar
import fr.carnetdefaerun.core.designsystem.component.CarnetSlider
import fr.carnetdefaerun.core.designsystem.component.CarnetSwitchRow
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.DropCapParagraph
import fr.carnetdefaerun.core.designsystem.component.FactCard
import fr.carnetdefaerun.core.designsystem.component.FloatingPill
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.LevelMarker
import fr.carnetdefaerun.core.designsystem.component.LevelPip
import fr.carnetdefaerun.core.designsystem.component.LevelTrack
import fr.carnetdefaerun.core.designsystem.component.Medallion
import fr.carnetdefaerun.core.designsystem.component.NavDestination
import fr.carnetdefaerun.core.designsystem.component.NavGlyph
import fr.carnetdefaerun.core.designsystem.component.ObjectiveRow
import fr.carnetdefaerun.core.designsystem.component.PartyMember
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.ReadingProgress
import fr.carnetdefaerun.core.designsystem.component.SearchField
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SegmentedChoice
import fr.carnetdefaerun.core.designsystem.component.SessionToggle
import fr.carnetdefaerun.core.designsystem.component.SpellSlots
import fr.carnetdefaerun.core.designsystem.component.SpoilerVeil
import fr.carnetdefaerun.core.designsystem.component.StatTile
import fr.carnetdefaerun.core.designsystem.component.Tag
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.illustration.ActVignette
import fr.carnetdefaerun.core.designsystem.illustration.ClassEmblem
import fr.carnetdefaerun.core.designsystem.illustration.Vignette
import fr.carnetdefaerun.core.designsystem.illustration.VignetteHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

// Pages du catalogue du design system, rendues par les tests de captures. Les textes sont des données de test.

private val Acts = listOf(ActChoice("Pr", "Prologue"), ActChoice("I", "Acte I"), ActChoice("II", "Acte II"), ActChoice("III", "Acte III"), ActChoice("Ép", "Épilogue"))
private val Destinations = listOf(
    NavDestination(NavGlyph.PARTIE, "Partie"), NavDestination(NavGlyph.GRIMOIRE, "Grimoire"), NavDestination(NavGlyph.CODEX, "Codex"),
    NavDestination(NavGlyph.BUILDS, "Builds"), NavDestination(NavGlyph.OUTILS, "Outils"),
)

@Composable
fun CatalogHome() {
    val c = CarnetTheme.colors
    var act by remember { mutableIntStateOf(1) }
    var done by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().grimoireBackground(c)) {
        VignetteHeader(Vignette.ACTE_I, "Ma partie · Équilibré", "Acte I", trailing = { SessionToggle("Session", true, {}) })
        ActSelector(Acts, act, { act = it }, Modifier.padding(top = 6.dp))
        AlertCard("Point de non-retour", "Avant de passer à l’Acte II, 6 éléments deviendront manquables.", "Voir la liste", {}, Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
        SectionHeader("Prochains objectifs", actionLabel = "Tout voir", onAction = {})
        Column(Modifier.padding(horizontal = 18.dp)) {
            ObjectiveRow("Chercher un remède au parasite", "Quête principale", done, { done = it }, tag = "Acte I", tagColor = c.gold)
            ObjectiveRow("Fouiller les ruines près de la route", "Lieu · 3 objets", true, {}, tag = "Lieu")
            ObjectiveRow("Récupérer le journal du camp", "Livre du jeu", false, {}, tag = "Manquable", tagColor = c.vermilion, showDivider = false)
        }
        SectionHeader("Groupe actif", actionLabel = "Modifier", onAction = {})
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceAround) {
            PartyMember("Lae’zel", "Guerrière", ClassEmblem.GUERRIER)
            PartyMember("Gale", "Magicien", ClassEmblem.MAGICIEN)
            PartyMember("Astarion", "Roublard", ClassEmblem.ROUBLARD)
            PartyMember("Karlach", "Barbare", ClassEmblem.BARBARE)
        }
        Spacer(Modifier.weight(1f))
        CarnetNavigationBar(Destinations, 0, {}, {}, "Rechercher")
    }
}

@Composable
fun CatalogReading() {
    val c = CarnetTheme.colors
    CarnetSectionScope(CarnetSection.CODEX) {
        Box(Modifier.fillMaxSize().grimoireBackground(c)) {
            Column {
                CarnetTopBar("Codex · Peuples", onBack = {})
                ReadingProgress(0.18f)
                ReadingColumn {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("Githyanki", style = CarnetTheme.type.display.copy(fontSize = CarnetTheme.type.display.fontSize * 0.96f), color = c.text)
                    Text("VO githyanki — peuple guerrier de la mer Astrale", style = CarnetTheme.type.readingItalic, color = c.text2)
                    TagRow(Modifier.padding(top = 10.dp)) {
                        CanonBadge("Canon D&D · 2e et 5e", CanonKind.DND)
                        CanonBadge("7 min de lecture", CanonKind.NEUTRE)
                    }
                }
                SectionHeader("Général")
                DropCapParagraph(
                    "Peuple guerrier de l’Astral, les githyanki descendent d’un peuple réduit en esclavage par les flagelleurs mentaux. Menés par Gith, leur libératrice, ils se sont soulevés et ont brisé l’empire illithid.",
                    Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SectionHeader("Dans BG3")
                Text(
                    "Lae’zel, guerrière githyanki, compte parmi les toutes premières rencontres de l’aventure ; d’autres githyanki croiseront votre route.",
                    style = CarnetTheme.type.reading, color = c.text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SectionHeader("Section masquée")
                SpoilerVeil({}, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }
            FloatingPill({}, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
                Text("Tirer le fil", style = CarnetTheme.type.dataStrong, color = c.text)
                Text("· Gith, githzerai, Astral…", style = CarnetTheme.type.dataStrong, color = c.gold)
            }
        }
    }
}

@Composable
fun CatalogBuild() {
    val c = CarnetTheme.colors
    CarnetSectionScope(CarnetSection.BUILDS) {
        Column(Modifier.fillMaxSize().grimoireBackground(c)) {
            CarnetTopBar("Builds · 3 sur 5", onBack = {})
            Text("L’Évocateur en harnois", style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = 24.dp))
            Text("Haut-elfe · niveau 12 · Guerrier 2 / Magicien 10", style = CarnetTheme.type.data, color = c.text2, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            LevelTrack(
                List(12) { i ->
                    val f = i < 2
                    LevelPip(if (f) ClassEmblem.GUERRIER else ClassEmblem.MAGICIEN, if (f) c.vermilion else c.lapis,
                        when (i) { 3 -> LevelMarker.SOUS_CLASSE; 5, 9 -> LevelMarker.DON; else -> null }, "Niveau ${i + 1}")
                },
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            SectionHeader("Caractéristiques", actionLabel = "Achat 27 / 27", onAction = {})
            Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Triple("For", 8, "−1"), Triple("Dex", 15, "+2"), Triple("Con", 16, "+3"), Triple("Int", 20, "+5"), Triple("Sag", 10, "+0"), Triple("Cha", 8, "−1")).forEach { (k, v, m) ->
                    AbilityTile(k, v, m, highlighted = k == "Int", a11y = "$k $v, modificateur $m", modifier = Modifier.weight(1f))
                }
            }
            SectionHeader("Statistiques dérivées", Modifier.padding(top = 8.dp))
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatTile("Points de vie", "92", true, {}, Modifier.weight(1f).fillMaxHeight()); StatTile("Classe d’armure", "19", false, {}, Modifier.weight(1f).fillMaxHeight()); StatTile("Maîtrise", "+4", false, {}, Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatTile("DD des sorts", "17", false, {}, Modifier.weight(1f).fillMaxHeight()); StatTile("Attaque des sorts", "+9", false, {}, Modifier.weight(1f).fillMaxHeight()); StatTile("Initiative", "+2", false, {}, Modifier.weight(1f).fillMaxHeight())
                }
            }
            SpellSlots(listOf("I" to 4, "II" to 3, "III" to 3, "IV" to 3, "V" to 2), "Emplacements", Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text("Détail du calcul · PV 92", style = CarnetTheme.type.title, color = c.text)
                CalcRow("Guerrier, niveau 1", "dé de vie maximal 10 + Constitution 3", "13")
                CalcRow("Guerrier, niveau 2", "gain fixe 6 + Constitution 3", "9")
                CalcRow("Magicien, niveaux 1 à 10", "10 × (gain fixe 4 + Constitution 3)", "70")
                CalcRow("Total", "", "92", total = true)
            }
        }
    }
}

@Composable
fun CatalogControls() {
    val c = CarnetTheme.colors
    var oled by remember { mutableStateOf(true) }
    var theme by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1.1f) }
    Column(
        Modifier.fillMaxSize().grimoireBackground(c).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader("Emblèmes des classes", Modifier.padding(top = 12.dp))
        LazyVerticalGrid(GridCells.Fixed(6), Modifier.padding(horizontal = 18.dp).height(140.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ClassEmblem.entries) { e -> Box(contentAlignment = Alignment.Center) { Medallion(e, size = 54.dp) } }
        }
        SectionHeader("Réglages")
        SegmentedChoice(listOf("Sombre", "Noir OLED", "Parchemin"), theme, { theme = it }, Modifier.padding(horizontal = 24.dp))
        CarnetSwitchRow("Écran toujours allumé", "Pendant une session de jeu", oled, { oled = it })
        SearchField("", {}, "Sort, objet, dieu, lieu…", Modifier.padding(horizontal = 24.dp))
        TagRow(Modifier.padding(horizontal = 24.dp)) {
            GoldButton("Exporter", {}); QuietButton("Importer", {})
        }
        TagRow(Modifier.padding(horizontal = 24.dp)) {
            Tag("Acte I", color = c.gold); Tag("Manquable", color = c.vermilion); CanonBadge("Canon BG3", CanonKind.BG3); CanonBadge("Interprétation", CanonKind.INTERPRETATION)
        }
        CarnetSlider(scale, { scale = it }, 0.9f..1.4f, steps = 4, valueLabel = "${(scale * 100).toInt()} %")
        FactCard("Le saviez-vous ?", "Imaginés par l’écrivain Charles Stross, les githyanki sont apparus en 1979 dans le magazine White Dwarf, puis dans le Fiend Folio en 1981.", Modifier.padding(horizontal = 18.dp))
    }
}

@Composable
fun CatalogVignettes() {
    Column(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) {
        Vignette.entries.forEach { ActVignette(it, Modifier.size(412.dp, 236.dp)) }
    }
}
