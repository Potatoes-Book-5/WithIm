package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.DrivingAlertSpeech
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnnounceDrivingAlertUseCaseTest {
    private class Speech : DrivingAlertSpeech {
        val spoken = mutableListOf<Pair<Long, String>>()
        val stopped = mutableListOf<Long?>()
        override fun announce(key: Long, apartmentName: String, kind: com.potatoes.book5.realestate.domain.port.DrivingAlertSpeechKind) { spoken += key to apartmentName }
        override fun stop(key: Long?) { stopped += key }
    }

    private fun consent(value: ConsentSettings?) = object : ConsentRepository {
        override suspend fun get() = value
        override suspend fun save(settings: ConsentSettings) = Unit
    }

    @Test fun onlyConsentedEnabledVisibleRecommendationsAreSpoken() = runTest {
        val speech = Speech()
        for (settings in listOf(null, ConsentSettings(false, true, true), ConsentSettings(true, false, true))) {
            AnnounceDrivingAlertUseCase(consent(settings), speech)(1, "Synthetic apartment") { true }
        }
        val useCase = AnnounceDrivingAlertUseCase(consent(ConsentSettings(true, true, true)), speech)
        useCase(1, "Hidden apartment") { false }
        useCase(2, "Visible apartment") { true }
        assertEquals(listOf(2L to "Visible apartment"), speech.spoken)
        useCase.stop(2)
        useCase.stop()
        assertEquals(listOf(2L, null), speech.stopped)
    }

    @Test fun leaderAndRecommendationUseTheirOwnSettings() = runTest {
        val speech = Speech()
        val leaderOnly = AnnounceDrivingAlertUseCase(consent(ConsentSettings(true, false, true)), speech)
        val recommendationOnly = AnnounceDrivingAlertUseCase(consent(ConsentSettings(true, true, false)), speech)
        val leader = com.potatoes.book5.realestate.domain.port.DrivingAlertSpeechKind.LEADER
        leaderOnly(1, "Recommendation") { true }
        leaderOnly(2, "Leader", leader) { true }
        recommendationOnly(3, "Leader disabled", leader) { true }
        recommendationOnly(4, "Recommendation enabled") { true }
        assertEquals(listOf(2L to "Leader", 4L to "Recommendation enabled"), speech.spoken)
    }

    @Test fun bannerHiddenDuringConsentReadIsNeverQueuedForSpeech() = runTest {
        val read = CompletableDeferred<Unit>()
        var visible = true
        val repository = object : ConsentRepository {
            override suspend fun get(): ConsentSettings { read.await(); return ConsentSettings(true, true, true) }
            override suspend fun save(settings: ConsentSettings) = Unit
        }
        val speech = Speech()
        val job = launch { AnnounceDrivingAlertUseCase(repository, speech)(1, "Synthetic apartment") { visible } }
        runCurrent()
        visible = false
        read.complete(Unit)
        job.join()
        assertEquals(emptyList<Pair<Long, String>>(), speech.spoken)
    }
}
