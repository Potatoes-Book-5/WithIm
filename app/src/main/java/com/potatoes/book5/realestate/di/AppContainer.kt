package com.potatoes.book5.realestate.di

import android.content.Context
import com.potatoes.book5.realestate.data.local.PreferenceConsentRepository
import com.potatoes.book5.realestate.data.local.PreferenceInterestRepository
import com.potatoes.book5.realestate.data.local.SampleNeighborhoodRepository
import com.potatoes.book5.realestate.data.sample.SampleRankingRepository
import com.potatoes.book5.realestate.data.local.RoomScrapRepository
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.data.sample.SampleApartmentRepository
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import com.potatoes.book5.realestate.domain.port.VehicleSimulation
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.GetNeighborhoodSuggestionsUseCase
import com.potatoes.book5.realestate.domain.usecase.GetOnboardingStepUseCase
import com.potatoes.book5.realestate.domain.usecase.GetApartmentDetailUseCase
import com.potatoes.book5.realestate.domain.usecase.GetScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.MarkScrapChangeSeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveIsScrappedUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveMyPageUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapsUseCase
import com.potatoes.book5.realestate.domain.usecase.RefreshScrapChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateNewDealsUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDrivingStateUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveConsentUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateAreaChangeUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateDrivingUseCase
import com.potatoes.book5.realestate.domain.usecase.SkipInterestConditionUseCase
import com.potatoes.book5.realestate.vehicle.simulation.SimulatedDrivingStateProvider
import kotlinx.coroutines.Dispatchers

/**
 * 구현체를 수동으로 조립한다(docs/ARCHITECTURE.md 의존성 주입).
 * 실거래가 API, 지도 SDK, 주행 규제가 연결되면 아래 샘플·시뮬레이션 구현만 교체한다.
 */
class AppContainer(context: Context) {
    private val prefs = context.getSharedPreferences("im_with_you", Context.MODE_PRIVATE)

    private val consentRepository: ConsentRepository = PreferenceConsentRepository(prefs, Dispatchers.IO)
    private val interestRepository: InterestRepository = PreferenceInterestRepository(prefs, Dispatchers.IO)
    private val neighborhoodRepository: NeighborhoodRepository = SampleNeighborhoodRepository()
    private val rankingRepository = SampleRankingRepository()
    private val database = AppDatabase.create(context)
    private val scrapRepository: ScrapRepository = RoomScrapRepository(database.scrapDao())
    private val apartmentRepository = SampleApartmentRepository()
    private val drivingStateProvider = SimulatedDrivingStateProvider()

    private val vehicleSimulation = object : VehicleSimulation {
        override fun setDriving(driving: Boolean) = drivingStateProvider.setDriving(driving)
        override fun moveToNextArea() = rankingRepository.moveToNextArea()
    }

    val getOnboardingStep = GetOnboardingStepUseCase(consentRepository, interestRepository)
    val saveConsent = SaveConsentUseCase(consentRepository)
    val getNeighborhoodSuggestions = GetNeighborhoodSuggestionsUseCase(neighborhoodRepository)
    val getInterestCondition = GetInterestConditionUseCase(interestRepository)
    val saveInterestCondition = SaveInterestConditionUseCase(interestRepository)
    val skipInterestCondition = SkipInterestConditionUseCase(interestRepository, consentRepository)

    val observeNearbyRanking = ObserveNearbyRankingUseCase(rankingRepository)
    val observeDrivingState = ObserveDrivingStateUseCase(drivingStateProvider)
    val observeLeaderChanges = ObserveLeaderChangesUseCase(rankingRepository, consentRepository)

    val simulateDriving = SimulateDrivingUseCase(vehicleSimulation)
    val simulateAreaChange = SimulateAreaChangeUseCase(vehicleSimulation)

    val observeScraps = ObserveScrapsUseCase(scrapRepository)
    val observeScrapSummary = ObserveScrapSummaryUseCase(scrapRepository)
    val observeIsScrapped = ObserveIsScrappedUseCase(scrapRepository)
    val getScrap = GetScrapUseCase(scrapRepository)
    val getApartmentDetail = GetApartmentDetailUseCase(apartmentRepository)
    val toggleScrap = ToggleScrapUseCase(scrapRepository)
    val markScrapChangeSeen = MarkScrapChangeSeenUseCase(scrapRepository)
    val refreshScrapChanges = RefreshScrapChangesUseCase(scrapRepository, apartmentRepository)
    val simulateNewDeals = SimulateNewDealsUseCase(apartmentRepository, refreshScrapChanges)

    val observeInterestCondition = ObserveInterestConditionUseCase(interestRepository)
    val observeMyPage = ObserveMyPageUseCase(interestRepository, scrapRepository)
}
