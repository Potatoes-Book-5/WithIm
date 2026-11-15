package com.potatoes.book5.realestate.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.potatoes.book5.realestate.RealEstateApplication
import com.potatoes.book5.realestate.di.AppContainer
import com.potatoes.book5.realestate.ui.consent.ConsentViewModel
import com.potatoes.book5.realestate.ui.detail.DetailViewModel
import com.potatoes.book5.realestate.ui.driving.DrivingViewModel
import com.potatoes.book5.realestate.ui.home.HomeViewModel
import com.potatoes.book5.realestate.ui.interest.InterestViewModel
import com.potatoes.book5.realestate.ui.mypage.MyPageViewModel
import com.potatoes.book5.realestate.ui.scrap.ScrapFilter
import com.potatoes.book5.realestate.ui.scrap.ScrapListViewModel
import com.potatoes.book5.realestate.ui.summary.DriveSummaryViewModel

/** AppContainer에서 ViewModel을 만든다. */
object AppViewModels {
    /** 화면 인자. 내비게이션 라이브러리 도입 전까지 CreationExtras로 넘긴다. */
    val APARTMENT_ID_KEY = object : CreationExtras.Key<String> {}
    val SCRAP_FILTER_KEY = object : CreationExtras.Key<ScrapFilter> {}
    val INTEREST_EDITING_KEY = object : CreationExtras.Key<Boolean> {}
    val SETTINGS_EDITING_KEY = object : CreationExtras.Key<Boolean> {}
    val DRIVE_SESSION_KEY = object : CreationExtras.Key<Long> {}
    /** DRIVE_SESSION_KEY 값. 가장 최근 요약을 뜻한다. */
    const val LATEST_SESSION = -1L

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val c = container()
            AppViewModel(
                c.getOnboardingStep, c.observeDrivingState, c.refreshScrapChanges, c.simulateDriving,
                c.observeUnseenDriveSummary, c.simulateDriveEnd, c.refreshMarket, c.observeUxRestricted, c.voiceContext,
            )
        }
        initializer {
            val c = container()
            ConsentViewModel(c.saveConsent, c.getConsent, c.deleteUserData, editing = this[SETTINGS_EDITING_KEY] ?: false)
        }
        initializer {
            val c = container()
            InterestViewModel(
                savedStateHandle = createSavedStateHandle(),
                getSuggestions = c.getNeighborhoodSuggestions,
                getInterestCondition = c.getInterestCondition,
                saveInterestCondition = c.saveInterestCondition,
                skipInterestCondition = c.skipInterestCondition,
                editing = this[INTEREST_EDITING_KEY] ?: false,
            )
        }
        initializer {
            val c = container()
            HomeViewModel(c.observeNearbyRanking, c.observeScrapSummary, c.observeInterestCondition,
                onRefresh = { c.refreshMarket() })
        }
        initializer {
            val c = container()
            DrivingViewModel(
                createSavedStateHandle(), c.observeNearbyRanking, c.observeLeaderChanges, c.refreshMarket,
                c.observeInterestAlerts, c.toggleScrapById, c.voiceContext, c.observeScrappedIds, c.scrapVoiceTarget,
            )
        }
        initializer { MyPageViewModel(container().observeMyPage) }
        initializer {
            val c = container()
            DriveSummaryViewModel(
                sessionId = this[DRIVE_SESSION_KEY]?.takeIf { it != LATEST_SESSION },
                observeDriveSummary = c.observeDriveSummary,
                observeScrappedIds = c.observeScrappedIds,
                markSeen = c.markDriveSummarySeen,
                toggleScrapById = c.toggleScrapById,
            )
        }
        initializer {
            ScrapListViewModel(this[SCRAP_FILTER_KEY] ?: ScrapFilter.ALL, container().observeScraps)
        }
        initializer {
            val c = container()
            DetailViewModel(
                apartmentId = checkNotNull(this[APARTMENT_ID_KEY]) { "apartmentId is required" },
                getApartmentDetail = c.getApartmentDetail,
                getScrap = c.getScrap,
                observeIsScrapped = c.observeIsScrapped,
                toggleScrap = c.toggleScrap,
                markScrapChangeSeen = c.markScrapChangeSeen,
            )
        }
    }

    /** 현재 ViewModelStoreOwner의 기본 extras에 화면 인자를 더한다. */
    @Composable
    fun <T> extrasWith(key: CreationExtras.Key<T>, value: T): CreationExtras {
        val base = (LocalViewModelStoreOwner.current as? HasDefaultViewModelProviderFactory)
            ?.defaultViewModelCreationExtras ?: CreationExtras.Empty
        return MutableCreationExtras(base).apply { set(key, value) }
    }

    private fun CreationExtras.container(): AppContainer =
        (checkNotNull(this[APPLICATION_KEY]) as RealEstateApplication).container
}
