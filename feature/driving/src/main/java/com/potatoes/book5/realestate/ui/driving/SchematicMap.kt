package com.potatoes.book5.realestate.ui.driving

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.potatoes.book5.realestate.feature.driving.R
import com.potatoes.book5.realestate.ui.common.CrownIcon
import com.potatoes.book5.realestate.ui.common.MapPointUi
import com.potatoes.book5.realestate.ui.common.RankBadge
import com.potatoes.book5.realestate.ui.common.RankItemUi
import com.potatoes.book5.realestate.ui.theme.wpx
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val MapGround = Color(0xFFEDEFF3)
private val MapRoad = Color.White
private val MapWater = Color(0xFFD6E4F5)
private val MapPark = Color(0xFFE3EDE3)
private val MapDot = Color(0xFFA9AFBA)

/**
 * 네이버 지도(ADR-004)를 쓸 수 없을 때의 간이 지도. 클라이언트 ID나 차량 위치가 없으면 [DrivingMap]이 이 지도를 그린다.
 * 진행 방향이 위쪽이고 차량은 아래쪽 3/4 지점에 고정한다. 배경 도로는 장식이며 실제 도로가 아니다.
 */
@Composable
fun SchematicMap(
    ranking: List<RankItemUi>,
    interestMatches: List<MapPointUi>,
    others: List<MapPointUi>,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.wpx))) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val vx = widthPx * 0.5f
        val vy = heightPx * 0.74f
        // 5km 원이 화면에 거의 들어오도록 축척을 정한다.
        val pxPerKm = min(widthPx * 0.5f, heightPx * 0.74f) / 5.5f

        fun position(km: Double, deg: Double): Offset {
            val rad = Math.toRadians(deg)
            return Offset(vx + (sin(rad) * km * pxPerKm).toFloat(), vy - (cos(rad) * km * pxPerKm).toFloat())
        }

        fun inside(p: Offset) = p.x in 0f..widthPx && p.y in 0f..heightPx

        Canvas(Modifier.fillMaxSize()) {
            drawRect(MapGround)
            val water = Path().apply {
                moveTo(-20f, size.height * 0.18f)
                cubicTo(size.width * 0.3f, size.height * 0.1f, size.width * 0.55f, size.height * 0.32f, size.width + 20f, size.height * 0.22f)
            }
            drawPath(water, MapWater, style = Stroke(width = 70.wpx.toPx()))
            drawRect(MapPark, Offset(size.width * 0.06f, size.height * 0.48f), androidx.compose.ui.geometry.Size(size.width * 0.18f, size.height * 0.16f))
            drawLine(MapRoad, Offset(vx, 0f), Offset(vx, size.height), strokeWidth = 64.wpx.toPx())
            listOf(0.3f, 0.58f, 0.86f).forEach { fy ->
                drawLine(MapRoad, Offset(0f, size.height * fy), Offset(size.width, size.height * fy - 30f), strokeWidth = 30.wpx.toPx())
            }
            drawLine(MapRoad, Offset(size.width * 0.78f, 0f), Offset(size.width * 0.7f, size.height), strokeWidth = 30.wpx.toPx())
            drawCircle(
                accent, radius = 2 * pxPerKm, center = Offset(vx, vy),
                style = Stroke(width = 4.wpx.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))),
            )
            drawCircle(sub.copy(alpha = 0.6f), radius = 5 * pxPerKm, center = Offset(vx, vy), style = Stroke(width = 4.wpx.toPx()))
            others.map { position(it.distanceKm, it.bearingDeg) }.filter(::inside).forEach {
                drawCircle(MapDot, radius = 12.wpx.toPx(), center = it)
            }
        }

        // 거리 원 라벨
        Label(text = "2km", color = accent, at = Offset(vx + 2 * pxPerKm * 0.71f + 12f, vy - 2 * pxPerKm * 0.71f - 40f))
        Label(text = "5km", color = sub, at = Offset(vx + 5 * pxPerKm * 0.71f + 12f, vy - 5 * pxPerKm * 0.71f - 40f))

        interestMatches.forEach { point ->
            val p = position(point.distanceKm, point.bearingDeg)
            if (inside(p)) Pin(at = p, size = 68.wpx) { InterestPin() }
        }
        ranking.forEach { item ->
            val p = position(item.distanceKm, item.bearingDeg)
            if (inside(p)) {
                Pin(at = p, size = 68.wpx) { RankBadge(rank = item.rank, size = 68.wpx) }
                if (item.rank == 1) Pin(at = p.copy(y = p.y - with(density) { 72.wpx.toPx() }), size = 44.wpx) { CrownIcon(size = 44.wpx) }
            }
        }
        Pin(at = Offset(vx, vy), size = 140.wpx) { VehicleMarker() }

        Text(
            text = stringResource(R.string.driving_heading_up),
            style = MaterialTheme.typography.bodySmall,
            color = sub,
            modifier = Modifier.align(Alignment.TopEnd).padding(24.wpx),
        )
        Text(
            text = stringResource(R.string.driving_map_note),
            style = MaterialTheme.typography.labelSmall,
            color = sub,
            modifier = Modifier.align(Alignment.BottomStart).padding(24.wpx),
        )
    }
}

@Composable
private fun Pin(at: Offset, size: Dp, content: @Composable () -> Unit) {
    val half = with(LocalDensity.current) { size.toPx() / 2 }
    Box(
        modifier = Modifier
            .offset { IntOffset((at.x - half).roundToInt(), (at.y - half).roundToInt()) }
            .size(size),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Label(text: String, color: Color, at: Offset) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) },
    )
}

@Composable
private fun InterestPin() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(3.wpx, MaterialTheme.colorScheme.primary),
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.wpx))
    }
}

@Composable
private fun VehicleMarker() {
    val accent = MaterialTheme.colorScheme.primary
    val ground = MaterialTheme.colorScheme.background
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(accent.copy(alpha = 0.15f), radius = size.minDimension / 2)
        drawCircle(accent, radius = size.minDimension * 0.32f)
        drawCircle(ground, radius = size.minDimension * 0.24f)
        val r = size.minDimension * 0.2f
        val arrow = Path().apply {
            moveTo(c.x, c.y - r * 0.9f)
            lineTo(c.x + r * 0.66f, c.y + r * 0.7f)
            lineTo(c.x, c.y + r * 0.35f)
            lineTo(c.x - r * 0.66f, c.y + r * 0.7f)
            close()
        }
        drawPath(arrow, accent)
    }
}
