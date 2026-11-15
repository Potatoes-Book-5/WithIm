package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.DrivingAlertSpeechKind
import com.potatoes.book5.realestate.domain.port.DrivingAlertSpeech

class AnnounceDrivingAlertUseCase(
    private val consent: ConsentRepository,
    private val speech: DrivingAlertSpeech,
) {
    suspend operator fun invoke(key: Long, apartmentName: String, kind: DrivingAlertSpeechKind = DrivingAlertSpeechKind.RECOMMENDATION, stillVisible: () -> Boolean) {
        val settings = consent.get()
        val enabled = when (kind) {
            DrivingAlertSpeechKind.RECOMMENDATION -> settings?.interestAlertEnabled == true
            DrivingAlertSpeechKind.LEADER -> settings?.leaderAlertEnabled == true
        }
        if (settings?.locationConsented == true && enabled && stillVisible()) {
            speech.announce(key, apartmentName, kind)
        }
    }

    fun stop(key: Long? = null) = speech.stop(key)
}
