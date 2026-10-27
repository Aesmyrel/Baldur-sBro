package fr.carnetdefaerun.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Jetons de couleur de la direction A « Grimoire enluminé » (docs/DIRECTION_ARTISTIQUE.md §2).
 * Tout jeton portant du texte atteint 4,5:1 sur chacun des fonds de son thème (bg, surface, raised, navBg).
 */
@Immutable
data class CarnetColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val navBg: Color,
    val line: Color,
    val text: Color,
    val text2: Color,
    val muted: Color,
    /** Or pour le texte, les chiffres clés et les filets. */
    val gold: Color,
    /** Or profond, réservé au décor (jamais au texte). */
    val goldDeep: Color,
    val lapis: Color,
    val vermilion: Color,
    val verdigris: Color,
    val tyrian: Color,
    val orpiment: Color,
    val sepia: Color,
    /** Texte posé sur la feuille d'or. */
    val onGold: Color,
    /** Fond des lettrines. */
    val lettrineGround: Color,
    val lettrineVine: Color,
    /** Halo de bougie (décor). */
    val candle: Color,
    val scrim: Color,
    val grainAlpha: Float,
) {
    /** Feuille d'or métallique : surfaces ≥ 24 dp uniquement (lettrines, médaillons, bouton de recherche). */
    val goldLeaf: Brush
        get() = Brush.linearGradient(
            0f to Color(0xFFF6E3A6),
            0.45f to Color(0xFFD8B260),
            0.72f to Color(0xFF9C7A2E),
            1f to Color(0xFFE9CF86),
        )

    fun accent(section: CarnetSection): Color = when (section) {
        CarnetSection.PARTIE -> gold
        CarnetSection.GRIMOIRE -> vermilion
        CarnetSection.CODEX -> lapis
        CarnetSection.BUILDS -> verdigris
        CarnetSection.COMPAGNONS -> tyrian
        CarnetSection.PROGRESSION -> orpiment
        CarnetSection.OUTILS -> sepia
    }
}

/** Sections de l'appli ; chacune a sa couleur d'accent (pigment médiéval). */
enum class CarnetSection { PARTIE, GRIMOIRE, CODEX, BUILDS, COMPAGNONS, PROGRESSION, OUTILS }

object CarnetPalettes {
    val Ink = CarnetColors(
        isDark = true,
        bg = Color(0xFF120F0C),
        surface = Color(0xFF1A1511),
        raised = Color(0xFF231C16),
        navBg = Color(0xFF15110D),
        line = Color(0xFF3B3026),
        text = Color(0xFFEEE4CD),
        text2 = Color(0xFFBCAD8F),
        muted = Color(0xFF978A72),
        gold = Color(0xFFD8B260),
        goldDeep = Color(0xFFA9843A),
        lapis = Color(0xFF7E9BE6),
        vermilion = Color(0xFFE66A47),
        verdigris = Color(0xFF5CB89D),
        tyrian = Color(0xFFCE72A6),
        orpiment = Color(0xFFE3B544),
        sepia = Color(0xFFC9A27A),
        onGold = Color(0xFF2A1D0C),
        lettrineGround = Color(0xFF1C2A52),
        lettrineVine = Color(0xFF7E9BE6),
        candle = Color(0x1AE2AA5C),
        scrim = Color(0x99000000),
        grainAlpha = 0.05f,
    )

    /** Noir pur pour OLED : le fond s'éteint, les surfaces s'éclaircissent légèrement pour garder la hiérarchie. */
    val Oled = Ink.copy(
        bg = Color(0xFF000000),
        surface = Color(0xFF0E0B09),
        raised = Color(0xFF18130F),
        navBg = Color(0xFF050403),
        line = Color(0xFF2E251D),
        candle = Color(0x10E2AA5C),
        grainAlpha = 0.03f,
    )

    val Parchment = CarnetColors(
        isDark = false,
        bg = Color(0xFFEFE5CC),
        surface = Color(0xFFF6EEDA),
        raised = Color(0xFFFBF6EA),
        navBg = Color(0xFFF4EBD3),
        line = Color(0xFFD9C9A4),
        text = Color(0xFF2B2016),
        text2 = Color(0xFF5B4A35),
        muted = Color(0xFF6F5E47),
        gold = Color(0xFF7A5814),
        goldDeep = Color(0xFFA9843A),
        lapis = Color(0xFF2F51A8),
        vermilion = Color(0xFFB03A1C),
        verdigris = Color(0xFF256A57),
        tyrian = Color(0xFF8E2E66),
        orpiment = Color(0xFF7D5C0C),
        sepia = Color(0xFF7A5230),
        onGold = Color(0xFF2A1D0C),
        lettrineGround = Color(0xFF2F51A8),
        lettrineVine = Color(0xFF9DB3F0),
        candle = Color(0x59FFECBE),
        scrim = Color(0x66000000),
        grainAlpha = 0.08f,
    )
}
