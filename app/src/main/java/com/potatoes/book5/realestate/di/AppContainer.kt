package com.potatoes.book5.realestate.di

import android.content.Context
import com.potatoes.book5.realestate.BuildConfig
import com.potatoes.book5.realestate.data.remote.MolitRepositoryFactory
import com.potatoes.book5.realestate.data.remote.KaptRepositoryFactory
import com.potatoes.book5.realestate.data.remote.VworldGeocoderFactory
import com.potatoes.book5.realestate.data.remote.VworldDistrictResolverFactory
import com.potatoes.book5.realestate.domain.usecase.GetApartmentTradesUseCase
import com.potatoes.book5.realestate.data.local.PreferenceConsentRepository
import com.potatoes.book5.realestate.data.local.PreferenceInterestRepository
import com.potatoes.book5.realestate.data.repository.LiveMarketRepository
import com.potatoes.book5.realestate.data.local.LegacySampleCleanup
import com.potatoes.book5.realestate.data.local.RoomDriveRecordRepository
import com.potatoes.book5.realestate.data.local.RoomScrapRepository
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import com.potatoes.book5.realestate.domain.port.VehicleSimulation
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.GetNeighborhoodSuggestionsUseCase
import com.potatoes.book5.realestate.domain.usecase.GetOnboardingStepUseCase
import com.potatoes.book5.realestate.domain.usecase.DeleteUserDataUseCase
import com.potatoes.book5.realestate.domain.usecase.DriveSessionRecorder
import com.potatoes.book5.realestate.domain.usecase.GetConsentUseCase
import com.potatoes.book5.realestate.domain.usecase.GetApartmentDetailUseCase
import com.potatoes.book5.realestate.domain.usecase.MarkDriveSummarySeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDriveSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrappedIdsUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveUnseenDriveSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateDriveEndUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapByIdUseCase
import com.potatoes.book5.realestate.domain.usecase.GetScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.MarkScrapChangeSeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveIsScrappedUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveMyPageUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapsUseCase
import com.potatoes.book5.realestate.domain.usecase.RefreshScrapChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.RefreshMarketUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDrivingStateUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestMatchAlertsUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveConsentUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateDrivingUseCase
import com.potatoes.book5.realestate.domain.usecase.SkipInterestConditionUseCase
import com.potatoes.book5.realestate.vehicle.location.PleosFusedLocationBridge
import com.potatoes.book5.realestate.vehicle.simulation.SimulatedDrivingStateProvider
import com.potatoes.book5.realestate.vehicle.location.VehicleLocationInput
import com.potatoes.book5.realestate.domain.usecase.GearDrivingStateProvider
import com.potatoes.book5.realestate.vehicle.gear.CarGearSource
import com.potatoes.book5.realestate.vehicle.restriction.CarUxRestrictionSource
import com.potatoes.book5.realestate.domain.usecase.ObserveUxRestrictedUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 구현체를 수동으로 조립한다(docs/ARCHITECTURE.md 의존성 주입).
 * 주행 상태는 AAOS 기어(GEAR_SELECTION)로 정하고, 기어를 읽지 못하면 개발용 수동 전환을 따른다(T-034).
 */
class AppContainer(context: Context) {
    val getApartmentTrades: GetApartmentTradesUseCase by lazy {
        GetApartmentTradesUseCase(MolitRepositoryFactory.create(BuildConfig.MOLIT_API_KEY))
    }
    private val prefs = context.getSharedPreferences("im_with_you", Context.MODE_PRIVATE)

    private val consentRepository: ConsentRepository = PreferenceConsentRepository(prefs, Dispatchers.IO)
    val consentChanges get() = consentRepository.observe()
    private val interestRepository: InterestRepository = PreferenceInterestRepository(prefs, Dispatchers.IO)
    val vehicleLocationProvider by lazy { VehicleLocationInput(consentRepository) }
    /** Pleos FusedLocation → [vehicleLocationProvider](T-029). RealEstateApplication이 시작한다. */
    val fusedLocationBridge by lazy { PleosFusedLocationBridge(context, vehicleLocationProvider, consentRepository) }
    val marketRepository by lazy {
        LiveMarketRepository(MolitRepositoryFactory.create(BuildConfig.MOLIT_API_KEY),
            VworldGeocoderFactory.create(BuildConfig.VWORLD_API_KEY), interestRepository, vehicleLocationProvider,
            VworldDistrictResolverFactory.create(BuildConfig.VWORLD_API_KEY),
            householdRepository = KaptRepositoryFactory.create(BuildConfig.KAPT_API_KEY))
    }
    private val neighborhoodRepository get() = marketRepository
    private val rankingRepository get() = marketRepository
    private val database = AppDatabase.create(context)
    private val scrapRepository: ScrapRepository = RoomScrapRepository(database.scrapDao())
    private val apartmentRepository get() = marketRepository
    private val driveRecordRepository = RoomDriveRecordRepository(database)
    /** 개발 빌드의 수동 전환. 기어를 읽지 못하는 환경의 대체 흐름이기도 하다(T-034). */
    private val simulatedDrivingState = SimulatedDrivingStateProvider()
    /** 컨테이너는 프로세스 동안 산다. 기어 연결을 주행 기록과 화면이 함께 쓰게 공유하는 범위다. */
    private val vehicleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** 기어(AAOS GEAR_SELECTION) P면 주행 완료, 그 밖의 기어면 주행 중(T-034). */
    private val drivingStateProvider = GearDrivingStateProvider(CarGearSource(context), simulatedDrivingState, vehicleScope)

    private val vehicleSimulation = object : VehicleSimulation {
        override fun setDriving(driving: Boolean) = simulatedDrivingState.setDriving(driving)
        override fun moveToNextArea() = Unit
    }

    val getOnboardingStep = GetOnboardingStepUseCase(consentRepository, interestRepository)
    val saveConsent = SaveConsentUseCase(consentRepository)
    val getNeighborhoodSuggestions = GetNeighborhoodSuggestionsUseCase(neighborhoodRepository)
    val getInterestCondition = GetInterestConditionUseCase(interestRepository)
    val saveInterestCondition = SaveInterestConditionUseCase(interestRepository)
    val skipInterestCondition = SkipInterestConditionUseCase(interestRepository, consentRepository)

    val observeNearbyRanking = ObserveNearbyRankingUseCase(rankingRepository)
    val observeDrivingState = ObserveDrivingStateUseCase(drivingStateProvider)
    /** 플랫폼 주행 규제(T-035). 규제 중에는 DO 화면만 보여 준다. */
    val observeUxRestricted = ObserveUxRestrictedUseCase(CarUxRestrictionSource(context))
    val observeLeaderChanges = ObserveLeaderChangesUseCase(rankingRepository, consentRepository)
    val observeInterestAlerts = ObserveInterestMatchAlertsUseCase(
        rankingRepository,
        consentRepository,
        scrapRepository,
        interestRepository = interestRepository,
        driveRecordRepository = driveRecordRepository,
    )

    val simulateDriving = SimulateDrivingUseCase(vehicleSimulation)
    val refreshMarket = RefreshMarketUseCase(marketRepository)

    val observeScraps = ObserveScrapsUseCase(scrapRepository)
    val observeScrapSummary = ObserveScrapSummaryUseCase(scrapRepository)
    val observeIsScrapped = ObserveIsScrappedUseCase(scrapRepository)
    val getScrap = GetScrapUseCase(scrapRepository)
    val getApartmentDetail = GetApartmentDetailUseCase(apartmentRepository)
    val toggleScrap = ToggleScrapUseCase(scrapRepository)
    val markScrapChangeSeen = MarkScrapChangeSeenUseCase(scrapRepository)
    val refreshScrapChanges = RefreshScrapChangesUseCase(scrapRepository, apartmentRepository)

    val observeInterestCondition = ObserveInterestConditionUseCase(interestRepository)
    val observeMyPage = ObserveMyPageUseCase(interestRepository, scrapRepository)

    /** 앱 프로세스 동안 주행 세션을 기록한다. RealEstateApplication이 시작한다. */
    val driveSessionRecorder = DriveSessionRecorder(
        drivingStateProvider,
        rankingRepository,
        driveRecordRepository,
        interestRepository = interestRepository,
    )
    val observeDriveSummary = ObserveDriveSummaryUseCase(driveRecordRepository)
    val observeUnseenDriveSummary = ObserveUnseenDriveSummaryUseCase(driveRecordRepository)
    val markDriveSummarySeen = MarkDriveSummarySeenUseCase(driveRecordRepository)
    val observeScrappedIds = ObserveScrappedIdsUseCase(scrapRepository)
    val toggleScrapById = ToggleScrapByIdUseCase(apartmentRepository, toggleScrap)
    val simulateDriveEnd = SimulateDriveEndUseCase(vehicleSimulation, driveSessionRecorder)

    val getConsent = GetConsentUseCase(consentRepository)
    val deleteUserData = DeleteUserDataUseCase(interestRepository, scrapRepository, driveRecordRepository)

    suspend fun prepareRealData() {
        LegacySampleCleanup(database)()
        interestRepository.get()?.let { condition ->
            val liveNeighborhoods = condition.neighborhoods.filterNot { it.code.startsWith("sample-") }.toSet()
            if (liveNeighborhoods != condition.neighborhoods) interestRepository.save(condition.copy(neighborhoods = liveNeighborhoods))
        }
    }
}
