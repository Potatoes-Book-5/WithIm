package com.potatoes.book5.realestate.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.potatoes.book5.realestate.ui.theme.wpx

/** 기본 스낵바는 차량 화면에서 글자가 작아 와이어프레임 크기로 직접 그린다. */
@Composable
fun LargeSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Surface(
            shape = RoundedCornerShape(36.wpx),
            color = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 48.wpx, vertical = 28.wpx),
            )
        }
    }
}
