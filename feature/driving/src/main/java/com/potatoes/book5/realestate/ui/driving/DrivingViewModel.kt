package com.potatoes.book5.realestate.ui.driving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.GetSamsungTradeRankingUseCase
import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import java.time.LocalDate
import java.time.ZoneId
import com.potatoes.book5.realestate.ui.common.toMapPoint
import com.potatoes.book5.realestate.ui.common.toRankItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DrivingViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val observeNearbyRanking: ObserveNearbyRankingUseCase,
    private val observeLeaderChanges: ObserveLeaderChangesUseCase,
    private val getTradeRanking: GetSamsungTradeRankingUseCase,
    private val alertDurationMillis: Long = ALERT_DURATION_MILLIS,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DrivingScreenState(mode = savedStateHandle.get<String>(KEY_MODE)?.let(DrivingMode::valueOf) ?: DrivingMode.RANK)
    )
    val state: StateFlow<DrivingScreenState> = _state.asStateFlow()

    private var alertCount = 0L
    private var tradeLookup: Job? = null

    private fun loadTradeRanking() {
        if (tradeLookup?.isActive == true) return
        tradeLookup = viewModelScope.launch {
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val initial = DrivingTradeRankingUi(today.minusMonths(1), today)
            apply(DrivingResult.TradesUpdated(initial))
            val ranking = when (val result = getTradeRanking(today)) {
                is Outcome.Success -> initial.copy(loading = false, rows = result.value.map {
                    DrivingTradeUi(it.trade.apartmentName ?: "단지명 미제공", it.trade.priceManwon,
                        it.trade.contractDate, it.distanceKm)
                })
                is Outcome.Failure -> initial.copy(loading = false, error = when (result.error) {
                    DomainError.MissingApiKey -> "API 키 설정을 확인해 주세요"
                    DomainError.Offline -> "네트워크에 연결할 수 없어요"
                    DomainError.Timeout -> "조회 시간이 초과됐어요"
                    DomainError.ProviderRejected -> "제공기관이 조회를 거절했어요"
                    else -> "실거래를 불러오지 못했어요"
                })
            }
            apply(DrivingResult.TradesUpdated(ranking))
        }
    }

    init {
        viewModelScope.launch {
            observeNearbyRanking().collect { snapshot ->
                apply(
                    DrivingResult.RankingUpdated(
                        radiusKm = snapshot.radiusKm,
                        ranking = snapshot.top.mapIndexed { i, item -> item.toRankItem(i + 1) },
                        interestMatches = snapshot.interestMatches.map { it.toMapPoint() },
                        others = snapshot.others.map { it.toMapPoint() },
                        vehicle = snapshot.vehicle,
                    )
                )
            }
        }
        viewModelScope.launch {
            observeLeaderChanges().collect { change ->
                val alert = LeaderAlertUi(
                    key = ++alertCount,
                    previousName = change.previous.name,
                    currentName = change.current.apartment.name,
                    priceManwon = change.current.apartment.priceManwon,
                    distanceKm = change.current.distanceKm,
                )
                apply(DrivingResult.AlertShown(alert))
                // 화면 알림은 자동으로 닫는다(F-006 규칙 3, 제안값 8초). TTS 연동은 이후 작업이다.
                launch {
                    delay(alertDurationMillis)
                    apply(DrivingResult.AlertExpired(alert.key))
                }
            }
        }
    }

    fun onIntent(intent: DrivingIntent) {
        when (intent) {
            DrivingIntent.Enter -> loadTradeRanking()
            DrivingIntent.Exit -> {
                tradeLookup?.cancel()
                tradeLookup = null
            }
            is DrivingIntent.SelectMode -> {
                savedStateHandle[KEY_MODE] = intent.mode.name
                apply(DrivingResult.ModeSelected(intent.mode))
            }
        }
    }

    private fun apply(result: DrivingResult) {
        _state.update { reduceDriving(it, result) }
    }

    companion object {
        const val ALERT_DURATION_MILLIS = 8_000L
        private const val KEY_MODE = "driving_mode"
    }
}
