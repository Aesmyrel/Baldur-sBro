package fr.carnetdefaerun.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Crédits et licences : mentions obligatoires, sources, licences des données, des polices et du code. */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val c = CarnetTheme.colors
    val context = LocalContext.current
    val fonts = remember { readFontCopyrights(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CarnetTopBar(stringResource(R.string.credits_crumb), onBack = onBack)
        Text(stringResource(R.string.credits_title), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = 24.dp))
        Block(R.string.credits_notice_title, stringResource(R.string.credits_notice))
        Block(R.string.credits_wotc_title, stringResource(R.string.credits_wotc_vo))
        Paragraph(stringResource(R.string.credits_wotc_vf))
        Paragraph(stringResource(R.string.credits_marks))
        Block(R.string.credits_data_title, stringResource(R.string.credits_data))
        SectionHeader(stringResource(R.string.credits_fonts_title))
        Paragraph(stringResource(R.string.credits_fonts_intro))
        fonts.forEach { Text(it, style = CarnetTheme.type.dataSmall, color = c.text2, modifier = Modifier.padding(horizontal = 24.dp, vertical = 3.dp)) }
        Block(R.string.credits_code_title, stringResource(R.string.credits_code))
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Block(title: Int, body: String) {
    SectionHeader(stringResource(title))
    Paragraph(body)
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = CarnetTheme.type.bodySmall, color = CarnetTheme.colors.text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
}

/** Première ligne de copyright de chaque licence OFL embarquée (aucun nom écrit à la main). */
private fun readFontCopyrights(context: Context): List<String> {
    val dir = "licences/polices"
    return context.assets.list(dir).orEmpty().sorted().mapNotNull { file ->
        context.assets.open("$dir/$file").bufferedReader().useLines { lines -> lines.firstOrNull { it.startsWith("Copyright") } }
    }
}
