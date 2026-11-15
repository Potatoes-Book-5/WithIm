package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.port.DriveRecordRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import com.potatoes.book5.realestate.domain.port.RecommendationFeedbackRepository

/**
 * 이 차량에 저장한 관심 조건, 스크랩, 주행 기록을 한 번에 지운다(F-007 규칙 3, SP-001).
 * 동의와 알림 설정은 남긴다. 되돌릴 수 없으므로 화면에서 한 번 더 확인한다.
 */
class DeleteUserDataUseCase(
    private val interestRepository: InterestRepository,
    private val scrapRepository: ScrapRepository,
    private val driveRecordRepository: DriveRecordRepository,
    private val recommendationFeedbackRepository: RecommendationFeedbackRepository,
    private val tourStore: com.potatoes.book5.realestate.domain.port.TourStore? = null,
    private val voiceContext: com.potatoes.book5.realestate.domain.model.appaction.VoiceContextPort? = null,
) {
    suspend operator fun invoke() {
        voiceContext?.invalidate()
        tourStore?.deleteAll()
        interestRepository.clear()
        scrapRepository.deleteAll()
        driveRecordRepository.deleteAll()
        recommendationFeedbackRepository.deleteAll()
    }
}
