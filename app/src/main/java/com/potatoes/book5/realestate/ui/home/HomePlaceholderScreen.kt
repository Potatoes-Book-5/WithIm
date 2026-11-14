package com.potatoes.book5.realestate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-001 홈을 구현하기 전까지 온보딩 이후에 보여 주는 임시 화면. */
@Composable
fun HomePlaceholderScreen(onEditConditions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(64.wpx),
        verticalArrangement = Arrangement.spacedBy(32.wpx),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PillButton(text = stringResource(R.string.home_edit_conditions), onClick = onEditConditions)
    }
}
