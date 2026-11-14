package com.potatoes.book5.realestate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

// 와이어프레임 px 값을 wsp로 적습니다. 실제 sp는 229dpi 기준으로 환산됩니다(Dimens.kt).
// 공식 글꼴 Asta Sans와 타입 스케일(T-002)이 확정되면 교체합니다.
private fun style(size: TextUnit, weight: FontWeight, lineHeightRatio: Float = 1.3f) =
    TextStyle(fontSize = size, fontWeight = weight, lineHeight = size * lineHeightRatio)

internal val AppTypography = Typography(
    displayLarge = style(96.wsp, FontWeight.ExtraBold, 1.15f),
    displayMedium = style(64.wsp, FontWeight.Bold, 1.2f),
    headlineLarge = style(56.wsp, FontWeight.Bold, 1.2f),
    headlineMedium = style(52.wsp, FontWeight.Bold, 1.2f),
    headlineSmall = style(48.wsp, FontWeight.SemiBold),
    titleLarge = style(44.wsp, FontWeight.Bold),
    titleMedium = style(40.wsp, FontWeight.Bold),
    titleSmall = style(36.wsp, FontWeight.SemiBold),
    bodyLarge = style(40.wsp, FontWeight.Normal),
    bodyMedium = style(34.wsp, FontWeight.Normal),
    bodySmall = style(30.wsp, FontWeight.Normal),
    labelLarge = style(40.wsp, FontWeight.SemiBold),
    labelMedium = style(32.wsp, FontWeight.SemiBold),
    labelSmall = style(28.wsp, FontWeight.SemiBold),
)
