package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PublicPricePolicyTest {
    @Test fun publicRouteCannotReadLocationOrPreviousQueryContext() = runTest {
        var reads = 0
        val source = object : AreaPriceSummaryRepository {
            override suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> {
                reads++; return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
            }
        }
        val api = GetAreaPriceSummaryUseCase(source, contextualTargetsAllowed = false)
        val context = AppCallContext("r", "s", "p")
        for (target in listOf(AreaTargetKind.CURRENT_LOCATION, AreaTargetKind.LAST_REFERENCED_AREA))
            assertEquals(Outcome.Failure(DomainError.Unsupported("authenticated_location_route")), api(AreaTarget(target), context))
        assertEquals(0, reads)
        assertEquals(Outcome.Success(ActionResult.NeedsInput("areaQuery")), api(AreaTarget(AreaTargetKind.NAMED_AREA, "서울특별시 강남구"), context))
        assertEquals(1, reads)
    }
}
