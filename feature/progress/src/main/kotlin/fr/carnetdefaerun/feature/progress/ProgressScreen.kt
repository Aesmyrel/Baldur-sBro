package fr.carnetdefaerun.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun ProgressRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.progress_crumb), stringResource(R.string.progress_title), onBack)
}
