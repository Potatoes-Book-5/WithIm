package com.potatoes.book5.realestate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx

/**
 * 온보딩(동의, 관심 조건)을 마치기 전에 주행이 시작되면 보여 준다(T-035).
 * 앱이 DO를 선언해 플랫폼이 화면을 가리지 않으므로, 주행 중 입력 화면 진입을 앱이 막는다(F-007, DR-002).
 * 조작 요소 없이 한 줄 안내만 둔다(DD 글랜스).
 */
@Composable
fun ParkFirstScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.wpx),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.park_first_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun ParkFirstScreenPreview() {
    KingOfRealEstateTheme { ParkFirstScreen() }
}
