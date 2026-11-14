package com.potatoes.book5.realestate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.GetScrapSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
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
    private val getScrapSummary: GetScrapSummaryUseCase,
    private val getInterestCondition: GetInterestConditionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            val condition = getInterestCondition()
            apply(
                HomeResult.InterestLoaded(
                    InterestCardUi(
                        neighborhoods = condition?.neighborhoods?.map { it.name }?.sorted().orEmpty(),
                        prices = condition?.priceRanges.orEmpty(),
                        areas = condition?.areaRanges.orEmpty(),
                    )
                )
            )
            val scrap = getScrapSummary()
            apply(
                HomeResult.ScrapLoaded(
                    ScrapCardUi(scrap.totalCount, scrap.changedCount, scrap.firstChange?.apartmentName, scrap.firstChange?.kind)
                )
            )
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
            HomeIntent.OpenInterest -> HomeEffect.OpenInterestConditions
            HomeIntent.OpenMyPage -> HomeEffect.ShowComingSoon(HomeDestination.MY_PAGE)
            HomeIntent.OpenSettings -> HomeEffect.ShowComingSoon(HomeDestination.SETTINGS)
            HomeIntent.OpenScrapChanges, HomeIntent.OpenScraps -> HomeEffect.ShowComingSoon(HomeDestination.SCRAPS)
            HomeIntent.OpenDriveSummary -> HomeEffect.ShowComingSoon(HomeDestination.DRIVE_SUMMARY)
            is HomeIntent.OpenApartment -> HomeEffect.ShowComingSoon(HomeDestination.APARTMENT_DETAIL)
        }
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun apply(result: HomeResult) {
        _state.update { reduceHome(it, result) }
    }
}
