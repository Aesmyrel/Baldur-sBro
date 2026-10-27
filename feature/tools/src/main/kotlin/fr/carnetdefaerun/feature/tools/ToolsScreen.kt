package fr.carnetdefaerun.feature.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun ToolsRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.tools_crumb), stringResource(R.string.tools_title), onBack)
}
