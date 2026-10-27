package fr.carnetdefaerun.feature.encyclopedia

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun EncyclopediaRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.encyclopedia_crumb), stringResource(R.string.encyclopedia_title), onBack)
}
