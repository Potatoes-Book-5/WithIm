package com.potatoes.book5.realestate.ui.mypage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.BuildingAge
import com.potatoes.book5.realestate.domain.model.HouseholdMin
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.common.BackButton
import com.potatoes.book5.realestate.ui.common.ChangeBadge
import com.potatoes.book5.realestate.ui.common.PentagonChart
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.common.labelRes
import com.potatoes.book5.realestate.ui.common.scrapBadgeText
import com.potatoes.book5.realestate.ui.common.shortLabelRes
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx

private const val TOP_COUNT = 3

/** SCR-010 마이 페이지(시안 C). 와이어프레임: docs/design/wireframes/SCR-010_landscape_parked.svg */
@Composable
fun MyPageScreen(state: MyPageState, onIntent: (MyPageIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.wpx),
    ) {
        AppBar(onBack = { onIntent(MyPageIntent.Back) })
        if (state.isLoading) return@Column
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 36.wpx, bottom = 48.wpx),
            horizontalArrangement = Arrangement.spacedBy(40.wpx),
        ) {
            ConditionPanel(
                condition = state.condition,
                onEdit = { onIntent(MyPageIntent.EditConditions) },
                modifier = Modifier.width(560.wpx).fillMaxHeight(),
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = stringResource(R.string.my_page_top_title, TOP_COUNT),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(24.wpx))
                when {
                    state.condition == null -> Placeholder(stringResource(R.string.my_page_needs_condition))
                    state.cards.isEmpty() -> Placeholder(stringResource(R.string.my_page_no_scraps))
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(24.wpx),
                        ) {
                            state.cards.forEach { card ->
                                MatchCard(card, onClick = { onIntent(MyPageIntent.OpenApartment(card.id)) }, modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                            // 3곳보다 적어도 카드 너비를 유지한다.
                            repeat(TOP_COUNT - state.cards.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(24.wpx))
                        Text(
                            text = stringResource(R.string.my_page_legend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(32.wpx))
                ScrapsTile(state.scrapCount, state.changedCount, onClick = { onIntent(MyPageIntent.OpenScraps) })
            }
        }
    }
}

@Composable
private fun AppBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(32.wpx))
        Column {
            Text(text = stringResource(R.string.my_page_title), style = MaterialTheme.typography.headlineLarge)
            Text(
                text = stringResource(R.string.my_page_subtitle),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConditionPanel(condition: InterestCondition?, onEdit: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(40.wpx)) {
            Text(text = stringResource(R.string.my_page_conditions), style = MaterialTheme.typography.titleMedium)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                PentagonChart(
                    values = if (condition != null) List(5) { 5f } else null,
                    radius = 170.wpx,
                    fillAlpha = 0.2f,
                    strokeWidth = 5.wpx,
                    axisLabels = listOf(
                        stringResource(R.string.axis_price),
                        stringResource(R.string.axis_area),
                        stringResource(R.string.axis_neighborhood),
                        stringResource(R.string.axis_households),
                        stringResource(R.string.axis_newness),
                    ),
                )
            }
            val sub = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            if (condition == null) {
                Text(text = stringResource(R.string.my_page_no_conditions), style = sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                conditionLines(condition).forEach { line ->
                    Text(text = line, style = sub, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(48.wpx))
            PillButton(
                text = stringResource(if (condition == null) R.string.my_page_set_conditions else R.string.my_page_edit),
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 예: "정자동, 수내동 · 10~15억", "30평대 · 500세대+ · 준공 10년 이내" */
@Composable
private fun conditionLines(condition: InterestCondition): List<String> {
    val first = buildList {
        if (condition.neighborhoods.isNotEmpty()) add(condition.neighborhoods.map { it.name }.sorted().joinToString(", "))
        PriceRange.entries.filter { it in condition.priceRanges }.takeIf { it.isNotEmpty() }
            ?.let { ranges -> add(ranges.map { stringResource(it.labelRes()) }.joinToString(", ")) }
    }
    val second = buildList {
        AreaRange.entries.filter { it in condition.areaRanges }.takeIf { it.isNotEmpty() }
            ?.let { ranges -> add(ranges.map { stringResource(it.labelRes()) }.joinToString(", ")) }
        condition.minHouseholds?.let { add(stringResource(it.shortLabelRes())) }
        condition.maxBuildingAge?.let { add(stringResource(it.shortLabelRes())) }
    }
    return listOf(first, second).filter { it.isNotEmpty() }.map { it.joinToString(" · ") }
}

@Composable
private fun MatchCard(card: MatchCardUi, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.wpx),
        color = colors.background,
        contentColor = colors.onSurface,
        border = BorderStroke(3.wpx, if (card.change != null) colors.primary else colors.outline),
    ) {
        Column(Modifier.padding(horizontal = 32.wpx, vertical = 24.wpx)) {
            // 배지가 없어도 같은 높이를 차지해 카드끼리 오각형 위치가 맞는다.
            Box(Modifier.fillMaxWidth().heightIn(min = 68.wpx), contentAlignment = Alignment.CenterEnd) {
                if (card.change != null) {
                    ChangeBadge(text = scrapBadgeText(card.change, card.deltaManwon), kind = card.change)
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                PentagonChart(values = card.scores, radius = 120.wpx, showReference = true)
            }
            Text(text = card.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.my_page_match, card.ratePercent),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun ColumnScope.Placeholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.wpx))
            .padding(48.wpx),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScrapsTile(scrapCount: Int, changedCount: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val changed = changedCount > 0
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(208.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = colors.background,
        contentColor = colors.onSurface,
        border = BorderStroke(3.wpx, if (changed) colors.primary else colors.outline),
    ) {
        Row(Modifier.padding(horizontal = 40.wpx), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(R.string.my_page_all_scraps, scrapCount), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (changed) stringResource(R.string.my_page_changes, changedCount) else stringResource(R.string.my_page_no_changes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                )
            }
            Text(text = "›", style = MaterialTheme.typography.displayMedium, color = colors.primary)
        }
    }
}

private val previewCondition = InterestCondition(
    neighborhoods = setOf(Neighborhood("a", "정자동"), Neighborhood("b", "수내동")),
    priceRanges = setOf(PriceRange.FROM_10_TO_15),
    areaRanges = setOf(AreaRange.PY_30S),
    minHouseholds = HouseholdMin.OVER_500,
    maxBuildingAge = BuildingAge.WITHIN_10,
)

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun MyPagePreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        MyPageScreen(
            MyPageState(
                isLoading = false,
                condition = previewCondition,
                cards = listOf(
                    MatchCardUi("a", "한빛마을 1단지", listOf(5f, 5f, 5f, 4f, 4f), 92, ScrapChangeKind.NEW_DEAL, 3_000),
                    MatchCardUi("b", "모아뜰 2단지", listOf(4f, 5f, 5f, 3f, 3.5f), 78, null, 0),
                    MatchCardUi("c", "새솔 리버뷰", listOf(3f, 5f, 5f, 4f, 2.5f), 61, null, 0),
                ),
                scrapCount = 8,
                changedCount = 2,
            ),
            onIntent = {},
        )
    }
}
