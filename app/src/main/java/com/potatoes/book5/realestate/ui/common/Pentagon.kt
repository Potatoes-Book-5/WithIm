package com.potatoes.book5.realestate.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.potatoes.book5.realestate.ui.theme.wpx
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 축 값의 최대. 도메인 ConditionMatchPolicy.MAX_SCORE와 같다. */
private const val PENTAGON_MAX = 5f
private const val AXES = 5
private const val RINGS = 5

/**
 * 오각형 차트(SCR-010). 축 순서: 가격대(위), 평수, 동네, 세대수, 신축(시계 방향).
 * @param values 축별 0~5. null이면 눈금만 그린다.
 * @param showReference 모든 축이 최대인 기준 모양(내 조건)을 점선으로 그린다.
 * @param axisLabels 주면 꼭짓점 바깥에 축 이름을 쓴다.
 */
@Composable
fun PentagonChart(
    values: List<Float>?,
    radius: Dp,
    modifier: Modifier = Modifier,
    showReference: Boolean = false,
    fillAlpha: Float = 0.3f,
    strokeWidth: Dp = 4.wpx,
    axisLabels: List<String>? = null,
    labelGap: Dp = 24.wpx,
) {
    val colors = MaterialTheme.colorScheme
    val grid = colors.outline
    val reference = colors.onSurfaceVariant
    val accent = colors.primary
    val chart: @Composable () -> Unit = {
        Canvas(Modifier.size(radius * 2).clearAndSetSemantics {}) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2
            for (ring in 1..RINGS) {
                drawPath(polygon(c, List(AXES) { r * ring / RINGS }), grid, style = Stroke(2.wpx.toPx(), join = StrokeJoin.Round))
            }
            repeat(AXES) { i -> drawLine(grid, c, vertex(c, r, i), strokeWidth = 2.wpx.toPx()) }
            if (showReference) {
                drawPath(
                    polygon(c, List(AXES) { r }),
                    reference,
                    style = Stroke(3.wpx.toPx(), join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.wpx.toPx(), 8.wpx.toPx()))),
                )
            }
            if (values != null) {
                val shape = polygon(c, values.map { r * (it / PENTAGON_MAX).coerceIn(0f, 1f) })
                drawPath(shape, accent.copy(alpha = fillAlpha))
                drawPath(shape, accent, style = Stroke(strokeWidth.toPx(), join = StrokeJoin.Round))
            }
        }
    }
    if (axisLabels == null) {
        Layout(content = chart, modifier = modifier) { m, constraints ->
            val p = m.first().measure(Constraints())
            layout(p.width, p.height) { p.place(0, 0) }
        }
        return
    }
    Layout(
        modifier = modifier,
        content = {
            chart()
            axisLabels.forEach { Text(text = it, style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) }
        },
    ) { measurables, _ ->
        val chartP = measurables.first().measure(Constraints())
        val labels = measurables.drop(1).map { it.measure(Constraints()) }
        val gap = labelGap.toPx()
        val r = chartP.width / 2f
        // 라벨이 들어갈 여백을 사방에 둔다.
        val padX = (labels.maxOfOrNull { it.width } ?: 0) + gap
        val padY = (labels.maxOfOrNull { it.height } ?: 0) + gap
        val w = (chartP.width + padX * 2).roundToInt()
        val h = (chartP.height + padY * 2).roundToInt()
        val cx = w / 2f
        val cy = h / 2f
        layout(w, h) {
            chartP.place((cx - r).roundToInt(), (cy - r).roundToInt())
            labels.forEachIndexed { i, label ->
                val v = vertex(Offset(cx, cy), r + gap, i)
                val (x, y) = when (i) {
                    0 -> v.x - label.width / 2f to v.y - label.height
                    1 -> v.x to v.y - label.height / 2f
                    2 -> v.x - label.width / 4f to v.y
                    3 -> v.x - label.width * 3f / 4f to v.y
                    else -> v.x - label.width to v.y - label.height / 2f
                }
                label.place(x.roundToInt(), y.roundToInt())
            }
        }
    }
}

private fun vertex(c: Offset, r: Float, i: Int): Offset {
    val a = Math.toRadians(-90.0 + 360.0 / AXES * i)
    return Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
}

private fun DrawScope.polygon(c: Offset, radii: List<Float>): Path = Path().apply {
    radii.forEachIndexed { i, r ->
        val p = vertex(c, r, i)
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}
