package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.fakes.FakeConsentRepository
import com.potatoes.book5.realestate.fakes.FakeInterestRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingUseCasesTest {

    private val agreed = ConsentSettings(locationConsented = true, interestAlertEnabled = true, leaderAlertEnabled = true)

    @Test
    fun `첫 실행이면 동의 화면부터 보여 준다`() = runTest {
        val step = GetOnboardingStepUseCase(FakeConsentRepository(), FakeInterestRepository())()
        assertEquals(OnboardingStep.CONSENT, step)
    }

    @Test
    fun `동의했고 조건을 정하지 않았으면 관심 조건 화면을 보여 준다`() = runTest {
        val step = GetOnboardingStepUseCase(FakeConsentRepository(agreed), FakeInterestRepository())()
        assertEquals(OnboardingStep.INTEREST, step)
    }

    @Test
    fun `위치 동의를 거부하면 관심 조건 없이 온보딩을 마친다`() = runTest {
        val declined = agreed.copy(locationConsented = false)
        val step = GetOnboardingStepUseCase(FakeConsentRepository(declined), FakeInterestRepository())()
        assertEquals(OnboardingStep.DONE, step)
    }

    @Test
    fun `관심 조건을 건너뛴 뒤에는 온보딩을 마친 상태다`() = runTest {
        val step = GetOnboardingStepUseCase(FakeConsentRepository(agreed), FakeInterestRepository(decided = true))()
        assertEquals(OnboardingStep.DONE, step)
    }

    @Test
    fun `빈 관심 조건은 저장하지 않는다`() = runTest {
        val repo = FakeInterestRepository()
        val outcome = SaveInterestConditionUseCase(repo)(InterestCondition())
        assertTrue(outcome is Outcome.Failure && outcome.error is DomainError.Invalid)
        assertNull(repo.stored)
        assertFalse(repo.decided)
    }

    @Test
    fun `조건이 하나라도 있으면 저장한다`() = runTest {
        val repo = FakeInterestRepository()
        val condition = InterestCondition(priceRanges = setOf(PriceRange.FROM_10_TO_15))
        val outcome = SaveInterestConditionUseCase(repo)(condition)
        assertTrue(outcome is Outcome.Success)
        assertEquals(condition, repo.stored)
        assertTrue(repo.decided)
    }

    @Test
    fun `관심 조건을 건너뛰면 관심 매물 알림을 끈다`() = runTest {
        val consent = FakeConsentRepository(agreed)
        val interest = FakeInterestRepository()
        SkipInterestConditionUseCase(interest, consent)()
        assertTrue(interest.decided)
        assertFalse(consent.stored!!.interestAlertEnabled)
        assertTrue(consent.stored!!.leaderAlertEnabled)
    }
}
