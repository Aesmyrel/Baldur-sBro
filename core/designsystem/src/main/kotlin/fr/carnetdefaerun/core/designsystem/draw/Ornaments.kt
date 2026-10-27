package fr.carnetdefaerun.core.designsystem.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetColors
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import java.util.Random

/** Fleuron : losange de feuille d'or flanqué de deux filets. */
@Composable
fun Fleuron(modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Canvas(modifier.size(22.dp, 12.dp)) {
        val k = size.width / 22f
        val diamond = Path().apply {
            moveTo(11f * k, 1f * k); lineTo(15f * k, 6f * k); lineTo(11f * k, 11f * k); lineTo(7f * k, 6f * k); close()
        }
        drawPath(diamond, c.goldLeaf)
        drawLine(c.goldDeep, Offset(1f * k, 6f * k), Offset(6f * k, 6f * k), 0.9f * k)
        drawLine(c.goldDeep, Offset(16f * k, 6f * k), Offset(21f * k, 6f * k), 0.9f * k)
    }
}

/** Double filet : or profond au-dessus, trait d'ombre au-dessous. */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Canvas(modifier) {
        val y = size.height / 2f
        drawLine(c.goldDeep, Offset(0f, y - 1.2.dp.toPx()), Offset(size.width, y - 1.2.dp.toPx()), 0.8.dp.toPx())
        drawLine(c.line, Offset(0f, y + 1.2.dp.toPx()), Offset(size.width, y + 1.2.dp.toPx()), 0.8.dp.toPx())
    }
}

/** Cadre gravé : double filet intérieur et rinceaux d'angle. */
fun Modifier.engravedFrame(c: CarnetColors, inset: Float = 12f): Modifier = drawBehind {
    val i = inset.dp.toPx()
    val w = 0.8.dp.toPx()
    drawRect(c.goldDeep, topLeft = Offset(i, i), size = size.copy(size.width - 2 * i, size.height - 2 * i), alpha = 0.75f, style = Stroke(w))
    val j = i + 4.dp.toPx()
    drawRect(c.goldDeep, topLeft = Offset(j, j), size = size.copy(size.width - 2 * j, size.height - 2 * j), alpha = 0.45f, style = Stroke(w * 0.6f))
}

/**
 * Grain de vélin : texture de bruit générée une seule fois, posée en mosaïque à très faible opacité.
 * Aucun coût d'animation ; désactivable (thème OLED : grain plus léger).
 */
object VellumGrain {
    private const val SIZE = 128
    val bitmap: ImageBitmap by lazy {
        val pixels = IntArray(SIZE * SIZE)
        val r = Random(42)
        for (i in pixels.indices) {
            val v = r.nextInt(256)
            pixels[i] = android.graphics.Color.argb(255, v, v, v)
        }
        val bmp = android.graphics.Bitmap.createBitmap(pixels, SIZE, SIZE, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.asImageBitmap()
    }

    fun brush(): Brush = ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

/** Fond du grimoire : encre ou vélin, halo de bougie très doux, grain. */
fun Modifier.grimoireBackground(c: CarnetColors): Modifier = drawBehind {
    drawRect(c.bg)
    drawRect(
        Brush.radialGradient(
            0f to c.candle, 1f to c.candle.copy(alpha = 0f),
            center = Offset(size.width * 0.12f, size.height * 0.34f), radius = size.maxDimension * 0.75f,
        ),
    )
    drawRect(VellumGrain.brush(), alpha = c.grainAlpha)
}

