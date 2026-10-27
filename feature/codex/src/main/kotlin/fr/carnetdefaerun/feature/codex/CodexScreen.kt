package fr.carnetdefaerun.feature.codex

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun CodexRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.codex_crumb), stringResource(R.string.codex_title), onBack)
}
