package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.port.ApartmentRepository
import com.potatoes.book5.realestate.domain.port.DriveRecordRepository
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import com.potatoes.book5.realestate.domain.port.VehicleSimulation
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 주행 세션을 기록한다(F-009 규칙 1, 2, 5, SM-009). 앱 프로세스가 살아 있는 동안 계속 실행한다.
 * - Drive: 세션을 열거나 이어 쓰고, 순위 스냅샷마다 저장한다. 대장 교체는 알림 간격과 관계없이 모두 기록한다.
 * - Parking: [endDelay](제안값 3분) 안에 다시 Drive가 되지 않으면 주차한 시각으로 세션을 닫는다.
 * - 시작할 때 이전 실행에서 닫히지 않은 세션은 마지막 기록 시각으로 닫는다.
 */
class DriveSessionRecorder(
    private val drivingStateProvider: DrivingStateProvider,
    private val rankingRepository: RankingRepository,
    private val repository: DriveRecordRepository,
    private val now: () -> LocalDateTime = LocalDateTime::now,
    private val endDelay: Duration = 3.minutes,
    private val keep: Int = 10,
) {
    /** 개발 빌드에서 기다리지 않고 세션을 닫는 신호. 받을 때까지 남아 있다. */
    private val endSignal = Channel<Unit>(Channel.CONFLATED)

    fun endNow() {
        endSignal.trySend(Unit)
    }

    suspend fun run() {
        repository.openSessionId()?.let { finish(it, endedAt = null) }
        var lastLeaderId: String? = null
        drivingStateProvider.drivingState.distinctUntilChanged().collectLatest { state ->
            when (state) {
                DrivingState.DRIVING -> {
                    // 주차 전에 눌린 종료 신호가 다음 주차를 바로 끝내지 않게 비운다.
                    endSignal.tryReceive()
                    var sessionId = repository.openSessionId()
                    if (sessionId == null) lastLeaderId = null
                    rankingRepository.observeNearbyRanking().collect { snapshot ->
                        if (snapshot.top.isEmpty() || snapshot.loading || snapshot.error != null) return@collect
                        val at = now()
                        val id = sessionId ?: repository.startSession(at, snapshot.radiusKm).also { sessionId = it }
                        repository.recordSnapshot(
                            sessionId = id,
                            at = at,
                            top = snapshot.top.mapIndexed { i, n -> DriveRank(i + 1, n.apartment.id, n.apartment.name, n.apartment.priceManwon) },
                            passed = snapshot.interestMatches.map { PassedInterest(it.apartment.id, it.apartment.name, it.distanceKm, it.apartment.priceManwon) },
                        )
                        val leader = snapshot.top.first().apartment
                        if (lastLeaderId != null && lastLeaderId != leader.id) {
                            repository.recordLeaderChange(id, DriveLeaderChange(at, leader.id, leader.name))
                        }
                        lastLeaderId = leader.id
                    }
                }
                DrivingState.PARKED -> {
                    val id = repository.openSessionId() ?: return@collectLatest
                    val parkedAt = now()
                    withTimeoutOrNull(endDelay) { endSignal.receive() }
                    finish(id, parkedAt)
                    lastLeaderId = null
                }
            }
        }
    }

    private suspend fun finish(id: Long, endedAt: LocalDateTime?) {
        if (repository.finishSession(id, endedAt)) repository.trim(keep)
    }
}

/** 요약 하나를 관찰한다. id가 null이면 가장 최근 요약(홈 `지난 주행 요약`). */
class ObserveDriveSummaryUseCase(private val repository: DriveRecordRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(id: Long?): Flow<DriveSummary?> =
        if (id != null) repository.observeSummary(id)
        else repository.observeLatestId().flatMapLatest { latest -> latest?.let(repository::observeSummary) ?: flowOf(null) }
}

/** 확인하지 않은 최근 요약. 주차 중 자동으로 띄운다(F-009 규칙 3, S-009 대체 흐름). */
class ObserveUnseenDriveSummaryUseCase(private val repository: DriveRecordRepository) {
    operator fun invoke(): Flow<Long?> = repository.observeLatestUnseenId().distinctUntilChanged()
}

class MarkDriveSummarySeenUseCase(private val repository: DriveRecordRepository) {
    suspend operator fun invoke(id: Long) = repository.markSeen(id)
}

class ObserveScrappedIdsUseCase(private val scrapRepository: ScrapRepository) {
    operator fun invoke(): Flow<Set<String>> = scrapRepository.observeAll().map { list -> list.map { it.apartmentId }.toSet() }
}

/** 단지 id로 스크랩을 켜고 끈다. 상세를 불러와 기준 값을 저장한다(S-009, 탭 1회). */
class ToggleScrapByIdUseCase(
    private val apartmentRepository: ApartmentRepository,
    private val toggleScrap: ToggleScrapUseCase,
) {
    suspend operator fun invoke(apartmentId: String): Outcome<Boolean> =
        when (val detail = apartmentRepository.detail(apartmentId)) {
            is Outcome.Success -> Outcome.Success(toggleScrap(detail.value))
            is Outcome.Failure -> detail
        }
}

/** 개발 빌드 전용: 주차로 바꾸고 3분을 기다리지 않고 세션을 닫는다. */
class SimulateDriveEndUseCase(
    private val simulation: VehicleSimulation,
    private val recorder: DriveSessionRecorder,
) {
    operator fun invoke() {
        simulation.setDriving(false)
        recorder.endNow()
    }
}
