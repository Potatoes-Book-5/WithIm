package com.potatoes.book5.realestate.ui.scrap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.common.BackButton
import com.potatoes.book5.realestate.ui.common.ChangeBadge
import com.potatoes.book5.realestate.ui.common.MinTouchHeight
import com.potatoes.book5.realestate.ui.common.formatMonthDay
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.common.scrapBadgeText
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.LocalDate

/** SCR-007 스크랩 목록. 와이어프레임: docs/design/wireframes/SCR-007_landscape_parked.svg */
@Composable
fun ScrapListScreen(state: ScrapListState, onIntent: (ScrapListIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.wpx),
    ) {
        AppBar(state, onIntent)
        val items = state.visibleItems
        when {
            state.isLoading -> Unit
            state.items.isEmpty() -> EmptyState(
                title = stringResource(R.string.scrap_empty_title),
                description = stringResource(R.string.scrap_empty_desc),
            )
            items.isEmpty() -> EmptyState(title = stringResource(R.string.scrap_changed_empty), description = null)
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(top = 32.wpx, bottom = 48.wpx),
                verticalArrangement = Arrangement.spacedBy(16.wpx),
            ) {
                items(items, key = { it.id }) { item ->
                    ScrapRow(item, onClick = { onIntent(ScrapListIntent.OpenItem(item.id)) })
                }
            }
        }
    }
}

@Composable
private fun AppBar(state: ScrapListState, onIntent: (ScrapListIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = { onIntent(ScrapListIntent.Back) })
        Spacer(Modifier.width(32.wpx))
        Text(
            text = stringResource(R.string.scrap_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.weight(1f),
        )
        FilterSegment(
            selected = state.filter,
            totalCount = state.totalCount,
            changedCount = state.changedCount,
            onSelect = { onIntent(ScrapListIntent.SelectFilter(it)) },
        )
    }
}

@Composable
private fun FilterSegment(
    selected: ScrapFilter,
    totalCount: Int,
    changedCount: Int,
    onSelect: (ScrapFilter) -> Unit,
) {
    Surface(
        modifier = Modifier.width(640.wpx).height(MinTouchHeight),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(Modifier.padding(8.wpx).selectableGroup()) {
            FilterOption(stringResource(R.string.scrap_filter_all, totalCount), selected == ScrapFilter.ALL) { onSelect(ScrapFilter.ALL) }
            FilterOption(stringResource(R.string.scrap_filter_changed, changedCount), selected == ScrapFilter.CHANGED) { onSelect(ScrapFilter.CHANGED) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FilterOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        shape = CircleShape,
        color = if (selected) colors.background else colors.surfaceVariant,
        contentColor = if (selected) colors.onSurface else colors.onSurfaceVariant,
        border = if (selected) BorderStroke(2.wpx, colors.outline) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            )
        }
    }
}

@Composable
private fun ScrapRow(item: ScrapItemUi, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val changed = item.change != null
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 168.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = if (changed) colors.background else colors.surfaceVariant,
        contentColor = colors.onSurface,
        border = if (changed) BorderStroke(3.wpx, colors.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(start = 32.wpx, end = 88.wpx, top = 24.wpx, bottom = 24.wpx),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 변동 점은 배지와 함께 보여 주는 보조 표시다(색만으로 구분하지 않음).
            Box(Modifier.width(48.wpx), contentAlignment = Alignment.CenterStart) {
                if (changed) Surface(Modifier.size(24.wpx), shape = CircleShape, color = colors.error) {}
            }
            Column(Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.scrap_item_meta, item.neighborhood, item.areaPyeong, formatMonthDay(item.scrappedOn)),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurfaceVariant,
                )
            }
            if (item.change != null) {
                ChangeBadge(text = scrapBadgeText(item.change, item.deltaManwon), kind = item.change)
                Spacer(Modifier.width(40.wpx))
            }
            Text(
                text = formatPrice(item.priceManwon),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 300.wpx),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EmptyState(title: String, description: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (description != null) {
            Spacer(Modifier.height(16.wpx))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val previewItems = listOf(
    ScrapItemUi("a", "한빛마을 1단지", "정자동", 34, 158_000, LocalDate.of(2026, 10, 28), ScrapChangeKind.NEW_DEAL, 3_000),
    ScrapItemUi("b", "모아뜰 2단지", "수내동", 32, 134_000, LocalDate.of(2026, 10, 21), ScrapChangeKind.PRICE_UP, 2_000),
    ScrapItemUi("c", "새솔 리버뷰", "정자동", 34, 245_000, LocalDate.of(2026, 11, 2), null, 0),
    ScrapItemUi("d", "청솔 하이츠", "서현동", 33, 198_000, LocalDate.of(2026, 10, 15), null, 0),
)

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun ScrapListPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        ScrapListScreen(ScrapListState(isLoading = false, items = previewItems), onIntent = {})
    }
}

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun ScrapListEmptyPreview() {
    KingOfRealEstateTheme(darkTheme = false) { ScrapListScreen(ScrapListState(isLoading = false), onIntent = {}) }
}
