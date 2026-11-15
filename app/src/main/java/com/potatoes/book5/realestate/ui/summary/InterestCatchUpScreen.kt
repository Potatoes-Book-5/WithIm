package com.potatoes.book5.realestate.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.ui.common.BackButton
import com.potatoes.book5.realestate.ui.common.MinTouchHeight
import com.potatoes.book5.realestate.ui.common.ScrapToggleButton
import com.potatoes.book5.realestate.ui.common.formatDistance
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.LocalDateTime

/**
 * SCR-011 관심 매물 모아보기. 와이어프레임: docs/design/wireframes/SCR-011_landscape_parked.svg
 * 주행 요약(SCR-008)의 `관심 매물 모아보기`로 열고, 뒤로 가면 SCR-008로 돌아간다.
 */
@Composable
fun InterestCatchUpScreen(state: DriveSummaryState, onIntent: (DriveSummaryIntent) -> Unit) {
    val summary = state.summary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.wpx),
    ) {
        Header(summary, onBack = { onIntent(DriveSummaryIntent.Close) })
        val recommendations = summary?.recommendations.orEmpty()
        val passedItems = summary?.additionalPassedInterests.orEmpty()
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(top = 40.wpx, bottom = 16.wpx),
            verticalArrangement = Arrangement.spacedBy(24.wpx),
        ) {
            if (recommendations.isNotEmpty()) {
                items(recommendations, key = { "recommendation-${it.apartmentId}" }) { item ->
                    CatchUpRecommendationRow(
                        item = item,
                        scrapped = item.apartmentId in state.scrappedIds,
                        toggling = item.apartmentId in state.togglingIds,
                        onOpen = { onIntent(DriveSummaryIntent.OpenApartment(item.apartmentId)) },
                        onToggle = { onIntent(DriveSummaryIntent.ToggleScrap(item.apartmentId)) },
                    )
                }
            }
            items(passedItems, key = { it.apartmentId }) { item ->
                CatchUpRow(
                    item = item,
                    scrapped = item.apartmentId in state.scrappedIds,
                    toggling = item.apartmentId in state.togglingIds,
                    onOpen = { onIntent(DriveSummaryIntent.OpenApartment(item.apartmentId)) },
                    onToggle = { onIntent(DriveSummaryIntent.ToggleScrap(item.apartmentId)) },
                )
            }
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
private fun Header(summary: DriveSummary?, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Spacer(Modifier.width(32.wpx))
        Text(stringResource(R.string.catch_up_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        if (summary != null) {
            val date = summary.endedAt.toLocalDate()
            Text(
                stringResource(
                    R.string.catch_up_meta,
                    summary.interestSummaryCount,
                    date.monthValue,
                    date.dayOfMonth,
                ),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CatchUpRecommendationRow(
    item: DriveRecommendation,
    scrapped: Boolean,
    toggling: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().heightIn(min = 176.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 40.wpx, vertical = 24.wpx), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.wpx))
            Spacer(Modifier.width(32.wpx))
            Column(Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.catch_up_closest, formatDistance(item.closestKm)),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(24.wpx))
            Text(text = formatPrice(item.priceManwon), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(40.wpx))
            ScrapToggleButton(scrapped = scrapped, enabled = !toggling, onToggle = onToggle, modifier = Modifier.width(300.wpx))
        }
    }
}

@Composable
private fun CatchUpRow(item: PassedInterest, scrapped: Boolean, toggling: Boolean, onOpen: () -> Unit, onToggle: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().heightIn(min = 176.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 40.wpx, vertical = 24.wpx), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.wpx))
            Spacer(Modifier.width(32.wpx))
            Column(Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.catch_up_closest, formatDistance(item.closestKm)),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(24.wpx))
            Text(text = formatPrice(item.priceManwon), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(40.wpx))
            ScrapToggleButton(scrapped = scrapped, enabled = !toggling, onToggle = onToggle, modifier = Modifier.width(300.wpx))
        }
    }
}

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun InterestCatchUpPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        InterestCatchUpScreen(
            DriveSummaryState(
                isLoading = false,
                summary = DriveSummary(
                    id = 1,
                    startedAt = LocalDateTime.of(2026, 11, 14, 10, 0),
                    endedAt = LocalDateTime.of(2026, 11, 14, 10, 32),
                    distanceKm = null,
                    radiusKm = 10,
                    finalTop = emptyList(),
                    leaderChanges = emptyList(),
                    passedInterests = listOf(
                        PassedInterest("h", "한빛마을 1단지", 1.2, 158_000),
                        PassedInterest("n", "느티마을 3단지", 1.6, 129_000),
                    ),
                    seen = false,
                ),
                scrappedIds = setOf("h"),
            ),
            onIntent = {},
        )
    }
}
