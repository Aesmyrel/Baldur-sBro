package fr.carnetdefaerun.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import fr.carnetdefaerun.core.designsystem.shader.AmbientAnimationSlot
import fr.carnetdefaerun.core.designsystem.shader.LocalAmbientAnimation
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Thème choisi par le joueur. */
enum class ThemeMode { SOMBRE, NOIR_OLED, PARCHEMIN }

@Immutable
data class CarnetDimens(
    val gutter: Dp = 24.dp,
    val cardInset: Dp = 18.dp,
    val minTouch: Dp = 48.dp,
    /** Largeur maximale de la colonne de lecture (~38 em). */
    val readingMaxWidth: Dp = 640.dp,
    val radius: Dp = 4.dp,
)

val LocalCarnetColors = staticCompositionLocalOf { CarnetPalettes.Ink }
val LocalCarnetTypography = staticCompositionLocalOf { CarnetTypography.create() }
val LocalCarnetMotion = staticCompositionLocalOf { CarnetMotion(reduced = false) }
val LocalCarnetDimens = staticCompositionLocalOf { CarnetDimens() }
val LocalCarnetSection = staticCompositionLocalOf { CarnetSection.PARTIE }

/** Accès aux jetons : `CarnetTheme.colors`, `CarnetTheme.type`… */
object CarnetTheme {
    val colors: CarnetColors @Composable @ReadOnlyComposable get() = LocalCarnetColors.current
    val type: CarnetTypography @Composable @ReadOnlyComposable get() = LocalCarnetTypography.current
    val motion: CarnetMotion @Composable @ReadOnlyComposable get() = LocalCarnetMotion.current
    val dimens: CarnetDimens @Composable @ReadOnlyComposable get() = LocalCarnetDimens.current
    val section: CarnetSection @Composable @ReadOnlyComposable get() = LocalCarnetSection.current
    val accent @Composable @ReadOnlyComposable get() = colors.accent(section)
}

fun paletteFor(mode: ThemeMode): CarnetColors = when (mode) {
    ThemeMode.SOMBRE -> CarnetPalettes.Ink
    ThemeMode.NOIR_OLED -> CarnetPalettes.Oled
    ThemeMode.PARCHEMIN -> CarnetPalettes.Parchment
}

/**
 * Thème de l'appli. Material 3 reste le socle (accessibilité, composants) mais reçoit nos jetons ;
 * l'identité visuelle passe par [CarnetTheme] et les composants du design system.
 */
@Composable
fun CarnetTheme(
    mode: ThemeMode = ThemeMode.SOMBRE,
    readingScale: Float = 1f,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = paletteFor(mode)
    val type = remember(readingScale) { CarnetTypography.create(readingScale) }
    val motion = remember(reducedMotion) { CarnetMotion(reducedMotion) }
    CompositionLocalProvider(
        LocalCarnetColors provides colors,
        LocalCarnetTypography provides type,
        LocalCarnetMotion provides motion,
        LocalAmbientAnimation provides remember { AmbientAnimationSlot() },
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = type.toMaterial(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(3.dp),
                small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(6.dp),
                large = RoundedCornerShape(14.dp),
                extraLarge = RoundedCornerShape(22.dp),
            ),
            content = content,
        )
    }
}

/** Section courante : fixe la couleur d'accent des composants enfants. */
@Composable
fun CarnetSectionScope(section: CarnetSection, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCarnetSection provides section, content = content)
}

private fun CarnetColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = gold, onPrimary = onGold, primaryContainer = raised, onPrimaryContainer = text,
        secondary = lapis, onSecondary = bg, tertiary = verdigris, onTertiary = bg,
        background = bg, onBackground = text, surface = surface, onSurface = text,
        surfaceVariant = raised, onSurfaceVariant = text2, surfaceContainer = surface,
        surfaceContainerLow = surface, surfaceContainerHigh = raised, surfaceContainerHighest = raised,
        surfaceContainerLowest = bg, surfaceBright = raised, surfaceDim = bg,
        outline = line, outlineVariant = line, error = vermilion, onError = bg, scrim = scrim,
        inverseSurface = text, inverseOnSurface = bg, inversePrimary = goldDeep,
    )
}

private fun CarnetTypography.toMaterial() = Typography(
    displayLarge = display, displayMedium = display, displaySmall = headline,
    headlineLarge = headline, headlineMedium = headline, headlineSmall = title,
    titleLarge = title, titleMedium = titleSmall, titleSmall = dataStrong,
    bodyLarge = reading, bodyMedium = bodySmall, bodySmall = dataSmall,
    labelLarge = button, labelMedium = data, labelSmall = dataSmall,
)
