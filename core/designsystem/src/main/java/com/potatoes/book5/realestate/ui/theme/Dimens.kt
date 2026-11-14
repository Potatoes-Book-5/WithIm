package com.potatoes.book5.realestate.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 와이어프레임은 2560×1440 물리 px로 그렸다. Pleos 에뮬레이터 v2.1.0에서 앱 창은 229dpi로 실행되므로
 * (AVD 설정값 160dpi와 다름, docs/design/foundations/layout.md) px를 dp로 바꿀 때 이 비율로 나눈다.
 * 덕분에 코드에는 와이어프레임 숫자를 그대로 쓰고, 실제 화면에서는 물리 크기가 유지된다.
 */
const val WIREFRAME_PX_PER_DP: Float = 229f / 160f

/** 와이어프레임 px → dp */
val Int.wpx: Dp get() = (this / WIREFRAME_PX_PER_DP).dp

/** 와이어프레임 px → sp */
val Int.wsp: TextUnit get() = (this / WIREFRAME_PX_PER_DP).sp
