package com.potatoes.book5.realestate.ui.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.common.formatDistance
import com.potatoes.book5.realestate.ui.common.formatPrice
import com.potatoes.book5.realestate.ui.theme.wpx
import java.time.format.DateTimeFormatter

@Composable
internal fun DrivingTradeTable(ranking: DrivingTradeRankingUi?, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(32.wpx), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(24.wpx), verticalArrangement = Arrangement.spacedBy(8.wpx)) {
            Text(stringResource(R.string.driving_trade_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.driving_trade_reference), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ranking?.let {
                Text("${it.fromDate} ~ ${it.toDate}", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            when {
                ranking == null || ranking.loading -> Text(stringResource(R.string.driving_trade_loading), style = MaterialTheme.typography.bodyMedium)
                ranking.error != null -> Text(ranking.error, style = MaterialTheme.typography.bodyMedium)
                ranking.rows.isEmpty() -> Text(stringResource(R.string.driving_trade_empty), style = MaterialTheme.typography.bodyMedium)
                else -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.driving_trade_rank), Modifier.width(56.wpx), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.driving_trade_apartment), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(R.string.driving_trade_amount), style = MaterialTheme.typography.labelSmall)
                    }
                    ranking.rows.forEachIndexed { index, trade ->
                        Row(Modifier.fillMaxWidth().height(116.wpx), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", Modifier.width(56.wpx), style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.wpx)) {
                                Text(trade.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(trade.contractDate.format(DateTimeFormatter.ofPattern("MM.dd")) + " · " + formatDistance(trade.distanceKm),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(12.wpx))
                            Text(formatPrice(trade.priceManwon), style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                }
            }
            Text(stringResource(R.string.driving_trade_basis), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
