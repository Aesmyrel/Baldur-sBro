package fr.carnetdefaerun

import android.app.UiModeManager
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import fr.carnetdefaerun.core.designsystem.component.CarnetNavigationBar
import fr.carnetdefaerun.core.designsystem.component.CarnetNavigationRail
import fr.carnetdefaerun.core.designsystem.component.NavDestination
import fr.carnetdefaerun.core.designsystem.component.NavGlyph
import fr.carnetdefaerun.core.designsystem.draw.grimoireBackground
import fr.carnetdefaerun.core.designsystem.theme.CarnetMotion
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.designsystem.theme.rememberReducedMotion
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.TopLevel
import fr.carnetdefaerun.core.model.settings.AppTheme
import fr.carnetdefaerun.navigation.AppNavigator
import fr.carnetdefaerun.navigation.appEntries

@Composable
fun CarnetApp(navigator: AppNavigator = hiltViewModel(), appState: AppStateViewModel = hiltViewModel()) {
    val prefs by appState.preferences.collectAsStateWithLifecycle()
    val nav by navigator.state.collectAsStateWithLifecycle()
    val preferences = prefs ?: return
    val mode = when (preferences.theme) {
        AppTheme.DARK -> ThemeMode.SOMBRE
        AppTheme.OLED -> ThemeMode.NOIR_OLED
        AppTheme.PARCHMENT -> ThemeMode.PARCHEMIN
    }
    CarnetTheme(mode = mode, readingScale = preferences.readingScale, reducedMotion = rememberReducedMotion(preferences.reduceMotion)) {
        SystemBars(dark = mode != ThemeMode.PARCHEMIN)
        AppNightMode(dark = mode != ThemeMode.PARCHEMIN)
        KeepScreenOn(preferences.sessionActive)
        val state = nav ?: return@CarnetTheme
        // Largeur moyenne (≥ 600 dp : pliable ouvert, paysage) → rail de navigation.
        val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } >= 600.dp
        val destinations = listOf(
            NavDestination(NavGlyph.PARTIE, stringResource(R.string.nav_partie)),
            NavDestination(NavGlyph.GRIMOIRE, stringResource(R.string.nav_grimoire)),
            NavDestination(NavGlyph.CODEX, stringResource(R.string.nav_codex)),
            NavDestination(NavGlyph.BUILDS, stringResource(R.string.nav_builds)),
            NavDestination(NavGlyph.OUTILS, stringResource(R.string.nav_outils)),
        )
        val searchLabel = stringResource(R.string.nav_rechercher)
        val onSearch = { navigator.navigate(Destination.Search()) }
        val top = state.visible.last()
        // Barre masquée en lecture immersive et dans la recherche (qui a son propre champ).
        val showBar = top !is Destination.LoreEntry && top !is Destination.Search

        // À la racine d'un onglet autre que « Ma partie », le retour ramène à « Ma partie ».
        BackHandler(enabled = state.visible.size == 1 && state.current != TopLevel.PARTIE) { navigator.select(TopLevel.PARTIE) }

        val motion = CarnetTheme.motion
        Row(Modifier.fillMaxSize().grimoireBackground(CarnetTheme.colors)) {
            if (wide) CarnetNavigationRail(destinations, state.current.ordinal, { navigator.select(TopLevel.entries[it]) }, onSearch, searchLabel)
            Column(Modifier.weight(1f)) {
                Box(Modifier.weight(1f)) {
                    // Une liste d'entrées décorées par onglet : changer d'onglet ne retire rien de sa pile,
                    // donc ni l'état sauvegardable ni les ViewModels de l'onglet quitté ne sont détruits.
                    val decorators = listOf(rememberSaveableStateHolderNavEntryDecorator<Destination>(), rememberViewModelStoreNavEntryDecorator())
                    val entriesByTab = TopLevel.entries.associateWith { tab ->
                        key(tab) { rememberDecoratedNavEntries(state.stack(tab), decorators, appEntries(navigator, tab)) }
                    }
                    NavDisplay(
                        entries = entriesByTab.getValue(state.current),
                        onBack = { navigator.back() },
                        transitionSpec = { pageTransition(motion, forward = true) },
                        popTransitionSpec = { pageTransition(motion, forward = false) },
                        predictivePopTransitionSpec = { pageTransition(motion, forward = false) },
                    )
                }
                if (!wide && showBar) {
                    CarnetNavigationBar(destinations, state.current.ordinal, { navigator.select(TopLevel.entries[it]) }, onSearch, searchLabel)
                }
            }
        }
    }
}

/** Page qui glisse et s'efface ; simple fondu en mouvement réduit. */
private fun pageTransition(motion: CarnetMotion, forward: Boolean): ContentTransform {
    val spec = tween<Float>(motion.medium, easing = CarnetMotion.Page)
    if (motion.reduced) return fadeIn(tween(motion.short)) togetherWith fadeOut(tween(motion.short))
    val dir = if (forward) 1 else -1
    return (fadeIn(spec) + slideInHorizontally(tween(motion.medium, easing = CarnetMotion.Page)) { dir * it / 10 }) togetherWith
        (fadeOut(tween(motion.short)) + slideOutHorizontally(tween(motion.medium, easing = CarnetMotion.Page)) { -dir * it / 14 })
}

@Composable
private fun SystemBars(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    LaunchedEffect(dark) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

/** Mode session : l'écran reste allumé tant qu'il est actif (aucune permission nécessaire). */
@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Déclare au système le mode clair ou sombre choisi dans l'appli : au prochain lancement, l'écran
 * de démarrage prend déjà la bonne couleur (ressources `values` / `values-notnight`).
 */
@Composable
private fun AppNightMode(dark: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(dark) {
        val manager = context.getSystemService(UiModeManager::class.java) ?: return@LaunchedEffect
        // Sans effet visible si le mode effectif ne change pas ; mémorisé par le système pour l'appli.
        manager.setApplicationNightMode(if (dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO)
    }
}
