package fr.carnetdefaerun.core.designsystem.illustration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import fr.carnetdefaerun.core.designsystem.draw.polygon
import fr.carnetdefaerun.core.designsystem.draw.sparklePath

/**
 * Emblèmes de classe originaux, gravés au trait dans un carré de 32 × 32.
 * Ils symbolisent un archétype ; aucun ne reproduit une icône du jeu ou d'un livre.
 */
enum class ClassEmblem { BARBARE, BARDE, CLERC, DRUIDE, ENSORCELEUR, GUERRIER, MAGICIEN, MOINE, OCCULTISTE, PALADIN, RODEUR, ROUBLARD }

private const val E = 32f

fun DrawScope.drawClassEmblem(emblem: ClassEmblem, color: Color, strokeWidth: Float = 1.25f, box: Float = size.minDimension) {
    val k = box / E
    val stroke = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fine = Stroke(strokeWidth * 0.5f, cap = StrokeCap.Round)
    scale(k, k, pivot = Offset.Zero) {
        when (emblem) {
            ClassEmblem.BARBARE -> {
                drawLine(color, Offset(16f, 5f), Offset(16f, 29f), strokeWidth * 1.3f, StrokeCap.Round)
                val blades = Path().apply {
                    moveTo(16f, 8f); quadraticTo(8f, 6f, 6f, 12f); quadraticTo(8f, 18f, 16f, 15f)
                    moveTo(16f, 8f); quadraticTo(24f, 6f, 26f, 12f); quadraticTo(24f, 18f, 16f, 15f)
                }
                drawPath(blades, color, style = stroke)
            }
            ClassEmblem.BARDE -> {
                val lyre = Path().apply {
                    moveTo(10f, 7f); quadraticTo(6f, 16f, 11f, 24f); lineTo(21f, 24f); quadraticTo(26f, 16f, 22f, 7f)
                    moveTo(9f, 7f); lineTo(23f, 7f)
                }
                drawPath(lyre, color, style = stroke)
                listOf(13.5f, 16f, 18.5f).forEach { x -> drawLine(color, Offset(x, 7f), Offset(x, 24f), strokeWidth * 0.5f) }
                drawCircle(color, 1.4f, Offset(16f, 27.5f))
            }
            ClassEmblem.CLERC -> {
                drawCircle(color, 6f, Offset(16f, 16f), style = stroke)
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0)
                    val r0 = 8.5f
                    val r1 = if (i % 2 == 0) 13.5f else 11f
                    drawLine(color, Offset(16f + r0 * kotlin.math.cos(a).toFloat(), 16f + r0 * kotlin.math.sin(a).toFloat()),
                        Offset(16f + r1 * kotlin.math.cos(a).toFloat(), 16f + r1 * kotlin.math.sin(a).toFloat()), strokeWidth * 0.8f, StrokeCap.Round)
                }
                drawPath(sparklePath(16f, 16f, 3f), color)
            }
            ClassEmblem.DRUIDE -> {
                drawPath(Path().apply { moveTo(16f, 29f); quadraticTo(15f, 18f, 16f, 6f) }, color, style = stroke)
                listOf(Triple(16f, 9f, -1f), Triple(16f, 15f, 1f), Triple(16f, 21f, -1f)).forEach { (x, y, s) ->
                    val leaf = Path().apply { moveTo(x, y); quadraticTo(x + 7f * s, y - 6f, x + 10f * s, y - 1f); quadraticTo(x + 6f * s, y + 4f, x, y) }
                    drawPath(leaf, color, style = stroke)
                    drawLine(color, Offset(x, y), Offset(x + 8f * s, y - 1.5f), strokeWidth * 0.45f)
                }
            }
            ClassEmblem.ENSORCELEUR -> {
                val flame = Path().apply {
                    moveTo(16f, 28f); quadraticTo(7f, 26f, 8f, 18f); quadraticTo(9f, 12f, 14f, 6f)
                    quadraticTo(14f, 12f, 17f, 13f); quadraticTo(20f, 9f, 19f, 4f); quadraticTo(26f, 11f, 24f, 20f); quadraticTo(23f, 27f, 16f, 28f)
                }
                drawPath(flame, color, style = stroke)
                val inner = Path().apply { moveTo(16f, 25f); quadraticTo(12f, 23f, 13f, 19f); quadraticTo(15f, 21f, 17f, 17f); quadraticTo(20f, 21f, 16f, 25f) }
                drawPath(inner, color, style = fine)
            }
            ClassEmblem.GUERRIER -> {
                drawPath(polygon(16f, 3f, 18f, 5f, 18f, 21f, 16f, 23f, 14f, 21f, 14f, 5f), color, style = stroke)
                drawLine(color, Offset(9f, 20f), Offset(23f, 20f), strokeWidth * 1.15f, StrokeCap.Round)
                drawLine(color, Offset(16f, 23f), Offset(16f, 28f), strokeWidth * 1.15f, StrokeCap.Round)
                drawLine(color, Offset(13.5f, 29.5f), Offset(18.5f, 29.5f), strokeWidth * 1.15f, StrokeCap.Round)
                drawLine(color, Offset(16f, 7f), Offset(16f, 19f), strokeWidth * 0.5f)
            }
            ClassEmblem.MAGICIEN -> {
                val book = Path().apply {
                    moveTo(5f, 23f); quadraticTo(11f, 20f, 16f, 23f); quadraticTo(21f, 20f, 27f, 23f)
                    lineTo(27f, 27f); quadraticTo(21f, 24f, 16f, 27f); quadraticTo(11f, 24f, 5f, 27f); close()
                }
                drawPath(book, color, style = stroke)
                drawLine(color, Offset(16f, 23f), Offset(16f, 27f), strokeWidth * 0.8f)
                drawPath(sparklePath(16f, 11f, 7f), color)
            }
            ClassEmblem.MOINE -> {
                drawArc(color, 20f, 300f, false, topLeft = Offset(5f, 5f), size = androidx.compose.ui.geometry.Size(22f, 22f), style = Stroke(strokeWidth * 1.4f, cap = StrokeCap.Round))
                drawCircle(color, 2.2f, Offset(16f, 16f))
            }
            ClassEmblem.OCCULTISTE -> {
                val eye = Path().apply { moveTo(4f, 16f); quadraticTo(16f, 5f, 28f, 16f); quadraticTo(16f, 27f, 4f, 16f) }
                drawPath(eye, color, style = stroke)
                drawCircle(color, 4.2f, Offset(16f, 16f), style = stroke)
                drawCircle(color, 1.6f, Offset(16f, 16f))
                drawPath(Path().apply { moveTo(12f, 4f); quadraticTo(16f, 1f, 20f, 4f) }, color, style = fine)
                drawPath(Path().apply { moveTo(12f, 28f); quadraticTo(16f, 31f, 20f, 28f) }, color, style = fine)
            }
            ClassEmblem.PALADIN -> {
                val shield = Path().apply { moveTo(16f, 3f); lineTo(26f, 6.5f); lineTo(26f, 14f); quadraticTo(26f, 23f, 16f, 29f); quadraticTo(6f, 23f, 6f, 14f); lineTo(6f, 6.5f); close() }
                drawPath(shield, color, style = stroke)
                drawPath(sparklePath(16f, 15f, 6.5f), color, style = fine)
                drawCircle(color, 1.6f, Offset(16f, 15f))
            }
            ClassEmblem.RODEUR -> {
                drawPath(Path().apply { moveTo(9f, 4f); quadraticTo(23f, 16f, 9f, 28f) }, color, style = stroke)
                drawLine(color, Offset(9f, 4f), Offset(9f, 28f), strokeWidth * 0.5f)
                drawLine(color, Offset(5f, 16f), Offset(27f, 16f), strokeWidth, StrokeCap.Round)
                drawPath(polygon(27f, 16f, 23f, 13.5f, 24f, 16f, 23f, 18.5f), color)
                drawLine(color, Offset(5f, 16f), Offset(3f, 14f), strokeWidth * 0.7f, StrokeCap.Round)
                drawLine(color, Offset(5f, 16f), Offset(3f, 18f), strokeWidth * 0.7f, StrokeCap.Round)
            }
            ClassEmblem.ROUBLARD -> {
                drawPath(polygon(8f, 25f, 20f, 9f, 24f, 6f, 23f, 10f, 11f, 26f), color, style = stroke)
                drawLine(color, Offset(7f, 21f), Offset(13f, 27f), strokeWidth * 1.15f, StrokeCap.Round)
                drawLine(color, Offset(6f, 28f), Offset(9f, 25f), strokeWidth * 1.15f, StrokeCap.Round)
                drawLine(color, Offset(12f, 22f), Offset(21f, 10f), strokeWidth * 0.5f)
                // Clé discrète.
                drawCircle(color, 2.4f, Offset(24.5f, 22.5f), style = fine)
                drawLine(color, Offset(22.8f, 24.2f), Offset(19f, 28f), strokeWidth * 0.6f, StrokeCap.Round)
            }
        }
    }
}

/** Disque doré gravé pour les cases à cocher « sceau » (le rendu animé est dans le composant). */
fun DrawScope.drawSealRing(ring: Color, inner: Color, strokeWidth: Float) {
    val r = size.minDimension / 2f
    drawCircle(ring, r - strokeWidth, style = Stroke(strokeWidth))
    drawCircle(inner, r * 0.72f, style = Stroke(strokeWidth * 0.8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1.2f * r / 15f, 2f * r / 15f))))
}
