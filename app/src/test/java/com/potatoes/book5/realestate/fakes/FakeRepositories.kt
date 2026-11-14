package com.potatoes.book5.realestate.fakes

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import com.potatoes.book5.realestate.domain.port.NeighborhoodRepository

class FakeConsentRepository(var stored: ConsentSettings? = null) : ConsentRepository {
    override suspend fun get(): ConsentSettings? = stored
    override suspend fun save(settings: ConsentSettings) {
        stored = settings
    }
}

class FakeInterestRepository(
    var stored: InterestCondition? = null,
    var decided: Boolean = stored != null,
) : InterestRepository {
    override suspend fun get(): InterestCondition? = stored
    override suspend fun save(condition: InterestCondition) {
        stored = condition
        decided = true
    }
    override suspend fun markSkipped() {
        decided = true
    }
    override suspend fun isDecided(): Boolean = decided
}

class FakeNeighborhoodRepository(
    private val names: List<String> = listOf("정자동", "수내동"),
    private val fail: Boolean = false,
) : NeighborhoodRepository {
    override suspend fun suggestions(): Outcome<List<Neighborhood>> =
        if (fail) Outcome.Failure(DomainError.Offline)
        else Outcome.Success(names.map { Neighborhood("code-$it", it) })
}
