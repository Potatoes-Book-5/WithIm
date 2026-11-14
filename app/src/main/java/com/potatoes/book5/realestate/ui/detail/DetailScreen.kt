package com.potatoes.book5.realestate.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.PricePoint
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.ui.common.BackButton
import com.potatoes.book5.realestate.ui.common.ChangeBadge
import com.potatoes.book5.realestate.ui.common.PillButton
import com.potatoes.book5.realestate.ui.common.ScrapToggleButton
import com.potatoes.book5.realestate.ui.common.formatAmount
import com.potatoes.book5.realestate.ui.common.formatCount
import com.potatoes.book5.realestate.ui.common.formatDealDate
import com.potatoes.book5.realestate.ui.common.formatDistance
import com.potatoes.book5.realestate.ui.common.formatMonthDay
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.common.formatYearMonth
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

/** SCR-002 아파트 상세. 와이어프레임: docs/design/wireframes/SCR-002_landscape_parked.svg */
@Composable
fun DetailScreen(state: DetailState, onIntent: (DetailIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.wpx),
    ) {
        AppBar(state, onIntent)
        val detail = state.detail
        when {
            detail != null -> Row(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 40.wpx, bottom = 48.wpx),
                horizontalArrangement = Arrangement.spacedBy(48.wpx),
            ) {
                // 와이어프레임 비율(1000 : 555)을 유지한다.
                PriceColumn(state, detail, Modifier.weight(1000f).fillMaxHeight())
                InfoColumn(detail, Modifier.weight(555f).fillMaxHeight())
            }
            state.failed -> Message(stringResource(R.string.detail_failed)) {
                Spacer(Modifier.height(32.wpx))
                PillButton(text = stringResource(R.string.detail_retry), onClick = { onIntent(DetailIntent.Retry) })
            }
            else -> Message(stringResource(R.string.detail_loading))
        }
    }
}

@Composable
private fun AppBar(state: DetailState, onIntent: (DetailIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = { onIntent(DetailIntent.Back) })
        Spacer(Modifier.width(32.wpx))
        Column(Modifier.weight(1f)) {
            val detail = state.detail
            if (detail != null) {
                Text(text = detail.name, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = stringResource(R.string.detail_meta, detail.district, detail.neighborhood, formatCount(detail.households), detail.builtYear),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.detail != null) {
            ScrapToggleButton(
                scrapped = state.isScrapped,
                enabled = !state.isToggling,
                onToggle = { onIntent(DetailIntent.ToggleScrap) },
                modifier = Modifier.width(340.wpx),
            )
        }
    }
}

@Composable
private fun PriceColumn(state: DetailState, detail: ApartmentDetail, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val sub = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
    Column(modifier) {
        Text(text = stringResource(R.string.detail_latest_price), style = sub, color = colors.onSurfaceVariant)
        Text(text = formatPrice(detail.latestDeal.priceManwon), style = MaterialTheme.typography.displayLarge)
        Text(
            text = stringResource(R.string.detail_latest_meta, detail.latestDeal.areaPyeong, formatDealDate(detail.latestDeal.date), detail.latestDeal.floor),
            style = sub,
            color = colors.onSurfaceVariant,
        )
        val change = state.change
        val delta = state.deltaManwon
        if (change != null || delta != null) {
            Spacer(Modifier.height(24.wpx))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (change != null) {
                    ChangeBadge(text = stringResource(change.detailLabelRes()), kind = change)
                    Spacer(Modifier.width(24.wpx))
                }
                if (delta != null) {
                    Text(
                        text = when {
                            delta > 0 -> stringResource(R.string.detail_delta_up, formatAmount(delta))
                            delta < 0 -> stringResource(R.string.detail_delta_down, formatAmount(abs(delta)))
                            else -> stringResource(R.string.detail_delta_same)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            delta > 0 -> colors.error
                            delta < 0 -> colors.primary
                            else -> colors.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(48.wpx))
        TrendCard(detail.trend, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun TrendCard(trend: List<PricePoint>, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.wpx),
        color = colors.surfaceVariant,
        contentColor = colors.onSurface,
    ) {
        Column(Modifier.padding(40.wpx)) {
            Text(text = stringResource(R.string.detail_trend_title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(32.wpx))
            // 차트는 추세만 보여 주는 단순 선 그래프다(SCR-002 차트 단순화). 정확한 값은 최근 거래 목록에서 읽는다.
            val line = colors.outline
            val accent = colors.primary
            Canvas(Modifier.fillMaxWidth().weight(1f).clearAndSetSemantics {}) {
                val rows = 4
                val gap = size.height / (rows - 1)
                repeat(rows) { i -> drawLine(line, Offset(0f, gap * i), Offset(size.width, gap * i), strokeWidth = 2.wpx.toPx()) }
                if (trend.size < 2) return@Canvas
                val min = trend.minOf { it.priceManwon }
                val max = trend.maxOf { it.priceManwon }
                val span = (max - min).coerceAtLeast(1)
                val inset = 30.wpx.toPx()
                val step = (size.width - inset * 2) / (trend.size - 1)
                val points = trend.mapIndexed { i, p ->
                    Offset(inset + step * i, size.height - (p.priceManwon - min).toFloat() / span * size.height * 0.8f - size.height * 0.1f)
                }
                val path = Path().apply {
                    points.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                }
                drawPath(path, accent, style = Stroke(width = 6.wpx.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(accent, radius = 12.wpx.toPx(), center = points.last())
            }
            Spacer(Modifier.height(24.wpx))
            if (trend.isNotEmpty()) {
                Row(Modifier.fillMaxWidth()) {
                    val label = MaterialTheme.typography.bodySmall
                    Text(text = formatYearMonth(trend.first().month), style = label, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(text = formatYearMonth(trend.last().month), style = label, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(detail: ApartmentDetail, modifier: Modifier) {
    Column(modifier) {
        Text(text = stringResource(R.string.detail_info), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.wpx))
        Divider()
        InfoRow(stringResource(R.string.detail_households), stringResource(R.string.detail_households_value, formatCount(detail.households)))
        InfoRow(stringResource(R.string.detail_built), stringResource(R.string.detail_built_value, detail.builtYear))
        InfoRow(stringResource(R.string.detail_pyeong), stringResource(R.string.detail_pyeong_value, detail.minPyeong, detail.maxPyeong))
        InfoRow(
            stringResource(R.string.detail_distance),
            detail.distanceKm?.let { formatDistance(it) } ?: stringResource(R.string.detail_distance_unknown),
        )
        Spacer(Modifier.height(64.wpx))
        Text(text = stringResource(R.string.detail_recent_deals), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.wpx))
        Column(verticalArrangement = Arrangement.spacedBy(16.wpx)) {
            detail.recentDeals.forEach { DealRow(it) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 90.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
    Divider()
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(2.wpx).background(MaterialTheme.colorScheme.outline))
}

@Composable
private fun DealRow(deal: Deal) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.wpx),
        shape = RoundedCornerShape(20.wpx),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(Modifier.padding(horizontal = 32.wpx), verticalAlignment = Alignment.CenterVertically) {
            Text(text = formatMonthDay(deal.date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(128.wpx))
            Text(
                text = stringResource(R.string.detail_deal_meta, deal.areaPyeong, deal.floor),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(text = formatPrice(deal.priceManwon), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Message(text: String, extra: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        extra()
    }
}

private fun ScrapChangeKind.detailLabelRes(): Int = when (this) {
    ScrapChangeKind.NEW_DEAL -> R.string.scrap_new_deal
    ScrapChangeKind.PRICE_UP -> R.string.scrap_price_up
    ScrapChangeKind.PRICE_DOWN -> R.string.scrap_price_down
    ScrapChangeKind.RANK_CHANGE -> R.string.scrap_rank_change
}

private val previewDetail = ApartmentDetail(
    id = "a",
    name = "한빛마을 1단지",
    district = "분당구",
    neighborhood = "정자동",
    households = 1_240,
    builtYear = 2009,
    minPyeong = 24,
    maxPyeong = 45,
    distanceKm = 1.2,
    latestDeal = Deal(LocalDate.of(2026, 10, 30), 34, 12, 158_000),
    previousDeal = Deal(LocalDate.of(2026, 9, 14), 34, 7, 155_000),
    trend = listOf(0.80, 0.82, 0.81, 0.85, 0.87, 0.86, 0.89, 0.92, 0.93, 0.96, 0.95, 1.0)
        .mapIndexed { i, r -> PricePoint(YearMonth.of(2026, 10).minusMonths((11 - i).toLong()), (158_000 * r).toLong()) },
    recentDeals = listOf(
        Deal(LocalDate.of(2026, 10, 30), 34, 12, 158_000),
        Deal(LocalDate.of(2026, 9, 14), 34, 7, 155_000),
        Deal(LocalDate.of(2026, 8, 2), 24, 3, 112_000),
    ),
)

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun DetailScreenPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        DetailScreen(
            DetailState(isLoading = false, detail = previewDetail, isScrapped = true, change = ScrapChangeKind.NEW_DEAL, deltaManwon = 3_000),
            onIntent = {},
        )
    }
}
