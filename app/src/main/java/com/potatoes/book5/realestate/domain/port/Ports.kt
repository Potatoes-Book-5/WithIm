package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood

interface ConsentRepository {
    /** 아직 동의 화면에서 결정하지 않았으면 null. */
    suspend fun get(): ConsentSettings?
    suspend fun save(settings: ConsentSettings)
}

interface InterestRepository {
    /** 저장된 관심 조건. 없거나 건너뛰었으면 null. */
    suspend fun get(): InterestCondition?
    suspend fun save(condition: InterestCondition)
    suspend fun markSkipped()
    /** 저장했거나 건너뛰어 관심 조건 단계를 마쳤는지. */
    suspend fun isDecided(): Boolean
}

interface NeighborhoodRepository {
    /** 관심 조건 화면에 보여 줄 추천 동네. */
    suspend fun suggestions(): Outcome<List<Neighborhood>>
}
