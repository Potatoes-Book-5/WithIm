package com.potatoes.book5.realestate.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.potatoes.book5.realestate.ui.theme.wpx

/** 터치 영역 최소 높이(팀 제안값, docs/design/foundations/layout.md). */
val MinTouchHeight: Dp = 120.wpx

/** 화면이 백그라운드로 가면 상태 수집을 멈춘다(SM-006). 모든 Route가 이 함수를 쓴다. */
@Composable
fun <T> StateFlow<T>.collectAsStateWithAppLifecycle(): State<T> = collectAsStateWithLifecycle()

enum class PillStyle { Primary, Secondary, Ghost }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Secondary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content, border) = when (style) {
        PillStyle.Primary -> Triple(colors.primary, colors.onPrimary, null)
        PillStyle.Secondary -> Triple(colors.background, colors.onSurface, BorderStroke(3.wpx, colors.outline))
        PillStyle.Ghost -> Triple(Color.Transparent, colors.onSurfaceVariant, null)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MinTouchHeight),
        shape = CircleShape,
        color = if (enabled) container else colors.surfaceVariant,
        contentColor = if (enabled) content else colors.onSurfaceVariant,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 48.wpx),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(16.wpx))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .heightIn(min = MinTouchHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.Checkbox),
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else colors.background,
        contentColor = if (selected) colors.primary else colors.onSurface,
        border = BorderStroke(3.wpx, if (selected) colors.primary else colors.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 40.wpx),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(40.wpx))
                Spacer(Modifier.width(12.wpx))
            }
            Text(
                text = label,
                style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** 와이어프레임 크기(128×72)의 토글. Material Switch는 이 화면 밀도에서 너무 작습니다. */
@Composable
fun LargeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(width = 128.wpx, height = 72.wpx)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
    ) {
        Surface(
            modifier = Modifier.size(width = 128.wpx, height = 72.wpx),
            shape = RoundedCornerShape(36.wpx),
            color = if (checked) colors.primary else colors.outline,
        ) {}
        Surface(
            modifier = Modifier
                .padding(8.wpx)
                .offset(x = if (checked) 56.wpx else 0.wpx)
                .size(56.wpx),
            shape = CircleShape,
            color = Color.White,
        ) {}
    }
}

/** 기본 아이콘 세트에 마이크가 없어 간단히 그립니다. */
@Composable
fun MicIcon(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier = modifier.size(40.wpx)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.1f)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.32f, h * 0.02f),
            size = Size(w * 0.36f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.18f),
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.25f),
            size = Size(w * 0.7f, h * 0.55f),
            style = stroke,
        )
        drawLine(color, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.5f, h * 0.98f), strokeWidth = w * 0.1f)
    }
}
