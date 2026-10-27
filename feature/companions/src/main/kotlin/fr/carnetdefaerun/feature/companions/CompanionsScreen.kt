package fr.carnetdefaerun.feature.companions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun CompanionsRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.companions_crumb), stringResource(R.string.companions_title), onBack)
}
