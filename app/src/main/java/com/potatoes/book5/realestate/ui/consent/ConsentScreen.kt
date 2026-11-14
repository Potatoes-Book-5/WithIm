package com.potatoes.book5.realestate.ui.consent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.potatoes.book5.realestate.ui.common.MinTouchHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.common.LargeToggle
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.common.PillStyle
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-006 동의와 설정. 화면을 그리기만 한다. 와이어프레임: docs/design/wireframes/SCR-006_landscape_parked.svg */
@Composable
fun ConsentScreen(state: ConsentState, onIntent: (ConsentIntent) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Landscape 창(약 1700dp)에서는 소개 패널을 함께, Portrait 창(약 837dp)에서는 동의 영역만 보여 준다.
        val showIntro = maxWidth >= 1200.wpx
        Row(Modifier.fillMaxSize()) {
            if (showIntro) {
                IntroPanel(Modifier.width(700.wpx).fillMaxHeight())
            }
            if (state.editing) {
                SettingsPanel(state = state, onIntent = onIntent, modifier = Modifier.weight(1f).fillMaxHeight())
            } else {
                ConsentPanel(
                    state = state,
                    onIntent = onIntent,
                    showBrand = !showIntro,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
    if (state.confirmingDelete) {
        DeleteDataDialog(
            onConfirm = { onIntent(ConsentIntent.ConfirmDeleteData) },
            onDismiss = { onIntent(ConsentIntent.DismissDeleteData) },
        )
    }
}

/** SCR-006/settings. 홈의 설정에서 연다. 바꾼 값은 저장을 눌러야 반영된다. */
@Composable
private fun SettingsPanel(state: ConsentState, onIntent: (ConsentIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 72.wpx, end = 64.wpx, top = 56.wpx, bottom = 64.wpx)) {
        Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.wpx))
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 32.wpx),
        ) {
            // 저장된 값을 불러오는 동안은 비워 둔다.
            if (!state.isLoading) {
                ToggleRow(
                    label = stringResource(R.string.settings_location),
                    description = stringResource(R.string.consent_location_desc),
                    checked = state.locationConsented,
                    onCheckedChange = { onIntent(ConsentIntent.SetLocation(it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.consent_alert_interest),
                    checked = state.interestAlertEnabled,
                    onCheckedChange = { onIntent(ConsentIntent.SetInterestAlert(it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.consent_alert_leader),
                    checked = state.leaderAlertEnabled,
                    onCheckedChange = { onIntent(ConsentIntent.SetLeaderAlert(it)) },
                )
                Spacer(Modifier.height(24.wpx))
                HorizontalDivider(thickness = 2.wpx, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.wpx),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_data_title), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.wpx))
                        Text(
                            text = stringResource(R.string.settings_data_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(32.wpx))
                    PillButton(
                        text = stringResource(R.string.settings_data_delete),
                        onClick = { onIntent(ConsentIntent.RequestDeleteData) },
                        enabled = !state.isDeleting,
                        modifier = Modifier.width(280.wpx),
                    )
                }
            }
        }
        Spacer(Modifier.height(32.wpx))
        Row(horizontalArrangement = Arrangement.spacedBy(24.wpx)) {
            PillButton(
                text = stringResource(R.string.settings_cancel),
                onClick = { onIntent(ConsentIntent.Cancel) },
                enabled = !state.isSaving,
                modifier = Modifier.width(280.wpx),
            )
            PillButton(
                text = stringResource(R.string.settings_save),
                onClick = { onIntent(ConsentIntent.Save) },
                style = PillStyle.Primary,
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 되돌릴 수 없는 삭제이므로 한 번 더 확인한다. */
@Composable
private fun DeleteDataDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_delete_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = stringResource(R.string.settings_delete_message), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = MinTouchHeight)) {
                Text(text = stringResource(R.string.settings_delete_confirm), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchHeight)) {
                Text(text = stringResource(R.string.settings_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun IntroPanel(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ground = MaterialTheme.colorScheme.background
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 64.wpx, end = 64.wpx, top = 140.wpx, bottom = 64.wpx),
    ) {
        // 두 사람을 뜻하는 겹친 원(와이어프레임 장식)
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 420.wpx, height = 260.wpx)
                .clearAndSetSemantics {},
        ) {
            val r = 130.wpx.toPx()
            val cy = size.height / 2
            drawCircle(ground, r, Offset(size.width / 2 - 80.wpx.toPx(), cy))
            drawCircle(accent, r, Offset(size.width / 2 - 80.wpx.toPx(), cy), style = Stroke(3.wpx.toPx()))
            drawCircle(accent, r, Offset(size.width / 2 + 80.wpx.toPx(), cy), style = Stroke(3.wpx.toPx()))
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.consent_brand),
            style = MaterialTheme.typography.displayLarge,
            color = accent,
        )
        Spacer(Modifier.height(24.wpx))
        Text(
            text = stringResource(R.string.consent_tagline),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(140.wpx))
    }
}

@Composable
private fun ConsentPanel(
    state: ConsentState,
    onIntent: (ConsentIntent) -> Unit,
    showBrand: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(start = 72.wpx, end = 64.wpx, top = 56.wpx, bottom = 64.wpx)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (showBrand) {
                Text(
                    text = stringResource(R.string.consent_brand),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.wpx))
            }
            Text(text = stringResource(R.string.consent_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(48.wpx))
            ConsentItem(1, stringResource(R.string.consent_location_title), stringResource(R.string.consent_location_desc))
            ConsentItem(2, stringResource(R.string.consent_voice_title), stringResource(R.string.consent_voice_desc))
            ConsentItem(3, stringResource(R.string.consent_storage_title), stringResource(R.string.consent_storage_desc))
            Spacer(Modifier.height(32.wpx))
            ToggleRow(
                label = stringResource(R.string.consent_alert_interest),
                checked = state.interestAlertEnabled,
                onCheckedChange = { onIntent(ConsentIntent.SetInterestAlert(it)) },
            )
            ToggleRow(
                label = stringResource(R.string.consent_alert_leader),
                checked = state.leaderAlertEnabled,
                onCheckedChange = { onIntent(ConsentIntent.SetLeaderAlert(it)) },
            )
        }
        Spacer(Modifier.height(32.wpx))
        Row(horizontalArrangement = Arrangement.spacedBy(24.wpx)) {
            PillButton(
                text = stringResource(R.string.consent_later),
                onClick = { onIntent(ConsentIntent.Decline) },
                enabled = !state.isSaving,
                modifier = Modifier.width(280.wpx),
            )
            PillButton(
                text = stringResource(R.string.consent_agree),
                onClick = { onIntent(ConsentIntent.Agree) },
                style = PillStyle.Primary,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConsentItem(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier.padding(bottom = 40.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(88.wpx),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.width(32.wpx))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.wpx))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, description: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            if (description != null) {
                Spacer(Modifier.height(8.wpx))
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(32.wpx))
        LargeToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(widthDp = 1699, heightDp = 1140, showBackground = true)
@Composable
private fun ConsentScreenLandscapePreview() {
    KingOfRealEstateTheme(darkTheme = false) { ConsentScreen(state = ConsentState(), onIntent = {}) }
}

@Preview(widthDp = 1699, heightDp = 1140, showBackground = true)
@Composable
private fun SettingsLandscapePreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        ConsentScreen(state = ConsentState(editing = true, locationConsented = false), onIntent = {})
    }
}

@Preview(widthDp = 837, heightDp = 1140, showBackground = true)
@Composable
private fun ConsentScreenPortraitPreview() {
    KingOfRealEstateTheme(darkTheme = false) { ConsentScreen(state = ConsentState(), onIntent = {}) }
}
