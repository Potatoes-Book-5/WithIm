package com.potatoes.book5.realestate.ui.driving

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.potatoes.book5.realestate.feature.driving.R
import com.potatoes.book5.realestate.core.ui.R as CoreUiR
import com.potatoes.book5.realestate.ui.common.CrownIcon
import com.potatoes.book5.realestate.ui.common.MapPointUi
import com.potatoes.book5.realestate.ui.common.MinTouchHeight
import com.potatoes.book5.realestate.ui.common.RankBadge
import com.potatoes.book5.realestate.ui.common.RankItemUi
import com.potatoes.book5.realestate.ui.common.SpeakerIcon
import com.potatoes.book5.realestate.ui.common.formatDistance
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.common.formatPriceShort
import com.potatoes.book5.realestate.ui.theme.KingOfRealEstateTheme
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.LocalDate

/**
 * 주행 중 화면. 와이어프레임:
 * SCR-004_landscape_driving(순위), SCR-009_landscape_driving(지도), SCR-005_landscape_driving(대장 교체 알림)
 * 주행 중에는 Top 3만 크게 보여 주고, 화면 전환 외의 조작은 두지 않는다(DD-002, DG-004).
 */
@Composable
fun DrivingScreen(state: DrivingScreenState, onIntent: (DrivingIntent) -> Unit, naverMapAvailable: Boolean = false) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 48.wpx)) {
            Header(state = state, onSelect = { onIntent(DrivingIntent.SelectMode(it)) })
            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 24.wpx, bottom = 48.wpx), horizontalArrangement = Arrangement.spacedBy(32.wpx)) {
                Column(Modifier.weight(0.57f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.wpx)) {
                    Text(stringResource(R.string.driving_sample_label), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BoxWithConstraints(Modifier.weight(1f)) {
                        when (state.mode) {
                            DrivingMode.RANK -> if (maxWidth < 1500.wpx) {
                                Column(verticalArrangement = Arrangement.spacedBy(20.wpx)) {
                                    state.ranking.forEach { RankListRow(it) }
                                }
                            } else RankCards(state.ranking)
                            DrivingMode.MAP -> DrivingMap(state, naverMapAvailable, Modifier.fillMaxSize())
                        }
                    }
                }
                DrivingTradeTable(state.actualTrades, Modifier.weight(0.43f).fillMaxHeight())
            }
        }
        state.alert?.let { LeaderAlertOverlay(it) }
    }
}

@Composable
private fun Header(state: DrivingScreenState, onSelect: (DrivingMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.wpx),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeToggle(selected = state.mode, onSelect = onSelect)
        Spacer(Modifier.width(28.wpx))
        Text(
            text = stringResource(R.string.driving_radius, state.radiusKm),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface) {
            Text(
                text = stringResource(R.string.driving_badge),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 28.wpx, vertical = 12.wpx),
            )
        }
    }
}

/** 순위 | 지도 전환. 주행 화면의 유일한 터치 동작이다(SCR-004, 주행 중 허용 여부는 T-001). */
@Composable
private fun ModeToggle(selected: DrivingMode, onSelect: (DrivingMode) -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(8.wpx)) {
            listOf(DrivingMode.RANK to R.string.driving_mode_rank, DrivingMode.MAP to R.string.driving_mode_map).forEach { (mode, label) ->
                val isSelected = mode == selected
                Surface(
                    modifier = Modifier
                        .width(172.wpx)
                        .height(MinTouchHeight - 16.wpx)
                        .selectable(selected = isSelected, onClick = { onSelect(mode) }, role = Role.Tab),
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = stringResource(label), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** SCR-004 Landscape: Top 3 가로 카드 */
@Composable
private fun RankCards(ranking: List<RankItemUi>) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.wpx)) {
        ranking.forEach { item ->
            val leader = item.rank == 1
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(32.wpx),
                color = if (leader) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = if (leader) BorderStroke(3.wpx, MaterialTheme.colorScheme.primary) else null,
            ) {
                Column(Modifier.padding(44.wpx), verticalArrangement = Arrangement.SpaceBetween) {
                    if (leader) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrownIcon(size = 52.wpx)
                            Spacer(Modifier.width(16.wpx))
                            Text(stringResource(R.string.driving_leader), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(
                            stringResource(CoreUiR.string.rank_n, item.rank),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column {
                        Text(item.name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(24.wpx))
                        Text(formatPrice(item.priceManwon), style = MaterialTheme.typography.displayMedium.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 0.75f), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RankListRow(item: RankItemUi) {
    val leader = item.rank == 1
    Surface(
        modifier = Modifier.fillMaxWidth().height(176.wpx),
        shape = RoundedCornerShape(28.wpx),
        color = if (leader) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
        border = if (leader) BorderStroke(3.wpx, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(Modifier.padding(horizontal = 28.wpx), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(rank = item.rank, size = 60.wpx)
            Column(Modifier.weight(1f).padding(start = 24.wpx)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = MaterialTheme.typography.titleLarge.fontWeight), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = directionText(item.isAhead, item.distanceKm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatPriceShort(item.priceManwon),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = MaterialTheme.typography.headlineLarge.fontWeight),
                color = if (leader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun directionText(ahead: Boolean, km: Double): String =
    stringResource(if (ahead) R.string.driving_ahead else R.string.driving_behind, formatDistance(km))

/** SCR-005 Landscape: 대장 교체 알림. 8초 뒤 자동으로 닫힌다. */
@Composable
private fun LeaderAlertOverlay(alert: LeaderAlertUi) {
    var progress by remember(alert.key) { mutableFloatStateOf(1f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(DrivingViewModel.ALERT_DURATION_MILLIS.toInt(), easing = LinearEasing),
        label = "alert-timeout",
    )
    LaunchedEffect(alert.key) { progress = 0f }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.wpx)
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(36.wpx),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.padding(horizontal = 56.wpx, vertical = 52.wpx)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownIcon(size = 64.wpx)
                    Spacer(Modifier.width(28.wpx))
                    Text(stringResource(R.string.alert_leader_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                    // TTS(F-006)는 아직 연결 전이라 표시만 한다.
                    SpeakerIcon(size = 48.wpx)
                    Spacer(Modifier.width(16.wpx))
                    Text(stringResource(R.string.alert_speaking), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(28.wpx))
                Text(
                    stringResource(R.string.alert_leader_change, alert.previousName, alert.currentName),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(12.wpx))
                Text(
                    stringResource(R.string.alert_leader_detail, formatPrice(alert.priceManwon), formatDistance(alert.distanceKm)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(36.wpx))
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.fillMaxWidth().height(8.wpx),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

private val previewRanking = listOf(
    RankItemUi("a", 1, "새솔 리버뷰", 245_000, 34, LocalDate.of(2026, 10, 21), 2.4, 25.0),
    RankItemUi("b", 2, "은행나무 파크", 221_000, 38, LocalDate.of(2026, 9, 30), 4.1, -40.0),
    RankItemUi("c", 3, "청솔 하이츠", 198_000, 33, LocalDate.of(2026, 10, 12), 6.8, -160.0),
)

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun DrivingRankPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        DrivingScreen(DrivingScreenState(ranking = previewRanking), onIntent = {})
    }
}

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun DrivingMapPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        DrivingScreen(
            DrivingScreenState(
                mode = DrivingMode.MAP,
                ranking = previewRanking,
                interestMatches = listOf(MapPointUi("h", "한빛마을 1단지", 1.2, -70.0)),
            ),
            onIntent = {},
        )
    }
}

@Preview(widthDp = 1187, heightDp = 797, showBackground = true)
@Composable
private fun DrivingAlertPreview() {
    KingOfRealEstateTheme(darkTheme = false) {
        DrivingScreen(
            DrivingScreenState(ranking = previewRanking, alert = LeaderAlertUi(1, "은행나무 파크", "새솔 리버뷰", 245_000, 2.4)),
            onIntent = {},
        )
    }
}
