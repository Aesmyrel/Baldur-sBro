package fr.carnetdefaerun.navigation

import androidx.navigation3.runtime.NavEntry
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.TopLevel
import fr.carnetdefaerun.core.ui.entryTypeOf
import fr.carnetdefaerun.core.ui.loreThemeOf
import fr.carnetdefaerun.feature.builds.BuildEditorRoute
import fr.carnetdefaerun.feature.builds.BuildsRoute
import fr.carnetdefaerun.feature.builds.PartyRoute
import fr.carnetdefaerun.feature.codex.CodexRoute
import fr.carnetdefaerun.feature.codex.LoreEntryRoute
import fr.carnetdefaerun.feature.codex.ThemeRoute
import fr.carnetdefaerun.feature.companions.CompanionsRoute
import fr.carnetdefaerun.feature.encyclopedia.CategoryRoute
import fr.carnetdefaerun.feature.encyclopedia.EncyclopediaRoute
import fr.carnetdefaerun.feature.encyclopedia.EntryRoute
import fr.carnetdefaerun.feature.home.HomeRoute
import fr.carnetdefaerun.feature.notes.NotesRoute
import fr.carnetdefaerun.feature.progress.ProgressRoute
import fr.carnetdefaerun.feature.search.SearchRoute
import fr.carnetdefaerun.feature.settings.CreditsScreen
import fr.carnetdefaerun.feature.settings.SettingsRoute
import fr.carnetdefaerun.feature.tools.DiceRoute
import fr.carnetdefaerun.feature.tools.ProbabilityRoute
import fr.carnetdefaerun.feature.tools.ToolsRoute

/**
 * Associe chaque destination à son écran. Les modules d'écrans ne se connaissent pas entre eux.
 * La clé d'état est préfixée par l'onglet : un même écran ouvert dans deux onglets garde deux états.
 */
fun appEntries(navigator: AppNavigator, tab: TopLevel): (Destination) -> NavEntry<Destination> = { destination ->
    val back = { navigator.back(); Unit }
    NavEntry(destination, contentKey = "${tab.name}/$destination") {
        when (destination) {
            Destination.Home -> HomeRoute(onNavigate = navigator::navigate)
            Destination.Encyclopedia -> EncyclopediaRoute(onNavigate = navigator::navigate)
            is Destination.EncyclopediaCategory -> entryTypeOf(destination.category)
                ?.let { CategoryRoute(it, navigator::navigate, back) } ?: EncyclopediaRoute(onNavigate = navigator::navigate)
            is Destination.GameEntry -> EntryRoute(ContentId(destination.id), navigator::navigate, back)
            Destination.Codex -> CodexRoute(onNavigate = navigator::navigate)
            is Destination.CodexTheme -> loreThemeOf(destination.theme)
                ?.let { ThemeRoute(it, navigator::navigate, back) } ?: CodexRoute(onNavigate = navigator::navigate)
            is Destination.LoreEntry -> LoreEntryRoute(ContentId(destination.id), destination.anchor, navigator::navigate, back)
            Destination.Builds -> BuildsRoute(onNavigate = navigator::navigate)
            is Destination.BuildEditor -> BuildEditorRoute(destination.buildId, back)
            Destination.Party -> PartyRoute(back)
            Destination.Tools -> ToolsRoute(onNavigate = navigator::navigate)
            Destination.Probability -> ProbabilityRoute(back)
            Destination.Dice -> DiceRoute(back)
            Destination.Settings -> SettingsRoute(onNavigate = navigator::navigate, onBack = back)
            Destination.Credits -> CreditsScreen(onBack = back)
            Destination.Companions, is Destination.CompanionDetail -> CompanionsRoute(onBack = back)
            is Destination.Progress -> ProgressRoute(onBack = back)
            Destination.Notes, is Destination.NoteEditor -> NotesRoute(onBack = back)
            is Destination.Search -> SearchRoute(destination.query, navigator::navigate, back)
            else -> CodexRoute(onNavigate = navigator::navigate)
        }
    }
}
