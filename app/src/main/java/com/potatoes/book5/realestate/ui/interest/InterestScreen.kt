package com.potatoes.book5.realestate.ui.interest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.ui.common.MicIcon
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.common.PillStyle
import com.potatoes.book5.realestate.ui.common.SelectChip
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-003 관심 조건. 와이어프레임: docs/design/wireframes/SCR-003_landscape_parked.svg */
@Composable
fun InterestScreen(state: InterestState, onIntent: (InterestIntent) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Landscape 창에서는 조건 이름을 왼쪽 열에, Portrait 창에서는 칩 위에 둔다.
        val wide = maxWidth >= 1200.wpx
        Column(Modifier.fillMaxSize().padding(horizontal = 48.wpx)) {
            Header(onSkip = { onIntent(InterestIntent.Skip) }, skipEnabled = !state.isSaving)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.wpx),
            ) {
                ConditionGroup(
                    title = stringResource(R.string.interest_neighborhood),
                    hint = stringResource(
                        if (state.suggestionsFailed) R.string.interest_suggestions_failed else R.string.interest_neighborhood_hint
                    ),
                    wide = wide,
                ) {
                    ChipFlow {
                        state.neighborhoodChips.forEach { n ->
                            SelectChip(
                                label = n.name,
                                selected = n in state.selectedNeighborhoods,
                                onClick = { onIntent(InterestIntent.ToggleNeighborhood(n)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.wpx))
                    PillButton(
                        text = stringResource(R.string.interest_voice),
                        onClick = { onIntent(InterestIntent.VoiceSearch) },
                        leading = { MicIcon() },
                    )
                }
                Divider()
                ConditionGroup(
                    title = stringResource(R.string.interest_price),
                    hint = stringResource(R.string.interest_multi_hint),
                    wide = wide,
                ) {
                    ChipFlow {
                        PriceRange.entries.forEach { range ->
                            SelectChip(
                                label = stringResource(range.labelRes()),
                                selected = range in state.selectedPrices,
                                onClick = { onIntent(InterestIntent.TogglePrice(range)) },
                            )
                        }
                    }
                }
                Divider()
                ConditionGroup(
                    title = stringResource(R.string.interest_area),
                    hint = stringResource(R.string.interest_multi_hint),
                    wide = wide,
                ) {
                    ChipFlow {
                        AreaRange.entries.forEach { range ->
                            SelectChip(
                                label = stringResource(range.labelRes()),
                                selected = range in state.selectedAreas,
                                onClick = { onIntent(InterestIntent.ToggleArea(range)) },
                            )
                        }
                    }
                }
            }
            Footer(state = state, onSave = { onIntent(InterestIntent.Save) })
        }
    }
}

@Composable
private fun Header(onSkip: () -> Unit, skipEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.wpx),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(R.string.interest_title), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.wpx))
            Text(
                text = stringResource(R.string.interest_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PillButton(
            text = stringResource(R.string.interest_skip),
            onClick = onSkip,
            style = PillStyle.Ghost,
            enabled = skipEnabled,
        )
    }
}

@Composable
private fun ConditionGroup(
    title: String,
    hint: String,
    wide: Boolean,
    content: @Composable () -> Unit,
) {
    val label: @Composable () -> Unit = {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.wpx))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (wide) {
        Row(Modifier.padding(vertical = 32.wpx)) {
            Column(Modifier.width(300.wpx).padding(top = 20.wpx)) { label() }
            Column(Modifier.weight(1f)) { content() }
        }
    } else {
        Column(Modifier.padding(vertical = 32.wpx)) {
            label()
            Spacer(Modifier.height(24.wpx))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(24.wpx),
        verticalArrangement = Arrangement.spacedBy(24.wpx),
    ) { content() }
}

@Composable
private fun Divider() {
    HorizontalDivider(thickness = 2.wpx, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun Footer(state: InterestState, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summaryText(state),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(32.wpx))
        PillButton(
            text = stringResource(R.string.interest_save),
            onClick = onSave,
            style = PillStyle.Primary,
            enabled = state.canSave,
            modifier = Modifier.width(480.wpx),
        )
    }
}

/** 예: "정자동, 수내동 · 10~15억 · 30평대" */
@Composable
private fun summaryText(state: InterestState): String {
    if (!state.hasSelection) return stringResource(R.string.interest_summary_empty)
    val parts = buildList {
        if (state.selectedNeighborhoods.isNotEmpty()) add(state.selectedNeighborhoods.joinToString(", ") { it.name })
        if (state.selectedPrices.isNotEmpty()) {
            add(PriceRange.entries.filter { it in state.selectedPrices }.map { stringResource(it.labelRes()) }.joinToString(", "))
        }
        if (state.selectedAreas.isNotEmpty()) {
            add(AreaRange.entries.filter { it in state.selectedAreas }.map { stringResource(it.labelRes()) }.joinToString(", "))
        }
    }
    return parts.joinToString(" · ")
}

private fun PriceRange.labelRes(): Int = when (this) {
    PriceRange.UNDER_5 -> R.string.price_under_5
    PriceRange.FROM_5_TO_10 -> R.string.price_5_10
    PriceRange.FROM_10_TO_15 -> R.string.price_10_15
    PriceRange.FROM_15_TO_20 -> R.string.price_15_20
    PriceRange.OVER_20 -> R.string.price_over_20
}

private fun AreaRange.labelRes(): Int = when (this) {
    AreaRange.UNDER_20 -> R.string.area_under_20
    AreaRange.PY_20S -> R.string.area_20s
    AreaRange.PY_30S -> R.string.area_30s
    AreaRange.OVER_40 -> R.string.area_over_40
}

private val previewState = InterestState(
    isLoading = false,
    suggestions = listOf("정자동", "수내동", "서현동", "판교동", "이매동").map { NeighborhoodUi("sample-$it", it) },
    selectedNeighborhoods = setOf(NeighborhoodUi("sample-정자동", "정자동"), NeighborhoodUi("sample-수내동", "수내동")),
    selectedPrices = setOf(PriceRange.FROM_10_TO_15),
    selectedAreas = setOf(AreaRange.PY_30S),
)

@Preview(widthDp = 1699, heightDp = 1140, showBackground = true)
@Composable
private fun InterestScreenLandscapePreview() {
    KingOfRealEstateTheme(darkTheme = false) { InterestScreen(state = previewState, onIntent = {}) }
}

@Preview(widthDp = 837, heightDp = 1140, showBackground = true)
@Composable
private fun InterestScreenPortraitPreview() {
    KingOfRealEstateTheme(darkTheme = false) { InterestScreen(state = previewState, onIntent = {}) }
}
