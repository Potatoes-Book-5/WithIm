package com.potatoes.book5.realestate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapSummaryUseCase
import com.potatoes.book5.realestate.ui.Screen
import com.potatoes.book5.realestate.ui.common.toRankItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeNearbyRanking: ObserveNearbyRankingUseCase,
    private val observeScrapSummary: ObserveScrapSummaryUseCase,
    private val observeInterestCondition: ObserveInterestConditionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        // 조건은 관찰해 마이 페이지에서 고친 내용이 홈에 바로 반영된다.
        viewModelScope.launch {
            observeInterestCondition().collect { condition ->
                apply(
                    HomeResult.InterestLoaded(
                        InterestCardUi(
                            neighborhoods = condition?.neighborhoods?.map { it.name }?.sorted().orEmpty(),
                            prices = condition?.priceRanges.orEmpty(),
                            areas = condition?.areaRanges.orEmpty(),
                        )
                    )
                )
            }
        }
        // 스크랩은 DB를 관찰해 상세 화면에서 바꾼 내용이 바로 반영된다.
        viewModelScope.launch {
            observeScrapSummary().collect { scrap ->
                apply(
                    HomeResult.ScrapLoaded(
                        ScrapCardUi(scrap.totalCount, scrap.changedCount, scrap.firstChange?.apartmentName, scrap.firstChange?.kind)
                    )
                )
            }
        }
        viewModelScope.launch {
            observeNearbyRanking().collect { snapshot ->
                apply(
                    HomeResult.RankingLoaded(
                        neighborhoodName = snapshot.neighborhoodName,
                        radiusKm = snapshot.radiusKm,
                        dataAsOf = snapshot.dataAsOf,
                        ranking = snapshot.top.mapIndexed { i, item -> item.toRankItem(i + 1) },
                        todayMatches = snapshot.interestMatches.size,
                    )
                )
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        val effect = when (intent) {
            // SCR-010: 관심 조건 카드도 마이 페이지로 연다.
            HomeIntent.OpenInterest, HomeIntent.OpenMyPage -> HomeEffect.Navigate(Screen.MyPage)
            HomeIntent.OpenScrapChanges -> HomeEffect.Navigate(Screen.Scraps(changedOnly = (_state.value.scrap?.changedCount ?: 0) > 0))
            HomeIntent.OpenScraps -> HomeEffect.Navigate(Screen.Scraps())
            is HomeIntent.OpenApartment -> HomeEffect.Navigate(Screen.Detail(intent.id))
            HomeIntent.OpenSettings -> HomeEffect.ShowComingSoon(HomeDestination.SETTINGS)
            HomeIntent.OpenDriveSummary -> HomeEffect.ShowComingSoon(HomeDestination.DRIVE_SUMMARY)
        }
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun apply(result: HomeResult) {
        _state.update { reduceHome(it, result) }
    }
}
