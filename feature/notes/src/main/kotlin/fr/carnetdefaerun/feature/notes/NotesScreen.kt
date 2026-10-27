package fr.carnetdefaerun.feature.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.ui.InProgressScreen

/** Écran provisoire, remplacé à la phase prévue par docs/PLAN.md §9. */
@Composable
fun NotesRoute(onBack: (() -> Unit)? = null) {
    InProgressScreen(stringResource(R.string.notes_crumb), stringResource(R.string.notes_title), onBack)
}
