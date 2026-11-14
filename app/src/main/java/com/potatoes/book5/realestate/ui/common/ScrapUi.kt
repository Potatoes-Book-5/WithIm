package com.potatoes.book5.realestate.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.theme.wpx
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 스크랩 목록 배지 문구(SCR-007): 새 실거래, 가격 ▲ 2,000만 … */
@Composable
fun scrapBadgeText(kind: ScrapChangeKind, deltaManwon: Long): String = when (kind) {
    ScrapChangeKind.NEW_DEAL -> stringResource(R.string.scrap_badge_new_deal)
    ScrapChangeKind.PRICE_UP -> stringResource(R.string.scrap_badge_price_up, formatAmount(abs(deltaManwon)))
    ScrapChangeKind.PRICE_DOWN -> stringResource(R.string.scrap_badge_price_down, formatAmount(abs(deltaManwon)))
    ScrapChangeKind.RANK_CHANGE -> stringResource(R.string.scrap_badge_rank_change)
}

/** 변동 배지. 가격 상승은 빨강, 나머지는 강조색. 색만으로 구분하지 않도록 문구에 ▲▼를 넣는다. */
@Composable
fun ChangeBadge(text: String, kind: ScrapChangeKind, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val up = kind == ScrapChangeKind.PRICE_UP
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (up) colors.errorContainer else colors.primaryContainer,
        contentColor = if (up) colors.error else colors.primary,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 28.wpx, vertical = 12.wpx),
        )
    }
}

/** 앱 바 뒤로 버튼(120×120 원). */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.back)
    val ink = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier.size(MinTouchHeight).semantics { contentDescription = description },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(52.wpx)) {
                val w = size.width
                val path = Path().apply {
                    moveTo(w * 0.7f, 0f); lineTo(w * 0.2f, w * 0.5f); lineTo(w * 0.7f, w)
                }
                drawPath(path, ink, style = Stroke(width = 6.wpx.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** 스크랩 별. 스크랩됨이면 채운다. 장식이므로 옆 글자가 의미를 전달한다. */
@Composable
fun StarIcon(filled: Boolean, size: Dp = 44.wpx, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).clearAndSetSemantics {}) {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val outer = this.size.minDimension / 2
        val inner = outer * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.toRadians(-90.0 + i * 36.0)
            val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color, style = if (filled) Fill else Stroke(width = 3.wpx.toPx(), join = StrokeJoin.Round))
    }
}
