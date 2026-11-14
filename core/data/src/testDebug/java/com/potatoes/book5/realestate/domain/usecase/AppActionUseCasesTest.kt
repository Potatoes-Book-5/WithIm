package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.data.sample.gleo.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppActionUseCasesTest {
    private val context = AppCallContext("request", "session", "synthetic-profile")
    private val current = AreaTarget(AreaTargetKind.CURRENT_LOCATION)
    @Test fun priceUsesSyntheticFixtureAndExactKrw() = runTest {
        val result = GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository())(current, context)
        val payload = ((result as Outcome.Success).value as ActionResult.Completed).payload
        assertEquals(1_200_000_000L, payload.averagePriceKrw)
        assertEquals(1_550_000_000L, payload.highestPriceKrw)
        assertEquals(5, payload.sampleCount)
        assertTrue(payload.metadata.synthetic)
        assertEquals("synthetic-area-a", payload.area.id)
    }
    @Test fun namedRealAreaNeverReceivesSyntheticPrices() = runTest {
        val result = GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository())(
            AreaTarget(AreaTargetKind.NAMED_AREA, "성수동2가"), context)
        assertEquals(Outcome.Failure(DomainError.Unsupported("real_area_resolution")), result)
    }
    @Test fun missingNamedAreaAsksWithoutCallingRepository() = runTest {
        var calls = 0
        val repository = object : AreaPriceSummaryRepository {
            override suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> {
                calls++; error("Should not be called")
            }
        }
        val result = GetAreaPriceSummaryUseCase(repository)(AreaTarget(AreaTargetKind.NAMED_AREA), context)
        assertEquals(Outcome.Success(ActionResult.NeedsInput("areaQuery")), result)
        assertEquals(0, calls)
    }
    @Test fun currentTargetRejectsConflictingQuery() = runTest {
        val result = GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository())(
            AreaTarget(AreaTargetKind.CURRENT_LOCATION, "가상마을 A"), context)
        assertTrue(result is Outcome.Failure && result.error is DomainError.Invalid)
    }
    @Test fun absentLastContextDoesNotInventAnArea() = runTest {
        val result = GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository())(
            AreaTarget(AreaTargetKind.LAST_REFERENCED_AREA), context)
        assertEquals(Outcome.Success(ActionResult.NeedsInput("areaQuery")), result)
    }
    @Test fun onlyExplicitInjectedLastContextIsUsed() = runTest {
        val area = ResolvedArea("synthetic-last", "가상 이전 지역")
        val result = GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository(area))(
            AreaTarget(AreaTargetKind.LAST_REFERENCED_AREA), context)
        assertEquals(area, ((result as Outcome.Success).value as ActionResult.Completed).payload.area)
    }
    @Test fun dummySaveNeverClaimsPersistenceAndIsRepeatable() = runTest {
        val api = SaveInterestAreaUseCase(DummyInterestAreaRepository())
        val first = api(current, context)
        val payload = ((first as Outcome.Success).value as ActionResult.Completed).payload
        assertFalse(payload.persisted)
        assertEquals(MutationState.NOT_APPLIED, payload.mutationState)
        assertEquals(first, api(current, context.copy(requestId = "another")))
    }
    @Test fun briefingIsDeterministicAndDoesNotMarkRead() = runTest {
        val api = GetDailyBriefingUseCase(DummyDailyBriefingRepository())
        val first = api(context)
        val payload = ((first as Outcome.Success).value as ActionResult.Completed).payload
        assertEquals(3, payload.items.size)
        assertTrue(payload.items.all { it.id.startsWith("synthetic-") })
        assertEquals(first, api(context.copy(requestId = "next-session", appSessionId = "next")))
    }
    @Test fun trustedApplicationContextIsRequired() = runTest {
        val result = GetDailyBriefingUseCase(DummyDailyBriefingRepository())(context.copy(profileScope = ""))
        assertTrue(result is Outcome.Failure && result.error is DomainError.Invalid)
    }
}
