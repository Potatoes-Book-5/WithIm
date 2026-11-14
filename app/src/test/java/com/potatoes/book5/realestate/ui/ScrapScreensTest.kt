package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.domain.usecase.GetApartmentDetailUseCase
import com.potatoes.book5.realestate.domain.usecase.GetScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.MarkScrapChangeSeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveIsScrappedUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapsUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import com.potatoes.book5.realestate.fakes.FakeApartmentRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.dealOf
import com.potatoes.book5.realestate.fakes.detailOf
import com.potatoes.book5.realestate.fakes.scrapOf
import com.potatoes.book5.realestate.ui.detail.DetailIntent
import com.potatoes.book5.realestate.ui.detail.DetailViewModel
import com.potatoes.book5.realestate.ui.detail.deltaFor
import com.potatoes.book5.realestate.ui.scrap.ScrapFilter
import com.potatoes.book5.realestate.ui.scrap.ScrapListEffect
import com.potatoes.book5.realestate.ui.scrap.ScrapListIntent
import com.potatoes.book5.realestate.ui.scrap.ScrapListViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScrapScreensTest {

    private val dispatcher = StandardTestDispatcher()
    private val oct1 = LocalDate.of(2026, 10, 1)
    private val newDeal = PriceChange(ScrapChangeKind.NEW_DEAL, 3_000, LocalDateTime.of(2026, 11, 14, 9, 0), seen = false)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun detailVm(id: String, scraps: FakeScrapRepository, apartments: FakeApartmentRepository) = DetailViewModel(
        apartmentId = id,
        getApartmentDetail = GetApartmentDetailUseCase(apartments),
        getScrap = GetScrapUseCase(scraps),
        observeIsScrapped = ObserveIsScrappedUseCase(scraps),
        toggleScrap = ToggleScrapUseCase(scraps),
        markScrapChangeSeen = MarkScrapChangeSeenUseCase(scraps),
    )

    @Test
    fun `스크랩 목록은 필터로 변동 단지만 보여 주고 항목을 누르면 상세를 연다`() = runTest(dispatcher) {
        val repo = FakeScrapRepository(listOf(scrapOf("a"), scrapOf("b", change = newDeal)))
        val vm = ScrapListViewModel(ScrapFilter.CHANGED, ObserveScrapsUseCase(repo))
        runCurrent()
        assertEquals(listOf("b"), vm.state.value.visibleItems.map { it.id })
        assertEquals(2, vm.state.value.totalCount)
        assertEquals(1, vm.state.value.changedCount)

        vm.onIntent(ScrapListIntent.SelectFilter(ScrapFilter.ALL))
        assertEquals(listOf("b", "a"), vm.state.value.visibleItems.map { it.id })

        vm.onIntent(ScrapListIntent.OpenItem("a"))
        assertEquals(ScrapListEffect.OpenDetail("a"), vm.effect.first())

        // 상세에서 해제하면 목록에서 바로 빠진다.
        repo.delete("b")
        runCurrent()
        assertEquals(listOf("a"), vm.state.value.visibleItems.map { it.id })
    }

    @Test
    fun `상세를 열면 변동을 보여 주고 확인 처리한다`() = runTest(dispatcher) {
        val scraps = FakeScrapRepository(listOf(scrapOf("a", change = newDeal)))
        val apartments = FakeApartmentRepository(mutableMapOf("a" to dealOf(153_000, oct1.plusDays(7))))
        val vm = detailVm("a", scraps, apartments)
        runCurrent()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isScrapped)
        assertEquals(ScrapChangeKind.NEW_DEAL, state.change)
        assertEquals(3_000L, state.deltaManwon)
        assertEquals(true, scraps.get("a")!!.change?.seen)
    }

    @Test
    fun `상세에서 스크랩을 켜고 끈다`() = runTest(dispatcher) {
        val scraps = FakeScrapRepository()
        val apartments = FakeApartmentRepository(mutableMapOf("a" to dealOf(153_000)))
        val vm = detailVm("a", scraps, apartments)
        runCurrent()
        assertFalse(vm.state.value.isScrapped)

        vm.onIntent(DetailIntent.ToggleScrap)
        runCurrent()
        assertTrue(vm.state.value.isScrapped)
        assertEquals(153_000L, scraps.get("a")!!.baseline.priceManwon)

        vm.onIntent(DetailIntent.ToggleScrap)
        runCurrent()
        assertFalse(vm.state.value.isScrapped)
        assertNull(scraps.get("a"))
    }

    @Test
    fun `상세를 못 불러오면 실패 상태가 되고 다시 시도할 수 있다`() = runTest(dispatcher) {
        val apartments = FakeApartmentRepository(mutableMapOf("a" to dealOf(153_000)), failing = setOf("a"))
        val vm = detailVm("a", FakeScrapRepository(), apartments)
        runCurrent()
        assertTrue(vm.state.value.failed)

        apartments.failing = emptySet()
        vm.onIntent(DetailIntent.Retry)
        runCurrent()
        assertFalse(vm.state.value.failed)
        assertEquals("a", vm.state.value.detail?.id)
    }

    @Test
    fun `직전 대비는 같은 평형 거래끼리만 비교한다`() {
        val latest = dealOf(158_000, oct1, pyeong = 34)
        assertEquals(3_000L, deltaFor(detailOf("a", latest, dealOf(155_000, oct1.minusDays(30), pyeong = 34)), null))
        assertNull(deltaFor(detailOf("a", latest, dealOf(112_000, oct1.minusDays(30), pyeong = 24)), null))
        assertNull(deltaFor(detailOf("a", latest), null))
        // 스크랩 변동이 있으면 마지막 확인 값 대비 차이를 쓴다.
        assertEquals(3_000L, deltaFor(detailOf("a", latest, dealOf(112_000, pyeong = 24)), newDeal))
    }
}
