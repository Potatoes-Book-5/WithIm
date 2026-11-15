package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.DriveSummary
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ConsentRepository {
    /** 아직 동의 화면에서 결정하지 않았으면 null. */
    suspend fun get(): ConsentSettings?
    suspend fun save(settings: ConsentSettings)
    /** Emits changes to the app's location permission choice. */
    fun observe(): Flow<ConsentSettings?> = flow { emit(get()) }
}

interface InterestRepository {
    /** 저장된 관심 조건. 없거나 건너뛰었으면 null. */
    suspend fun get(): InterestCondition?
    /** 저장된 관심 조건을 관찰한다. 저장할 때마다 새 값을 낸다. */
    fun observe(): Flow<InterestCondition?>
    suspend fun save(condition: InterestCondition)
    suspend fun markSkipped()
    /** 저장된 조건을 지운다. 조건 단계를 마친 상태는 유지한다(F-007 규칙 3). */
    suspend fun clear()
    /** 저장했거나 건너뛰어 관심 조건 단계를 마쳤는지. */
    suspend fun isDecided(): Boolean
}

interface NeighborhoodRepository {
    /** 관심 조건 화면에 보여 줄 추천 동네. */
    suspend fun suggestions(): Outcome<List<Neighborhood>>
}

/** 차량 주변 반경 순위. 실거래가 API(T-005)와 차량 위치(F-001)를 연결하면 구현을 교체한다. */
interface RankingRepository {
    fun observeNearbyRanking(): Flow<RankingSnapshot>
}

/** 기기에 저장한 스크랩(F-008). 구현은 Room(ADR-005). */
interface ScrapRepository {
    fun observeAll(): Flow<List<Scrap>>
    fun observeIsScrapped(apartmentId: String): Flow<Boolean>
    suspend fun getAll(): List<Scrap>
    suspend fun get(apartmentId: String): Scrap?
    suspend fun upsert(scrap: Scrap)
    suspend fun delete(apartmentId: String)
    suspend fun markChangeSeen(apartmentId: String)
    suspend fun deleteAll()
}

/** 단지 상세와 최신 실거래. 실거래가 API(T-005)를 연결하면 구현을 교체한다. */
interface ApartmentRepository {
    suspend fun detail(apartmentId: String): Outcome<ApartmentDetail>
    suspend fun latestDeal(apartmentId: String): Outcome<Deal>
}

/** 개발 빌드에서 새 실거래가 등록된 것처럼 흉내 낸다. */
interface MarketSimulation {
    fun publishNewDeals()
}

/** 주행 규제 상태. 실제 구현은 vehicle/ 패키지의 CarUxRestrictions 어댑터다(DI-001). */
interface DrivingStateProvider {
    val drivingState: Flow<DrivingState>
}

/**
 * 개발 빌드에서 차량 상태를 흉내 내기 위한 포트.
 * 실제 차량 연동이 생기면 개발 도구로만 남긴다.
 */
interface VehicleSimulation {
    fun setDriving(driving: Boolean)
    /** 이동한 것처럼 순위를 바꿔 대장 교체를 일으킨다. */
    fun moveToNextArea()
}

/**
 * 주행 세션 기록과 요약(F-009). 구현은 Room(ADR-005).
 * 진행 중인 세션은 하나뿐이다.
 */
interface DriveRecordRepository {
    /** 닫히지 않은 세션 id. 없으면 null */
    suspend fun openSessionId(): Long?
    suspend fun startSession(at: LocalDateTime, radiusKm: Int): Long
    /** 최종 Top 3를 덮어쓰고, 관심 매물은 더하거나 더 가까운 거리로 바꾸고, 마지막 기록 시각을 갱신한다. */
    suspend fun recordSnapshot(
        sessionId: Long,
        at: LocalDateTime,
        top: List<DriveRank>,
        passed: List<PassedInterest>,
        recommendations: List<DriveRecommendation> = emptyList(),
    )
    /** Persist the exact recommendation that produced a driving notification in this trip summary. */
    suspend fun recordAlertedInterest(at: LocalDateTime, radiusKm: Int, interest: PassedInterest)
    suspend fun recordLeaderChange(sessionId: Long, change: DriveLeaderChange)
    suspend fun recordRound(
        sessionId: Long, at: LocalDateTime,
        round: com.potatoes.book5.realestate.domain.model.MarketRound, top: List<DriveRank>,
    )
    /**
     * 세션을 닫는다. 순위 기록이 없으면 지운다.
     * @param endedAt null이면 마지막 기록 시각으로 닫는다.
     * @return 요약이 만들어졌는지
     */
    suspend fun finishSession(sessionId: Long, endedAt: LocalDateTime?): Boolean
    /** 닫힌 요약을 최근 [keep]건만 남긴다. */
    suspend fun trim(keep: Int)
    fun observeSummary(id: Long): Flow<DriveSummary?>
    /** 가장 최근 요약 id. 없으면 null */
    fun observeLatestId(): Flow<Long?>
    /** 확인하지 않은 가장 최근 요약 id */
    fun observeLatestUnseenId(): Flow<Long?>
    suspend fun markSeen(id: Long)
    /** 진행 중인 세션을 포함해 모든 주행 기록을 지운다. */
    suspend fun deleteAll()
}
