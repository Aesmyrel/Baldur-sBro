package fr.carnetdefaerun.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.draw.Hatch
import fr.carnetdefaerun.core.designsystem.draw.Hatches
import fr.carnetdefaerun.core.designsystem.draw.crossHatch
import fr.carnetdefaerun.core.designsystem.draw.engrave
import fr.carnetdefaerun.core.designsystem.draw.hatch
import fr.carnetdefaerun.core.designsystem.draw.polygon
import fr.carnetdefaerun.core.designsystem.draw.sparklePath
import fr.carnetdefaerun.core.designsystem.theme.CarnetColors
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import java.util.Random

/**
 * En-têtes gravés, un par moment de la partie. Ce sont des paysages génériques :
 * aucun ne représente un lieu, un personnage ou un événement de l'intrigue.
 */
enum class Vignette { PROLOGUE, ACTE_I, ACTE_II, ACTE_III, EPILOGUE }

private const val W = 412f
private const val H = 236f

/** Vignette gravée, enregistrée une seule fois par taille dans une couche graphique (aucun coût par image). */
@Composable
fun ActVignette(vignette: Vignette, modifier: Modifier = Modifier) {
    val colors = CarnetTheme.colors
    Canvas(
        modifier
            .fillMaxWidth()
            .height(236.dp)
            .drawWithCache {
                val layer = obtainGraphicsLayer()
                layer.record { drawVignette(vignette, colors) }
                onDrawWithContent { drawLayer(layer) }
            },
    ) {}
}

fun DrawScope.drawVignette(vignette: Vignette, c: CarnetColors) {
    val k = size.width / W
    clipRect(0f, 0f, size.width, size.height) {
    scale(k, k, pivot = Offset.Zero) {
        drawRect(c.bg, size = androidx.compose.ui.geometry.Size(W, H))
        when (vignette) {
            Vignette.PROLOGUE -> prologue(c)
            Vignette.ACTE_I -> acteI(c)
            Vignette.ACTE_II -> acteII(c)
            Vignette.ACTE_III -> acteIII(c)
            Vignette.EPILOGUE -> epilogue(c)
        }
        fadeAndFrame(c)
    }
    }
}

// ---------- Éléments communs ----------

private fun DrawScope.sky(c: CarnetColors, top: Float = 30f) {
    hatch(polygon(0f, top, W, top, W, H, 0f, H), c.gold, Hatches.Sky)
}

private fun DrawScope.halo(c: CarnetColors, x: Float, y: Float, r: Float) {
    drawCircle(
        Brush.radialGradient(0f to c.bg, 0.55f to c.bg.copy(alpha = 0.85f), 1f to c.bg.copy(alpha = 0f), center = Offset(x, y), radius = r),
        radius = r, center = Offset(x, y),
    )
}

private fun DrawScope.rings(c: CarnetColors, x: Float, y: Float, radii: List<Float>) {
    radii.forEach { r ->
        drawCircle(
            c.gold, radius = r, center = Offset(x, y), alpha = (0.55f - r / 140f).coerceAtLeast(0.08f),
            style = Stroke(0.45f, pathEffect = if (r > 40f) PathEffect.dashPathEffect(floatArrayOf(1.5f, 3f)) else null),
        )
    }
}

private fun DrawScope.crescent(c: CarnetColors, x: Float, y: Float, r: Float) {
    val moon = Path().apply { addOval(Rect(Offset(x, y), r)) }
    val cut = Path().apply { addOval(Rect(Offset(x + r * 0.42f, y - r * 0.3f), r * 0.87f)) }
    drawPath(Path.combine(PathOperation.Difference, moon, cut), c.goldLeaf)
}

private fun DrawScope.stars(c: CarnetColors, vararg s: Float) {
    var i = 0
    while (i < s.size) {
        drawPath(sparklePath(s[i], s[i + 1], s[i + 2]), c.gold, alpha = 0.9f)
        i += 3
    }
}

private fun DrawScope.cloud(c: CarnetColors, dx: Float, dy: Float, scale: Float = 1f) {
    fun X(v: Float) = dx + v * scale
    fun Y(v: Float) = dy + v * scale
    val p = Path().apply {
        moveTo(X(0f), Y(0f))
        quadraticTo(X(10f), Y(-12f), X(24f), Y(-6f))
        quadraticTo(X(32f), Y(-18f), X(46f), Y(-10f))
        quadraticTo(X(60f), Y(-14f), X(62f), Y(-2f))
        quadraticTo(X(74f), Y(0f), X(70f), Y(8f))
        lineTo(X(-4f), Y(8f))
        quadraticTo(X(-10f), Y(2f), X(0f), Y(0f))
        close()
    }
    drawPath(p, c.bg)
    drawPath(p, c.gold, alpha = 0.9f, style = Stroke(0.6f))
    drawLine(c.gold, Offset(X(14f), Y(8f - 4f)), Offset(X(58f), Y(8f - 4f)), 0.45f, alpha = 0.6f)
}

private fun DrawScope.conifers(c: CarnetColors, seed: Long, count: Int, x0: Float, step: Float, baseY: Float) {
    val r = Random(seed)
    repeat(count) { i ->
        val x = x0 + i * step + r.nextFloat() * 6f
        val base = baseY + kotlin.math.sin(i * 0.7f) * 6f + r.nextFloat() * 6f
        val th = 28f + r.nextFloat() * 22f
        val tw = 13f + r.nextFloat() * 7f
        val tree = Path()
        listOf(0f, 0.33f, 0.62f).forEach { t ->
            val y0 = base - th * t
            val y1 = base - th * (t + 0.48f)
            val ww = tw * (1 - t * 0.55f)
            tree.addPath(polygon(x - ww / 2, y0, x, y1, x + ww / 2, y0))
        }
        engrave(tree, c.bg, c.gold, if (i % 3 == 0) Hatches.Light else Hatches.Dense, outline = 0.7f)
        drawLine(c.gold, Offset(x, base), Offset(x, base + 6f), 0.8f)
    }
}

private fun DrawScope.fadeAndFrame(c: CarnetColors) {
    drawRect(
        Brush.verticalGradient(0.5f to c.bg.copy(alpha = 0f), 0.8f to c.bg.copy(alpha = 0.82f), 1f to c.bg, startY = 0f, endY = H),
        size = androidx.compose.ui.geometry.Size(W, H),
    )
    val outer = Rect(12.5f, 38.5f, W - 12.5f, H - 5.5f)
    drawRect(c.goldDeep, topLeft = outer.topLeft, size = outer.size, alpha = 0.75f, style = Stroke(0.8f))
    val inner = Rect(16.5f, 42.5f, W - 16.5f, H - 9.5f)
    drawRect(c.goldDeep, topLeft = inner.topLeft, size = inner.size, alpha = 0.45f, style = Stroke(0.5f))
    listOf(outer.topLeft to 1f, Offset(outer.right, outer.top) to -1f).forEach { (o, sx) ->
        val p = Path().apply {
            moveTo(o.x, o.y); quadraticTo(o.x + 12f * sx, o.y + 2f, o.x + 14f * sx, o.y + 14f)
            moveTo(o.x, o.y); quadraticTo(o.x + 2f * sx, o.y + 12f, o.x + 14f * sx, o.y + 14f)
            moveTo(o.x + 4f * sx, o.y + 4f); lineTo(o.x + 8f * sx, o.y + 8f)
        }
        drawPath(p, c.goldLeaf, style = Stroke(1f))
    }
}

// ---------- Prologue : nuit sur la mer, falaises, comète ----------

private fun DrawScope.prologue(c: CarnetColors) {
    sky(c)
    halo(c, 300f, 74f, 52f)
    // Comète : tête dorée, queue en traits fins.
    for (i in 0 until 9) {
        val o = i * 1.6f
        drawLine(c.gold, Offset(300f + o, 74f - o * 0.4f), Offset(372f + o * 2f, 46f - o), 0.5f, alpha = 0.75f - i * 0.07f)
    }
    drawCircle(c.goldLeaf, radius = 5.5f, center = Offset(300f, 74f))
    rings(c, 300f, 74f, listOf(12f, 20f))
    stars(c, 60f, 64f, 3.2f, 112f, 96f, 2.2f, 182f, 58f, 2.6f, 226f, 92f, 1.8f, 380f, 104f, 2.4f, 250f, 50f, 2f, 36f, 112f, 1.6f)
    cloud(c, 84f, 92f, 1.1f)
    cloud(c, 196f, 118f, 0.8f)
    // Mer : horizontales denses, vagues en petits arcs.
    val sea = polygon(0f, 152f, W, 152f, W, H, 0f, H)
    engrave(sea, c.bg, c.gold, Hatches.Water, outline = 0f)
    val r = Random(3)
    repeat(26) {
        val x = r.nextFloat() * W
        val y = 160f + r.nextFloat() * 70f
        val w = 8f + r.nextFloat() * 10f
        val arc = Path().apply { moveTo(x, y); quadraticTo(x + w / 2, y - 3f, x + w, y) }
        drawPath(arc, c.gold, alpha = 0.8f, style = Stroke(0.7f, cap = StrokeCap.Round))
    }
    drawLine(c.gold, Offset(0f, 152f), Offset(W, 152f), 0.8f)
    // Falaises.
    val cliff = polygon(0f, 118f, 26f, 110f, 48f, 126f, 70f, 124f, 92f, 146f, 112f, 176f, 0f, 176f)
    engrave(cliff, c.bg, c.gold, null, cross = true)
    val cliffRight = polygon(340f, 176f, 356f, 150f, 378f, 140f, 398f, 146f, W, 138f, W, 176f)
    engrave(cliffRight, c.bg, c.gold, Hatches.Dense)
}

// ---------- Acte I : forêt, rivière, ruine, croissant de lune ----------

private fun DrawScope.acteI(c: CarnetColors) {
    sky(c)
    halo(c, 324f, 78f, 64f)
    rings(c, 324f, 78f, listOf(28f, 36f, 46f, 58f))
    crescent(c, 324f, 78f, 19f)
    stars(c, 60f, 64f, 3.2f, 112f, 92f, 2.2f, 182f, 58f, 2.6f, 226f, 96f, 1.8f, 380f, 104f, 2.4f, 262f, 50f, 2f, 36f, 112f, 1.6f)
    cloud(c, 70f, 70f)
    val far = polygon(0f, 152f, 34f, 132f, 58f, 142f, 96f, 112f, 132f, 136f, 170f, 122f, 206f, 138f, 244f, 118f, 282f, 134f, 318f, 116f, 356f, 132f, 392f, 120f, W, 128f, W, H, 0f, H)
    engrave(far, c.bg, c.gold, Hatches.Sky.copy(alpha = 0.6f), outline = 0.7f)
    val hill = Path().apply { moveTo(200f, 184f); quadraticTo(258f, 146f, 330f, 152f); quadraticTo(382f, 157f, W, 170f); lineTo(W, H); lineTo(200f, H); close() }
    engrave(hill, c.bg, c.gold, Hatches.Light)
    // Ruine : arche brisée et pilier.
    val ruin = Path().apply {
        moveTo(280f, 154f); lineTo(280f, 116f); quadraticTo(280f, 99f, 298f, 97f); quadraticTo(306f, 97f, 311f, 101f)
        lineTo(306f, 107f); quadraticTo(300f, 105f, 296f, 107f); quadraticTo(289f, 110f, 289f, 118f); lineTo(289f, 154f); close()
        addPath(polygon(305f, 154f, 305f, 126f, 309f, 121f, 314f, 124f, 313f, 132f, 313f, 154f))
    }
    engrave(ruin, c.bg, c.gold, null, outline = 0.9f, cross = true)
    drawLine(c.gold, Offset(276f, 154f), Offset(318f, 154f), 0.7f)
    // Rivière.
    val river = Path().apply {
        moveTo(150f, 252f); cubicTo(170f, 232f, 200f, 226f, 214f, 214f); cubicTo(230f, 202f, 256f, 206f, 268f, 200f)
        lineTo(282f, 200f); cubicTo(268f, 210f, 246f, 214f, 232f, 224f); cubicTo(214f, 238f, 200f, 240f, 196f, 252f); close()
    }
    drawPath(river, c.bg)
    drawPath(river, c.gold, style = Stroke(0.7f))
    val r = Random(11)
    for (i in 0..25) {
        val t = i / 25f
        val cx = 150f + t * 120f + kotlin.math.sin(t * 6f) * 10f
        val cy = 248f - t * 44f
        val len = 22f - t * 14f
        drawLine(c.gold, Offset(cx - len / 2, cy), Offset(cx + len / 2, cy), 0.5f, alpha = 0.35f + r.nextFloat() * 0.4f)
    }
    conifers(c, seed = 11, count = 17, x0 = -6f, step = 13.5f, baseY = 196f)
    val ground = Path().apply { moveTo(0f, 214f); quadraticTo(54f, 196f, 112f, 214f); quadraticTo(150f, 226f, 172f, 252f); lineTo(0f, 252f); close() }
    engrave(ground, c.bg, c.gold, null, cross = true)
}

// ---------- Acte II : col de montagne, route sinueuse, brume, nouvelle lune ----------

private fun DrawScope.acteII(c: CarnetColors) {
    sky(c)
    stars(c, 48f, 60f, 2.6f, 96f, 84f, 1.8f, 150f, 52f, 3f, 214f, 74f, 2f, 270f, 56f, 2.4f, 330f, 88f, 1.8f, 372f, 60f, 3f, 120f, 112f, 1.6f, 300f, 112f, 1.6f)
    // Nouvelle lune : simple cercle gravé.
    halo(c, 206f, 70f, 30f)
    drawCircle(c.gold, radius = 12f, center = Offset(206f, 70f), style = Stroke(0.8f))
    rings(c, 206f, 70f, listOf(18f, 26f))
    val back = polygon(0f, 170f, 40f, 132f, 70f, 146f, 118f, 96f, 150f, 120f, 176f, 108f, 206f, 140f, 236f, 104f, 268f, 126f, 302f, 90f, 340f, 128f, 372f, 112f, W, 146f, W, H, 0f, H)
    engrave(back, c.bg, c.gold, Hatches.Light, outline = 0.8f)
    // Neige des sommets : zones laissées blanches.
    listOf(polygon(118f, 96f, 108f, 108f, 122f, 106f, 130f, 112f, 136f, 106f), polygon(302f, 90f, 292f, 102f, 304f, 100f, 312f, 106f, 318f, 99f), polygon(236f, 104f, 228f, 114f, 238f, 112f, 246f, 116f)).forEach {
        drawPath(it, c.bg); drawPath(it, c.gold, style = Stroke(0.6f))
    }
    // Bandes de brume.
    listOf(150f to 0.9f, 168f to 0.75f).forEach { (y, a) ->
        drawRect(c.bg.copy(alpha = a), topLeft = Offset(0f, y), size = androidx.compose.ui.geometry.Size(W, 8f))
        drawLine(c.gold, Offset(20f, y + 4f), Offset(150f, y + 4f), 0.45f, alpha = 0.5f)
        drawLine(c.gold, Offset(250f, y + 2f), Offset(392f, y + 2f), 0.45f, alpha = 0.5f)
    }
    val front = Path().apply { moveTo(0f, 196f); quadraticTo(110f, 170f, 206f, 186f); quadraticTo(310f, 200f, W, 178f); lineTo(W, H); lineTo(0f, H); close() }
    engrave(front, c.bg, c.gold, Hatches.Dense)
    // Route qui monte vers le col.
    val road = Path().apply {
        moveTo(150f, 252f); cubicTo(180f, 226f, 250f, 222f, 236f, 204f); cubicTo(226f, 192f, 196f, 196f, 204f, 184f)
        lineTo(214f, 184f); cubicTo(210f, 194f, 240f, 192f, 252f, 204f); cubicTo(268f, 222f, 214f, 232f, 196f, 252f); close()
    }
    drawPath(road, c.bg)
    drawPath(road, c.gold, style = Stroke(0.7f))
    drawPath(Path().apply { moveTo(172f, 246f); cubicTo(200f, 226f, 246f, 222f, 242f, 208f) }, c.gold, alpha = 0.7f,
        style = Stroke(0.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f))))
    // Arbre seul, sans feuilles.
    val tree = Path().apply {
        moveTo(350f, 200f); lineTo(352f, 158f); moveTo(351f, 176f); lineTo(338f, 162f); moveTo(351f, 170f); lineTo(366f, 154f)
        moveTo(352f, 162f); lineTo(346f, 148f); moveTo(359f, 162f); lineTo(370f, 166f)
    }
    drawPath(tree, c.gold, style = Stroke(1.2f, cap = StrokeCap.Round))
}

// ---------- Acte III : cité fortifiée sur l'estuaire, soleil couchant, navires ----------

private fun DrawScope.acteIII(c: CarnetColors) {
    sky(c)
    halo(c, 206f, 150f, 80f)
    for (i in 0 until 14) {
        val a = Math.toRadians((190 + i * 12).toDouble())
        drawLine(c.gold, Offset(206f, 150f), Offset(206f + 110f * kotlin.math.cos(a).toFloat(), 150f + 110f * kotlin.math.sin(a).toFloat()), 0.45f, alpha = 0.45f)
    }
    val sun = Path().apply { addOval(Rect(Offset(206f, 150f), 20f)) }
    drawPath(Path.combine(PathOperation.Intersect, sun, polygon(0f, 0f, W, 0f, W, 150f, 0f, 150f)), c.goldLeaf)
    stars(c, 48f, 62f, 2.4f, 104f, 86f, 1.6f, 330f, 58f, 2.6f, 372f, 90f, 1.8f)
    cloud(c, 60f, 96f, 0.9f)
    cloud(c, 300f, 84f, 1f)
    // Remparts et tours.
    val wall = Path().apply {
        moveTo(24f, 150f)
        var x = 24f
        while (x < 388f) { lineTo(x, 142f); lineTo(x + 4f, 142f); lineTo(x + 4f, 146f); lineTo(x + 8f, 146f); x += 8f }
        lineTo(388f, 150f); close()
    }
    engrave(wall, c.bg, c.gold, Hatches.Dense, outline = 0.6f)
    listOf(Triple(52f, 118f, 14f), Triple(108f, 110f, 12f), Triple(160f, 122f, 10f), Triple(252f, 120f, 10f), Triple(300f, 104f, 14f), Triple(356f, 116f, 12f)).forEach { (x, top, w) ->
        val tower = polygon(x, 150f, x, top, x + w, top, x + w, 150f)
        engrave(tower, c.bg, c.gold, null, outline = 0.7f, cross = true)
        val roof = polygon(x - 2f, top, x + w / 2, top - w * 1.2f, x + w + 2f, top)
        engrave(roof, c.bg, c.gold, Hatches.Light, outline = 0.7f)
    }
    // Pont sur l'estuaire.
    val bridge = Path().apply { moveTo(170f, 152f); lineTo(242f, 152f); lineTo(242f, 156f); quadraticTo(206f, 146f, 170f, 156f); close() }
    engrave(bridge, c.bg, c.gold, Hatches.Light, outline = 0.6f)
    // Eau et reflets.
    val water = polygon(0f, 156f, W, 156f, W, H, 0f, H)
    engrave(water, c.bg, c.gold, Hatches.Water, outline = 0f)
    for (i in 0 until 9) {
        val y = 162f + i * 7f
        val w = 34f - i * 3f
        drawLine(c.gold, Offset(206f - w, y), Offset(206f + w, y), 1.2f, alpha = 0.55f)
    }
    // Navires.
    listOf(86f to 186f, 322f to 196f).forEach { (x, y) ->
        val hull = Path().apply { moveTo(x - 16f, y); lineTo(x + 16f, y); lineTo(x + 11f, y + 6f); lineTo(x - 11f, y + 6f); close() }
        engrave(hull, c.bg, c.gold, null, outline = 0.7f, cross = true)
        drawLine(c.gold, Offset(x, y), Offset(x, y - 26f), 0.8f)
        val sail = polygon(x + 1f, y - 24f, x + 14f, y - 6f, x + 1f, y - 6f)
        engrave(sail, c.bg, c.gold, Hatches.Sky.copy(alpha = 0.9f), outline = 0.7f)
        val sail2 = polygon(x - 1f, y - 20f, x - 11f, y - 6f, x - 1f, y - 6f)
        engrave(sail2, c.bg, c.gold, Hatches.Sky.copy(alpha = 0.9f), outline = 0.7f)
    }
    val shore = Path().apply { moveTo(0f, 220f); quadraticTo(80f, 206f, 140f, 236f); lineTo(0f, 236f); close() }
    engrave(shore, c.bg, c.gold, null, cross = true)
}

// ---------- Épilogue : aube sur la mer calme, oiseaux ----------

private fun DrawScope.epilogue(c: CarnetColors) {
    sky(c)
    halo(c, 206f, 156f, 110f)
    for (i in 0 until 19) {
        val a = Math.toRadians((180 + i * 10).toDouble())
        val len = if (i % 2 == 0) 150f else 110f
        drawLine(c.gold, Offset(206f, 156f), Offset(206f + len * kotlin.math.cos(a).toFloat(), 156f + len * kotlin.math.sin(a).toFloat()), 0.5f, alpha = 0.5f)
    }
    val sun = Path().apply { addOval(Rect(Offset(206f, 156f), 26f)) }
    drawPath(Path.combine(PathOperation.Intersect, sun, polygon(0f, 0f, W, 0f, W, 156f, 0f, 156f)), c.goldLeaf)
    rings(c, 206f, 156f, listOf(34f, 44f))
    cloud(c, 70f, 92f, 1.1f)
    cloud(c, 290f, 76f, 0.9f)
    // Oiseaux.
    listOf(120f to 70f, 136f to 62f, 150f to 74f, 262f to 58f, 276f to 66f).forEach { (x, y) ->
        val bird = Path().apply { moveTo(x - 5f, y); quadraticTo(x - 2f, y - 3f, x, y); quadraticTo(x + 2f, y - 3f, x + 5f, y) }
        drawPath(bird, c.gold, style = Stroke(0.8f, cap = StrokeCap.Round))
    }
    val sea = polygon(0f, 156f, W, 156f, W, H, 0f, H)
    engrave(sea, c.bg, c.gold, Hatch(3.4f, 0f, 0.45f, 0.55f), outline = 0f)
    for (i in 0 until 10) {
        val y = 162f + i * 7f
        val w = 40f - i * 3.4f
        drawLine(c.gold, Offset(206f - w, y), Offset(206f + w, y), 1.3f, alpha = 0.6f)
    }
    drawLine(c.gold, Offset(0f, 156f), Offset(W, 156f), 0.8f)
    val left = Path().apply { moveTo(0f, 150f); quadraticTo(40f, 128f, 92f, 140f); quadraticTo(120f, 150f, 140f, 157f); lineTo(0f, 157f); close() }
    engrave(left, c.bg, c.gold, Hatches.Light)
    val right = Path().apply { moveTo(300f, 157f); quadraticTo(340f, 140f, 380f, 142f); quadraticTo(400f, 143f, W, 136f); lineTo(W, 157f); close() }
    engrave(right, c.bg, c.gold, Hatches.Dense)
    val shore = Path().apply { moveTo(W, 214f); quadraticTo(340f, 200f, 290f, 236f); lineTo(W, 236f); close() }
    engrave(shore, c.bg, c.gold, null, cross = true)
}

/**
 * En-tête d'écran : vignette gravée, libellé et grand titre posés en bas à gauche, avec une ombre
 * d'encre qui garantit la lisibilité au-dessus des hachures. [trailing] se place en bas à droite.
 */
@Composable
fun VignetteHeader(
    vignette: Vignette,
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val c = CarnetTheme.colors
    val shadow = androidx.compose.ui.graphics.Shadow(c.bg, Offset.Zero, blurRadius = 14f)
    androidx.compose.foundation.layout.Box(modifier.fillMaxWidth()) {
        ActVignette(vignette)
        androidx.compose.foundation.layout.Row(
            Modifier.align(androidx.compose.ui.Alignment.BottomStart).fillMaxWidth().padding(start = 24.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Bottom,
        ) {
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                androidx.compose.material3.Text(label, style = CarnetTheme.type.label.copy(shadow = shadow), color = c.gold)
                androidx.compose.material3.Text(title, style = CarnetTheme.type.display.copy(shadow = shadow), color = c.text)
            }
            trailing()
        }
    }
}
