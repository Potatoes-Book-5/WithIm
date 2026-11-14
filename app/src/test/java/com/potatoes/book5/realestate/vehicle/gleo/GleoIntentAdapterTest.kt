package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.data.sample.gleo.*
import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import com.potatoes.book5.realestate.domain.usecase.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GleoIntentAdapterTest {
    private fun adapter(sent: MutableList<GleoCallback>) = GleoIntentAdapter(
        GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository()),
        SaveInterestAreaUseCase(DummyInterestAreaRepository()),
        GetDailyBriefingUseCase(DummyDailyBriefingRepository()),
        { sent += it }, { AppCallContext("r", "s", "synthetic-profile") },
    )
    @Test fun dispatchesAllThreeActionsAndDoesNotClaimSaved() = runTest {
        val sent = mutableListOf<GleoCallback>(); val adapter = adapter(sent)
        adapter.handle(GleoInput(GleoAction.GET_AREA_PRICE_SUMMARY, mapOf("target" to "CURRENT_LOCATION")))
        adapter.handle(GleoInput(GleoAction.SAVE_INTEREST_AREA, mapOf("target" to "CURRENT_LOCATION")))
        adapter.handle(GleoInput(GleoAction.GET_REAL_ESTATE_BRIEFING))
        assertEquals(listOf("success", "fail", "success"), sent.map { it.status })
        assertEquals(false, sent[1].data.single()["persisted"])
        assertEquals("DUMMY_NOT_PERSISTED", sent[1].data.single()["code"])
        assertEquals(3, sent[2].data.size)
        assertTrue(sent.all { callback -> callback.data.all { it["synthetic"] == true } })
    }
    @Test fun missingTargetAsksWhileMalformedWireFails() = runTest {
        val sent = mutableListOf<GleoCallback>(); val adapter = adapter(sent)
        adapter.handle(GleoInput(GleoAction.SAVE_INTEREST_AREA))
        adapter.handle(GleoInput(GleoAction.SAVE_INTEREST_AREA, decodingError = "MALFORMED_REQUEST"))
        assertEquals(listOf("asking", "fail"), sent.map { it.status })
    }
    @Test fun realAreaIsUnsupportedInsteadOfReceivingFakePrices() = runTest {
        val sent = mutableListOf<GleoCallback>()
        adapter(sent).handle(GleoInput(GleoAction.GET_AREA_PRICE_SUMMARY,
            mapOf("target" to "NAMED_AREA", "areaQuery" to "성수동2가")))
        assertEquals("fail", sent.single().status)
        assertEquals("UNSUPPORTED", sent.single().data.single()["code"])
        assertFalse(sent.single().data.single().containsKey("averagePriceKrw"))
    }
}
