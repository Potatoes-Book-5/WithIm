package com.potatoes.book5.realestate.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.core.ui.R as CoreUiR
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.common.CrownIcon
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.common.RankItemUi
import com.potatoes.book5.realestate.ui.common.formatDataAsOf
import com.potatoes.book5.realestate.ui.common.formatDealDate
import com.potatoes.book5.realestate.ui.common.formatDistance
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.common.labelRes
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.LocalDate
import java.time.LocalDateTime

/** SCR-001 홈. 와이어프레임: docs/design/wireframes/SCR-001_landscape_parked.svg */
@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.wpx),
    ) {
        AppBar(state, onIntent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 24.wpx, bottom = 48.wpx),
            horizontalArrangement = Arrangement.spacedBy(40.wpx),
        ) {
            // 와이어프레임 비율(1040 : 523)을 유지한다.
            Column(
                modifier = Modifier.weight(2f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(24.wpx),
            ) {
                val leader = state.leader
                if (leader != null) {
                    LeaderCard(leader, onClick = { onIntent(HomeIntent.OpenApartment(leader.id)) }, modifier = Modifier.weight(440f))
                    state.followers.forEach { item ->
                        RankRow(item, onClick = { onIntent(HomeIntent.OpenApartment(item.id)) }, modifier = Modifier.weight(200f))
                    }
                }
                // 불러오는 중에는 비워 둔다.
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(24.wpx),
            ) {
                ScrapChangeCard(state.scrap, onClick = { onIntent(HomeIntent.OpenScrapChanges) }, modifier = Modifier.weight(260f))
                InterestCard(state.interest, state.todayMatches, onClick = { onIntent(HomeIntent.OpenInterest) }, modifier = Modifier.weight(260f))
                Tile(
                    text = stringResource(R.string.home_scraps, state.scrap?.totalCount ?: 0),
                    onClick = { onIntent(HomeIntent.OpenScraps) },
                    modifier = Modifier.weight(160f),
                )
                Tile(
                    text = stringResource(R.string.home_drive_summary),
                    onClick = { onIntent(HomeIntent.OpenDriveSummary) },
                    modifier = Modifier.weight(160f),
                )
            }
        }
    }
}

@Composable
private fun AppBar(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
            val asOf = state.dataAsOf
            if (asOf != null) {
                Text(
                    text = stringResource(R.string.home_subtitle, state.neighborhoodName, state.radiusKm, formatDataAsOf(asOf)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PillButton(
            text = stringResource(R.string.home_my_page),
            onClick = { onIntent(HomeIntent.OpenMyPage) },
            modifier = Modifier.width(320.wpx),
        )
        Spacer(Modifier.width(24.wpx))
        PillButton(
            text = stringResource(R.string.home_settings),
            onClick = { onIntent(HomeIntent.OpenSettings) },
            modifier = Modifier.width(220.wpx),
        )
    }
}

@Composable
private fun LeaderCard(item: RankItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.wpx),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(3.wpx, MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(horizontal = 48.wpx, vertical = 40.wpx), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrownIcon(size = 52.wpx)
                Spacer(Modifier.width(16.wpx))
                Text(
                    text = stringResource(R.string.home_leader),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(CoreUiR.string.rank_n, item.rank),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(text = item.name, style = MaterialTheme.typography.displayMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = formatPrice(item.priceManwon), style = MaterialTheme.typography.displayLarge, maxLines = 1)
                Text(
                    text = stringResource(R.string.home_leader_meta, item.areaPyeong, formatDealDate(item.dealDate), formatDistance(item.distanceKm)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RankRow(item: RankItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 40.wpx),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.rank.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.wpx),
            )
            Column(Modifier.weight(1f).padding(start = 24.wpx)) {
                Text(text = item.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = MaterialTheme.typography.headlineLarge.fontWeight), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.home_rank_meta, item.areaPyeong, formatDealDate(item.dealDate), formatDistance(item.distanceKm)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = formatPrice(item.priceManwon), style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
private fun ScrapChangeCard(scrap: ScrapCardUi?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val changed = (scrap?.changedCount ?: 0) > 0
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.wpx),
        color = if (changed) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant,
        border = if (changed) BorderStroke(3.wpx, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box(Modifier.padding(40.wpx)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.wpx)) {
                Text(
                    text = if (changed) stringResource(R.string.home_scrap_changes, scrap!!.changedCount) else stringResource(R.string.home_scrap_no_changes),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (changed && scrap!!.firstChangedName != null) {
                    Text(text = scrap.firstChangedName!!, style = MaterialTheme.typography.labelMedium.copy(fontWeight = null), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val kind = stringResource(scrap.firstChangeKind.labelRes())
                    Text(
                        text = if (scrap.changedCount > 1) stringResource(R.string.home_scrap_change_more, kind, scrap.changedCount - 1) else kind,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = null),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.wpx))
                    Text(text = stringResource(R.string.home_check), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (changed) {
                Surface(Modifier.align(Alignment.TopEnd).size(28.wpx), shape = CircleShape, color = MaterialTheme.colorScheme.error) {}
            }
        }
    }
}

@Composable
private fun InterestCard(interest: InterestCardUi?, todayMatches: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(40.wpx), verticalArrangement = Arrangement.spacedBy(8.wpx)) {
            Text(text = stringResource(R.string.home_interest), style = MaterialTheme.typography.titleMedium)
            val sub = MaterialTheme.typography.labelMedium.copy(fontWeight = null)
            if (interest == null || interest.isEmpty) {
                Text(text = stringResource(R.string.home_interest_empty), style = sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                if (interest.neighborhoods.isNotEmpty()) {
                    Text(text = interest.neighborhoods.joinToString(", "), style = sub, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val ranges = PriceRange.entries.filter { it in interest.prices }.map { stringResource(it.labelRes()) } +
                    AreaRange.entries.filter { it in interest.areas }.map { stringResource(it.labelRes()) }
                if (ranges.isNotEmpty()) {
                    Text(text = ranges.joinToString(" · "), style = sub, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.wpx))
                Text(text = stringResource(R.string.home_today_matches, todayMatches), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Tile(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 40.wpx), verticalAlignment = Alignment.CenterVertically) {
            Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(text = "›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun ScrapChangeKind?.labelRes(): Int = when (this) {
    ScrapChangeKind.PRICE_UP -> R.string.scrap_price_up
    ScrapChangeKind.PRICE_DOWN -> R.string.scrap_price_down
    ScrapChangeKind.RANK_CHANGE -> R.string.scrap_rank_change
    ScrapChangeKind.NEW_DEAL, null -> R.string.scrap_new_deal
}

private val previewState = HomeState(
    isLoading = false,
    neighborhoodName = "정자동",
    dataAsOf = LocalDateTime.of(2026, 11, 13, 9, 0),
    ranking = listOf(
        RankItemUi("a", 1, "새솔 리버뷰", 245_000, 34, LocalDate.of(2026, 10, 21), 2.4, 25.0),
        RankItemUi("b", 2, "은행나무 파크", 221_000, 38, LocalDate.of(2026, 9, 30), 4.1, -40.0),
        RankItemUi("c", 3, "청솔 하이츠", 198_000, 33, LocalDate.of(2026, 10, 12), 6.8, -160.0),
    ),
    scrap = ScrapCardUi(8, 2, "한빛마을 1단지", ScrapChangeKind.NEW_DEAL),
    interest = InterestCardUi(listOf("수내동", "정자동"), setOf(PriceRange.FROM_10_TO_15), setOf(AreaRange.PY_30S)),
)

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun HomeScreenPreview() {
    KingOfRealEstateTheme(darkTheme = false) { HomeScreen(state = previewState, onIntent = {}) }
}
