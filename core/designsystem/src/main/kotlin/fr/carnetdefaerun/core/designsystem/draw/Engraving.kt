package fr.carnetdefaerun.core.designsystem.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.hypot

/**
 * Gravure au trait : la valeur d'une surface vient de la densité de ses hachures
 * (ciel en horizontales, reliefs en diagonales, ombres en croisillons), jamais d'un aplat.
 */
data class Hatch(val spacing: Float, val angle: Float, val width: Float, val alpha: Float = 1f)

object Hatches {
    val Sky = Hatch(spacing = 3.4f, angle = 0f, width = 0.42f, alpha = 0.5f)
    val Light = Hatch(spacing = 3f, angle = 38f, width = 0.55f, alpha = 0.85f)
    val Dense = Hatch(spacing = 2.1f, angle = 38f, width = 0.6f)
    val Water = Hatch(spacing = 2.8f, angle = 0f, width = 0.5f, alpha = 0.7f)
}

/** Remplit [path] de traits parallèles. Les coordonnées sont celles du dessin, l'épaisseur est mise à l'échelle par [unit]. */
fun DrawScope.hatch(path: Path, color: Color, hatch: Hatch, unit: Float = 1f) {
    val bounds = path.getBounds()
    if (bounds.isEmpty) return
    val spacing = hatch.spacing * unit
    val center = bounds.center
    val radius = hypot(bounds.width, bounds.height) / 2f + spacing
    clipPath(path) {
        rotate(hatch.angle, pivot = center) {
            var y = center.y - radius
            while (y <= center.y + radius) {
                drawLine(
                    color = color,
                    start = Offset(center.x - radius, y),
                    end = Offset(center.x + radius, y),
                    strokeWidth = hatch.width * unit,
                    alpha = hatch.alpha,
                )
                y += spacing
            }
        }
    }
}

/** Croisillons : deux réseaux croisés, pour les ombres. */
fun DrawScope.crossHatch(path: Path, color: Color, spacing: Float = 2.6f, angle: Float = 28f, width: Float = 0.55f, unit: Float = 1f) {
    hatch(path, color, Hatch(spacing, angle, width, 0.9f), unit)
    hatch(path, color, Hatch(spacing, angle + 90f, width, 0.9f), unit)
}

/**
 * Grave une forme : on l'occulte d'abord avec le fond (pour masquer ce qui est derrière),
 * puis on la hachure et on trace son contour.
 */
fun DrawScope.engrave(
    path: Path,
    ground: Color,
    ink: Color,
    hatch: Hatch?,
    outline: Float = 0.8f,
    cross: Boolean = false,
    unit: Float = 1f,
) {
    drawPath(path, ground)
    if (cross) crossHatch(path, ink, unit = unit) else if (hatch != null) hatch(path, ink, hatch, unit)
    if (outline > 0f) {
        drawPath(path, ink, style = Stroke(width = outline * unit, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

/** Étoile à quatre branches (scintillement gravé). */
fun sparklePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    val k = r * 0.22f
    moveTo(cx, cy - r)
    quadraticTo(cx + k, cy - k, cx + r, cy)
    quadraticTo(cx + k, cy + k, cx, cy + r)
    quadraticTo(cx - k, cy + k, cx - r, cy)
    quadraticTo(cx - k, cy - k, cx, cy - r)
    close()
}

/** Construit un chemin à partir de points (polygone fermé). */
fun polygon(vararg points: Float): Path = Path().apply {
    moveTo(points[0], points[1])
    var i = 2
    while (i < points.size) {
        lineTo(points[i], points[i + 1])
        i += 2
    }
    close()
}
