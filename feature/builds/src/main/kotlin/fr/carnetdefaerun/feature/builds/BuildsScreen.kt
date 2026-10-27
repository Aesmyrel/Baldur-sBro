package fr.carnetdefaerun.feature.builds

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun BuildsRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.builds_crumb), stringResource(R.string.builds_title), onBack)
}
