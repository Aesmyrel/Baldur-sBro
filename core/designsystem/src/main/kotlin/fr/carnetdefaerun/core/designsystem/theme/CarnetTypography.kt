package fr.carnetdefaerun.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fr.carnetdefaerun.core.designsystem.R

/** Polices SIL OFL intégrées : titres Almendra, lecture EB Garamond, données Alegreya Sans. */
object CarnetFonts {
    val Almendra = FontFamily(
        Font(R.font.almendra_regular, FontWeight.Normal),
        Font(R.font.almendra_bold, FontWeight.Bold),
        Font(R.font.almendra_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val AlmendraSC = FontFamily(Font(R.font.almendra_sc_regular, FontWeight.Normal))
    val Garamond = FontFamily(
        Font(R.font.eb_garamond_regular, FontWeight.Normal),
        Font(R.font.eb_garamond_medium, FontWeight.Medium),
        Font(R.font.eb_garamond_semibold, FontWeight.SemiBold),
        Font(R.font.eb_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val AlegreyaSans = FontFamily(
        Font(R.font.alegreya_sans_regular, FontWeight.Normal),
        Font(R.font.alegreya_sans_medium, FontWeight.Medium),
        Font(R.font.alegreya_sans_bold, FontWeight.Bold),
    )
    val AlegreyaSansSC = FontFamily(
        Font(R.font.alegreya_sans_sc_medium, FontWeight.Medium),
        Font(R.font.alegreya_sans_sc_bold, FontWeight.Bold),
    )
}

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** Almendra : ligatures historiques désactivées (le « ct » lié gênait la lecture des titres). */
private const val TITLE_FEATURES = "liga 0, dlig 0, hlig 0, clig 0"

/**
 * Hiérarchie typographique. [readingScale] agrandit uniquement le texte de lecture (réglage « taille du texte »),
 * en plus de la taille de police du système, qui s'applique à tout.
 */
@Immutable
data class CarnetTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
    val reading: TextStyle,
    val readingItalic: TextStyle,
    val bodySmall: TextStyle,
    val data: TextStyle,
    val dataStrong: TextStyle,
    val dataSmall: TextStyle,
    val numeric: TextStyle,
    val numericLarge: TextStyle,
    val button: TextStyle,
) {
    companion object {
        fun create(readingScale: Float = 1f): CarnetTypography {
            fun r(size: Float): TextUnit = (size * readingScale).sp
            return CarnetTypography(
                display = TextStyle(fontFamily = CarnetFonts.Almendra, fontFeatureSettings = TITLE_FEATURES, fontSize = 50.sp, lineHeight = 52.sp, letterSpacing = (-0.005).em, lineHeightStyle = Trim),
                headline = TextStyle(fontFamily = CarnetFonts.Almendra, fontFeatureSettings = TITLE_FEATURES, fontSize = 34.sp, lineHeight = 38.sp, lineHeightStyle = Trim),
                title = TextStyle(fontFamily = CarnetFonts.Almendra, fontFeatureSettings = TITLE_FEATURES, fontSize = 24.sp, lineHeight = 28.sp, lineHeightStyle = Trim),
                titleSmall = TextStyle(fontFamily = CarnetFonts.Almendra, fontFeatureSettings = TITLE_FEATURES, fontSize = 20.sp, lineHeight = 24.sp, lineHeightStyle = Trim),
                label = TextStyle(fontFamily = CarnetFonts.AlegreyaSansSC, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, lineHeight = 14.sp, letterSpacing = 0.2.em),
                labelSmall = TextStyle(fontFamily = CarnetFonts.AlegreyaSansSC, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, lineHeight = 13.sp, letterSpacing = 0.12.em),
                reading = TextStyle(fontFamily = CarnetFonts.Garamond, fontSize = r(18.5f), lineHeight = r(27.5f), fontFeatureSettings = "kern, liga, onum"),
                readingItalic = TextStyle(fontFamily = CarnetFonts.Garamond, fontStyle = FontStyle.Italic, fontSize = r(17f), lineHeight = r(24f), fontFeatureSettings = "kern, liga, onum"),
                bodySmall = TextStyle(fontFamily = CarnetFonts.Garamond, fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = "kern, liga, onum"),
                data = TextStyle(fontFamily = CarnetFonts.AlegreyaSans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = "lnum"),
                dataStrong = TextStyle(fontFamily = CarnetFonts.AlegreyaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, fontFeatureSettings = "lnum"),
                dataSmall = TextStyle(fontFamily = CarnetFonts.AlegreyaSans, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 16.sp, fontFeatureSettings = "lnum"),
                numeric = TextStyle(fontFamily = CarnetFonts.AlegreyaSans, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 28.sp, fontFeatureSettings = "lnum, tnum"),
                numericLarge = TextStyle(fontFamily = CarnetFonts.AlegreyaSans, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 36.sp, fontFeatureSettings = "lnum, tnum"),
                button = TextStyle(fontFamily = CarnetFonts.AlegreyaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
            )
        }
    }
}
