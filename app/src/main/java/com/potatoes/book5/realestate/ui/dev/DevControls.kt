package com.potatoes.book5.realestate.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.theme.wpx

/**
 * 개발 빌드 전용. 주행 규제 연동(DI-001), 실제 위치 이동, 실거래가 API(T-005) 전까지
 * 주행 화면, 대장 교체 알림, 스크랩 변동을 확인하는 데 쓴다.
 * 릴리스 빌드에는 나타나지 않는다(AppRoot의 BuildConfig.DEBUG).
 */
@Composable
fun DevControls(
    driving: Boolean,
    onSetDriving: (Boolean) -> Unit,
    onEndDrive: () -> Unit,
    onLookupTrades: () -> Unit,
    tradeStatus: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(16.wpx),
        shape = RoundedCornerShape(20.wpx),
        color = Color(0xCC111318),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.wpx, vertical = 8.wpx)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.wpx),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.dev_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC94D))
            TextButton(onClick = { onSetDriving(!driving) }) {
                Text(
                    stringResource(if (driving) R.string.dev_park else R.string.dev_start_driving),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
            // 주차로 바꾸고 바로 세션을 닫아 요약을 만든다.
            TextButton(onClick = onEndDrive) {
                Text(stringResource(R.string.dev_end_drive), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            if (!driving) TextButton(onClick = onLookupTrades) {
                Text(stringResource(R.string.dev_lookup_yeongtong), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
        if (!driving) tradeStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White) }
        }
    }
}
