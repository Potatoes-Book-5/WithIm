package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository

/**
 * 첫 실행 흐름의 다음 단계를 정한다(S-002).
 * 동의 전이면 동의 화면, 위치에 동의했고 관심 조건을 아직 정하지 않았으면 관심 조건 화면이다.
 * 동의를 거부하면 위치 기반 기능 없이 바로 홈으로 간다.
 */
class GetOnboardingStepUseCase(
    private val consentRepository: ConsentRepository,
    private val interestRepository: InterestRepository,
) {
    suspend operator fun invoke(): OnboardingStep {
        val consent = consentRepository.get() ?: return OnboardingStep.CONSENT
        if (!consent.locationConsented) return OnboardingStep.DONE
        return if (interestRepository.isDecided()) OnboardingStep.DONE else OnboardingStep.INTEREST
    }
}

class SaveConsentUseCase(private val consentRepository: ConsentRepository) {
    suspend operator fun invoke(settings: ConsentSettings) = consentRepository.save(settings)
}

class GetNeighborhoodSuggestionsUseCase(private val neighborhoodRepository: NeighborhoodRepository) {
    suspend operator fun invoke(): Outcome<List<Neighborhood>> = neighborhoodRepository.suggestions()
}

class GetInterestConditionUseCase(private val interestRepository: InterestRepository) {
    suspend operator fun invoke(): InterestCondition? = interestRepository.get()
}

/** 관심 조건을 저장한다. 조건이 하나도 없으면 저장하지 않는다(F-004 규칙 2). */
class SaveInterestConditionUseCase(private val interestRepository: InterestRepository) {
    suspend operator fun invoke(condition: InterestCondition): Outcome<Unit> {
        if (condition.isEmpty) return Outcome.Failure(DomainError.Invalid("empty-condition"))
        interestRepository.save(condition)
        return Outcome.Success(Unit)
    }
}

/** 관심 조건을 건너뛴다. 관심 매물 알림은 꺼진 상태로 시작한다(F-004 예외). */
class SkipInterestConditionUseCase(
    private val interestRepository: InterestRepository,
    private val consentRepository: ConsentRepository,
) {
    suspend operator fun invoke() {
        interestRepository.markSkipped()
        consentRepository.get()?.let { consentRepository.save(it.copy(interestAlertEnabled = false)) }
    }
}
