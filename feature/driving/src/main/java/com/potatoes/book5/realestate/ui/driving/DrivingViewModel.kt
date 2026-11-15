package com.potatoes.book5.realestate.ui.driving

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.policy.MarketPeriod
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestMatchAlertsUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.RefreshMarketUseCase
import com.potatoes.book5.realestate.domain.error.DomainError
import java.time.LocalDate
import java.time.ZoneId
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapByIdUseCase
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
    private val refreshMarket: RefreshMarketUseCase,
    private val observeInterestAlerts: ObserveInterestMatchAlertsUseCase,
    private val toggleScrapById: ToggleScrapByIdUseCase,
    private val voiceContext: com.potatoes.book5.realestate.domain.model.appaction.VoiceContextPort? = null,
    private val observeScrappedIds: com.potatoes.book5.realestate.domain.usecase.ObserveScrappedIdsUseCase? = null,
    private val ensureScrap: com.potatoes.book5.realestate.domain.usecase.ScrapVoiceTargetUseCase? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DrivingScreenState(mode = savedStateHandle.get<String>(KEY_MODE)?.let(DrivingMode::valueOf) ?: DrivingMode.RANK)
    )
    val state: StateFlow<DrivingScreenState> = _state.asStateFlow()

    private val alertCount get() = alertIds.incrementAndGet()
    private var tradeLookup: Job? = null
    private var savedIds: Set<String> = emptySet()

    private fun loadTradeRanking() {
        if (tradeLookup?.isActive == true) return
        tradeLookup = viewModelScope.launch {
            refreshMarket()
        }
    }

    init {
        observeScrappedIds?.let { observe -> viewModelScope.launch {
            observe().collect { ids ->
                savedIds=ids
                val alert=_state.value.alert as? InterestAlertUi
                if(alert != null && alert.apartmentId in ids)
                    apply(DrivingResult.AlertScrapChanged(alert.key, ScrapButtonState.SAVED))
            }
        } }
        viewModelScope.launch {
            observeNearbyRanking().collect { snapshot ->
                apply(
                    DrivingResult.RankingUpdated(
                        radiusKm = snapshot.radiusKm,
                        ranking = snapshot.top.mapIndexed { i, item -> item.toRankItem(i + 1) },
                        mapRanking = (snapshot.top + snapshot.interestMatches + snapshot.others)
                            .distinctBy { it.apartment.id }
                            .sortedWith(compareByDescending<NearbyApartment> {
                                it.apartment.priceManwon
                            }.thenByDescending { it.apartment.dealDate })
                            .take(5).mapIndexed { i, item -> item.toRankItem(i + 1) },
                        interestMatches = snapshot.interestMatches.map { it.toMapPoint() },
                        others = snapshot.others.map { it.toMapPoint() },
                        vehicle = snapshot.vehicle,
                        mapCenter = snapshot.mapCenter,
                        referenceLabel = snapshot.referenceLabel,
                    )
                )
                val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
                apply(DrivingResult.TradesUpdated(DrivingTradeRankingUi(
                    fromDate = snapshot.periodStart ?: today.minusMonths(MarketPeriod.MONTHS),
                    toDate = snapshot.periodEnd ?: today,
                    loading = snapshot.loading,
                    areaLabel = snapshot.neighborhoodName,
                    error = snapshot.error?.let { error -> when (error) {
                        DomainError.MissingApiKey -> "API 키 설정을 확인해 주세요"
                        DomainError.Offline -> "네트워크에 연결할 수 없어요"
                        DomainError.Timeout -> "조회 시간이 초과됐어요"
                        DomainError.InvalidResponse -> "시세 응답을 확인할 수 없어요"
                        DomainError.ProviderRejected -> "데이터 제공처에서 요청을 거절했어요"
                        else -> "실거래를 불러오지 못했어요"
                    } },
                )))
            }
        }
        viewModelScope.launch {
            observeLeaderChanges().collect { change ->
                show(
                    LeaderAlertUi(
                        key = alertCount,
                        previousName = change.previous.name,
                        currentName = change.current.apartment.name,
                        priceManwon = change.current.apartment.priceManwon,
                        distanceKm = change.current.distanceKm,
                        kind = change.kind,
                        districtName = change.districtName,
                    )
                )
            }
        }
        viewModelScope.launch {
            observeInterestAlerts().collect { event ->
                val apartment = event.match.apartment
                apply(DrivingResult.RecommendationLocated(event.match.toMapPoint()))
                show(
                    InterestAlertUi(
                        key = alertCount,
                        apartmentId = apartment.id,
                        name = apartment.name,
                        priceManwon = apartment.priceManwon,
                        areaPyeong = apartment.areaPyeong,
                        distanceKm = event.match.distanceKm,
                        bearingDeg = event.match.bearingDeg,
                        directionKnown = event.match.directionKnown,
                    )
                )
            }
        }
    }

    /** 화면 알림은 자동으로 닫는다(F-006 규칙 3). TTS 연동은 이후 작업이다. */
    private fun show(alert: DrivingAlertUi) {
        val previous=_state.value.alert
        if(previous != null) voiceContext?.dismissNudge(previous.key)
        val displayed=if(alert is InterestAlertUi && alert.apartmentId in savedIds)
            alert.copy(scrap=ScrapButtonState.SAVED) else alert
        apply(DrivingResult.AlertShown(displayed))
        if(alert is InterestAlertUi) voiceContext?.showNudge(alert.key,alert.apartmentId,durationOf(alert))
        viewModelScope.launch {
            delay(durationOf(alert))
            voiceContext?.dismissNudge(alert.key)
            apply(DrivingResult.AlertExpired(alert.key))
        }
    }

    private fun scrap(key: Long) {
        val alert = _state.value.alert as? InterestAlertUi ?: return
        if (alert.key != key || alert.scrap != ScrapButtonState.IDLE) return
        apply(DrivingResult.AlertScrapChanged(key, ScrapButtonState.SAVING))
        viewModelScope.launch {
            if(ensureScrap != null) {
                val result=ensureScrap.ensure(alert.apartmentId)
                apply(DrivingResult.AlertScrapChanged(key, if(result is Outcome.Success) ScrapButtonState.SAVED else ScrapButtonState.IDLE))
                return@launch
            }
            var outcome = toggleScrapById(alert.apartmentId)
            // 그사이 다른 화면에서 스크랩해 둔 단지였다면 토글이 해제했으므로 다시 켠다.
            if (outcome is Outcome.Success && !outcome.value) outcome = toggleScrapById(alert.apartmentId)
            val next = if (outcome is Outcome.Success && outcome.value) ScrapButtonState.SAVED else ScrapButtonState.IDLE
            apply(DrivingResult.AlertScrapChanged(key, next))
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
            is DrivingIntent.ScrapAlert -> scrap(intent.key)
        }
    }

    private fun apply(result: DrivingResult) {
        _state.update { reduceDriving(it, result) }
    }

    override fun onCleared() {
        _state.value.alert?.let { voiceContext?.dismissNudge(it.key) }
        super.onCleared()
    }
    companion object {
        private val alertIds=java.util.concurrent.atomic.AtomicLong()
        /** 대장 교체 알림 표시 시간(F-006 규칙 3, 2026-11-14 사용자 결정 10초) */
        const val LEADER_ALERT_DURATION_MILLIS = 10_000L
        /** 관심 매물 배너 표시 시간(F-006 규칙 3, 제안값 8초) */
        const val INTEREST_ALERT_DURATION_MILLIS = 8_000L

        fun durationOf(alert: DrivingAlertUi): Long = when (alert) {
            is LeaderAlertUi -> LEADER_ALERT_DURATION_MILLIS
            is InterestAlertUi -> INTEREST_ALERT_DURATION_MILLIS
        }
        private const val KEY_MODE = "driving_mode"
    }
}
