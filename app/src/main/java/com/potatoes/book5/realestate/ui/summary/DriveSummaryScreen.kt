package com.potatoes.book5.realestate.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.model.RecommendationKey
import com.potatoes.book5.realestate.ui.common.CrownIcon
import com.potatoes.book5.realestate.ui.common.MinTouchHeight
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.common.PillStyle
import com.potatoes.book5.realestate.ui.common.ScrapToggleButton
import com.potatoes.book5.realestate.ui.common.formatDistance
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** SCR-008 주행 요약. 와이어프레임: docs/design/wireframes/SCR-008_landscape_parked.svg */
@Composable
fun DriveSummaryScreen(state: DriveSummaryState, onIntent: (DriveSummaryIntent) -> Unit, today: LocalDate = LocalDate.now()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.wpx),
    ) {
        val summary = state.summary
        AppBar(summary, today, onClose = { onIntent(DriveSummaryIntent.Close) })
        when {
            // 불러오는 중이거나 기록이 없으면 비워 둔다.
            state.isLoading || summary == null -> Unit
            else -> Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 40.wpx, bottom = 48.wpx),
                horizontalArrangement = Arrangement.spacedBy(64.wpx),
            ) {
                // 와이어프레임 비율(820 : 719)을 유지한다.
                Column(Modifier.weight(820f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    FinalRanking(summary, onOpen = { onIntent(DriveSummaryIntent.OpenApartment(it)) })
                    Spacer(Modifier.height(48.wpx))
                    LeaderTimeline(summary.leaderChanges)
                }
                PassedInterests(
                    recommendations = summary.recommendationTop,
                    items = summary.passedInterests,
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.weight(719f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun AppBar(summary: DriveSummary?, today: LocalDate, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val date = summary?.endedAt?.toLocalDate()
            Text(
                text = when {
                    date == null || date == today -> stringResource(R.string.summary_title_today)
                    else -> stringResource(R.string.summary_title_date, date.monthValue, date.dayOfMonth)
                },
                style = MaterialTheme.typography.headlineLarge,
            )
            if (summary != null && date != null) {
                val parts = buildList {
                    add(stringResource(R.string.summary_date, date.monthValue, date.dayOfMonth))
                    summary.distanceKm?.let { add(formatDistance(it)) }
                    add(durationText(summary.duration))
                }
                Text(
                    text = parts.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PillButton(text = stringResource(R.string.summary_close), onClick = onClose, modifier = Modifier.width(240.wpx))
    }
}

@Composable
private fun durationText(duration: Duration): String {
    val minutes = duration.toMinutes()
    return when {
        minutes < 1 -> stringResource(R.string.summary_duration_short)
        minutes < 60 -> stringResource(R.string.summary_duration_minutes, minutes.toInt())
        else -> stringResource(R.string.summary_duration_hours, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}

@Composable
private fun FinalRanking(summary: DriveSummary, onOpen: (String) -> Unit) {
    Text(text = stringResource(if (summary.rounds.isEmpty()) R.string.summary_final_ranking else R.string.summary_trip_ranking,
        summary.radiusKm), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(24.wpx))
    Column(verticalArrangement = Arrangement.spacedBy(16.wpx)) {
        summary.tripTop.forEach { RankRow(it, onClick = { onOpen(it.apartmentId) }) }
    }
}

@Composable
private fun RankRow(item: DriveRank, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val leader = item.rank == 1
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 116.wpx),
        shape = RoundedCornerShape(24.wpx),
        color = if (leader) colors.primaryContainer else colors.surfaceVariant,
        contentColor = colors.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 32.wpx), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(72.wpx), contentAlignment = Alignment.CenterStart) {
                if (leader) {
                    CrownIcon()
                } else {
                    Text(
                        text = item.rank.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(44.wpx),
                    )
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = formatPrice(item.priceManwon), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun LeaderTimeline(changes: List<DriveLeaderChange>) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = if (changes.isEmpty()) stringResource(R.string.summary_no_leader_change) else stringResource(R.string.summary_leader_changes, changes.size),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(24.wpx))
    changes.forEachIndexed { i, change ->
        val last = i == changes.lastIndex
        Row(Modifier.fillMaxWidth().heightIn(min = 132.wpx)) {
            // 세로선과 점. 마지막(현재 대장)만 채운다.
            Box(Modifier.width(48.wpx).height(132.wpx), contentAlignment = Alignment.TopCenter) {
                if (!last) Box(Modifier.padding(top = 32.wpx).width(4.wpx).fillMaxHeight().background(colors.outline))
                Surface(
                    modifier = Modifier.padding(top = 4.wpx).size(32.wpx),
                    shape = CircleShape,
                    color = if (last) colors.primary else colors.background,
                    border = BorderStroke(3.wpx, colors.primary),
                ) {}
            }
            Spacer(Modifier.width(24.wpx))
            Text(
                text = change.at.format(TIME),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(128.wpx),
            )
            Text(
                text = listOfNotNull(change.districtName, change.name).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (last) FontWeight.Bold else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PassedInterests(
    recommendations: List<DriveRecommendation>,
    items: List<PassedInterest>,
    state: DriveSummaryState,
    onIntent: (DriveSummaryIntent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        if (items.isEmpty() && recommendations.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else {
            if (recommendations.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 16.wpx),
                    verticalArrangement = Arrangement.spacedBy(24.wpx),
                ) {
                    item(key = "recommendations-title") {
                        Text(text = stringResource(R.string.summary_recommendations), style = MaterialTheme.typography.titleMedium)
                    }
                    items(recommendations, key = { "recommendation-${it.apartmentId}" }) { item ->
                        RecommendationRow(
                            item = item,
                            scrapped = item.apartmentId in state.scrappedIds,
                            toggling = item.apartmentId in state.togglingIds,
                            onOpen = { onIntent(DriveSummaryIntent.OpenRecommendation(item)) },
                            onToggle = { onIntent(DriveSummaryIntent.ToggleScrap(item.apartmentId)) },
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.wpx))
            // SCR-011: 이번 주행에서 만난 관심 매물만 모아 본다(T-026).
            PillButton(
                text = stringResource(R.string.summary_open_catch_up),
                onClick = { onIntent(DriveSummaryIntent.OpenInterestCatchUp) },
                style = PillStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = stringResource(R.string.summary_open_scraps),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = MinTouchHeight)
                .clickable { onIntent(DriveSummaryIntent.OpenScraps) }
                .padding(horizontal = 16.wpx, vertical = 36.wpx),
        )
    }
}

@Composable
private fun RecommendationRow(
    item: DriveRecommendation,
    scrapped: Boolean,
    toggling: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().heightIn(min = 192.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 40.wpx, vertical = 24.wpx), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${recommendationKeyword(item)} · ${formatPrice(item.priceManwon)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(24.wpx))
            ScrapToggleButton(scrapped = scrapped, enabled = !toggling, onToggle = onToggle, modifier = Modifier.width(300.wpx))
        }
    }
}

@Composable
private fun recommendationKeyword(item: DriveRecommendation): String = when (item.mainRecoKey) {
    RecommendationKey.PRICE -> stringResource(when {
        item.priceManwon < 50_000 -> R.string.recommendation_keyword_price_under_5
        item.priceManwon < 100_000 -> R.string.recommendation_keyword_price_5_10
        item.priceManwon < 150_000 -> R.string.recommendation_keyword_price_10_15
        item.priceManwon < 200_000 -> R.string.recommendation_keyword_price_15_20
        else -> R.string.recommendation_keyword_price_over_20
    })
    RecommendationKey.AREA -> stringResource(when {
        item.areaPyeong < 20 -> R.string.recommendation_keyword_area_small
        item.areaPyeong < 30 -> R.string.recommendation_keyword_area_medium
        item.areaPyeong < 40 -> R.string.recommendation_keyword_area_preferred
        else -> R.string.recommendation_keyword_area_large
    })
    RecommendationKey.NEIGHBORHOOD -> item.neighborhoodName?.takeIf { it.isNotBlank() }?.let {
        stringResource(R.string.recommendation_keyword_neighborhood, it)
    } ?: stringResource(R.string.recommendation_keyword_neighborhood_generic)
    RecommendationKey.HOUSEHOLDS -> item.households?.let { count ->
        stringResource(when {
            count >= 1_000 -> R.string.recommendation_keyword_households_large
            count >= 500 -> R.string.recommendation_keyword_households_medium
            count >= 300 -> R.string.recommendation_keyword_households_300
            else -> R.string.recommendation_keyword_households_near
        })
    } ?: stringResource(R.string.recommendation_keyword_households_unknown)
    RecommendationKey.NEWNESS -> item.builtYear?.let { builtYear ->
        val age = (LocalDate.now().year - builtYear).coerceAtLeast(0)
        stringResource(when {
            age <= 10 -> R.string.recommendation_keyword_newness_new
            age <= 15 -> R.string.recommendation_keyword_newness_recent
            else -> R.string.recommendation_keyword_newness_near
        })
    } ?: stringResource(R.string.recommendation_keyword_newness_unknown)
}

@Composable
private fun PassedRow(item: PassedInterest, scrapped: Boolean, toggling: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().heightIn(min = 192.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 40.wpx, vertical = 24.wpx), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${formatDistance(item.closestKm)} · ${formatPrice(item.priceManwon)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(24.wpx))
            ScrapToggleButton(scrapped = scrapped, enabled = !toggling, onToggle = onToggle, modifier = Modifier.width(300.wpx))
        }
    }
}

private val previewSummary = DriveSummary(
    id = 1,
    startedAt = LocalDateTime.of(2026, 11, 13, 10, 0),
    endedAt = LocalDateTime.of(2026, 11, 13, 11, 12),
    distanceKm = null,
    radiusKm = 10,
    finalTop = listOf(
        DriveRank(1, "a", "새솔 리버뷰", 245_000),
        DriveRank(2, "b", "은행나무 파크", 221_000),
        DriveRank(3, "c", "청솔 하이츠", 198_000),
    ),
    leaderChanges = listOf(
        DriveLeaderChange(LocalDateTime.of(2026, 11, 13, 10, 12), "b", "은행나무 파크"),
        DriveLeaderChange(LocalDateTime.of(2026, 11, 13, 10, 41), "c", "청솔 하이츠"),
        DriveLeaderChange(LocalDateTime.of(2026, 11, 13, 11, 5), "a", "새솔 리버뷰"),
    ),
    passedInterests = listOf(
        PassedInterest("h", "한빛마을 1단지", 1.2, 158_000),
        PassedInterest("m", "모아뜰 2단지", 3.4, 134_000),
    ),
    seen = false,
    recommendations = listOf(
        DriveRecommendation("h", "한빛마을 1단지", 158_000, 34, 92, 1.2, RecommendationKey.NEWNESS, "매탄동", builtYear = 2020),
        DriveRecommendation("m", "모아뜰 2단지", 134_000, 32, 88, 3.4, RecommendationKey.AREA, "망포동"),
    ),
)

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun DriveSummaryPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        DriveSummaryScreen(
            DriveSummaryState(isLoading = false, summary = previewSummary, scrappedIds = setOf("h")),
            onIntent = {},
            today = LocalDate.of(2026, 11, 13),
        )
    }
}
