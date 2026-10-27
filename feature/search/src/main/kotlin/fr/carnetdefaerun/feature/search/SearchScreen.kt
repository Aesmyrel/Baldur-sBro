package fr.carnetdefaerun.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun SearchRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.search_crumb), stringResource(R.string.search_title), onBack)
}
