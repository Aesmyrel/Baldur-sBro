package fr.carnetdefaerun.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.designsystem.component.ActChoice
import fr.carnetdefaerun.core.designsystem.illustration.Vignette
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Tolerance

@get:StringRes
val Act.labelRes: Int
    get() = when (this) {
        Act.PROLOGUE -> R.string.act_prologue
        Act.ACT_1 -> R.string.act_1
        Act.ACT_2 -> R.string.act_2
        Act.ACT_3 -> R.string.act_3
        Act.EPILOGUE -> R.string.act_epilogue
    }

@get:StringRes
private val Act.shortRes: Int
    get() = when (this) {
        Act.PROLOGUE -> R.string.act_prologue_short
        Act.ACT_1 -> R.string.act_1_short
        Act.ACT_2 -> R.string.act_2_short
        Act.ACT_3 -> R.string.act_3_short
        Act.EPILOGUE -> R.string.act_epilogue_short
    }

val Act.vignette: Vignette
    get() = when (this) {
        Act.PROLOGUE -> Vignette.PROLOGUE
        Act.ACT_1 -> Vignette.ACTE_I
        Act.ACT_2 -> Vignette.ACTE_II
        Act.ACT_3 -> Vignette.ACTE_III
        Act.EPILOGUE -> Vignette.EPILOGUE
    }

@Composable
fun actChoices(): List<ActChoice> = Act.entries.map { ActChoice(stringResource(it.shortRes), stringResource(it.labelRes)) }

@get:StringRes
val Difficulty.labelRes: Int
    get() = when (this) {
        Difficulty.EXPLORER -> R.string.difficulty_explorer
        Difficulty.BALANCED -> R.string.difficulty_balanced
        Difficulty.TACTICIAN -> R.string.difficulty_tactician
        Difficulty.HONOUR -> R.string.difficulty_honour
        Difficulty.CUSTOM -> R.string.difficulty_custom
    }

@get:StringRes
val Tolerance.labelRes: Int
    get() = when (this) {
        Tolerance.NONE -> R.string.tolerance_none
        Tolerance.LIGHT -> R.string.tolerance_light
        Tolerance.FULL -> R.string.tolerance_full
    }
