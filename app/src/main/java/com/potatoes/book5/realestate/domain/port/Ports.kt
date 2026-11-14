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
import kotlinx.coroutines.flow.Flow

interface ConsentRepository {
    /** 아직 동의 화면에서 결정하지 않았으면 null. */
    suspend fun get(): ConsentSettings?
    suspend fun save(settings: ConsentSettings)
}

interface InterestRepository {
    /** 저장된 관심 조건. 없거나 건너뛰었으면 null. */
    suspend fun get(): InterestCondition?
    /** 저장된 관심 조건을 관찰한다. 저장할 때마다 새 값을 낸다. */
    fun observe(): Flow<InterestCondition?>
    suspend fun save(condition: InterestCondition)
    suspend fun markSkipped()
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
