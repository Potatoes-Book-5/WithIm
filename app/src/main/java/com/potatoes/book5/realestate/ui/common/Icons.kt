package com.potatoes.book5.realestate.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.potatoes.book5.realestate.ui.theme.wpx

/** 대장 아파트 표시용 왕관. 장식이므로 접근성 트리에서 뺀다(옆의 "대장" 글자가 의미를 전달). */
@Composable
fun CrownIcon(size: Dp = 44.wpx, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = size, height = size * 34f / 44f).clearAndSetSemantics {}) {
        val k = this.size.width / 44f
        val pts = listOf(0f to 34f, 0f to 8f, 12f to 20f, 22f to 2f, 32f to 20f, 44f to 8f, 44f to 34f)
        val path = Path().apply {
            pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * k, y * k) else lineTo(x * k, y * k) }
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun SpeakerIcon(size: Dp = 48.wpx, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size).clearAndSetSemantics {}) {
        val w = this.size.width
        val h = this.size.height
        val body = Path().apply {
            moveTo(0f, h * 0.35f); lineTo(w * 0.22f, h * 0.35f); lineTo(w * 0.48f, h * 0.12f)
            lineTo(w * 0.48f, h * 0.88f); lineTo(w * 0.22f, h * 0.65f); lineTo(0f, h * 0.65f); close()
        }
        drawPath(body, color)
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        drawArc(color, -45f, 90f, false, Offset(w * 0.42f, h * 0.3f), androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.4f), style = stroke)
        drawArc(color, -50f, 100f, false, Offset(w * 0.45f, h * 0.12f), androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.76f), style = stroke)
    }
}

/** 순위 번호 원. 1위는 강조색. */
@Composable
fun RankBadge(rank: Int, size: Dp = 60.wpx, leader: Boolean = rank == 1, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = if (leader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        contentColor = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = rank.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}
