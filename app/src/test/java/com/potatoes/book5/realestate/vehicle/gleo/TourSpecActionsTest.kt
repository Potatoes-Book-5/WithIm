package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Utterance labels trace approved normalized inputs; this is not a natural-language interpreter test. */
@RunWith(Parameterized::class)
internal class TourSpecActionsTest(private val case: Case) {
    data class Case(val phrase:String,val intent:(TourFixture)->TourIntent,val kind:Class<*>?,val gate:Boolean=false) {
        override fun toString()=phrase
    }
    @Test fun normalizedSpecRequestUsesProductionHandlersAndDataRepositories() = runTest {
        val f=TourFixture();f.prepare(this)
        if(case.intent(f) !is TourIntent.SaveArea) f.saved()
        f.seedBriefing()
        val reply=f.adapter.handle(case.intent(f),f.context())
        if(case.gate) {
            assertTrue(reply is Outcome.Failure)
            assertTrue((reply as Outcome.Failure).error is DomainError.Unsupported)
            assertEquals(0,f.camera.calls)
            return@runTest
        }
        assertTrue("${case.phrase}: $reply",reply is Outcome.Success)
        val result=(reply as Outcome.Success).value
        assertTrue(result is ActionResult.Completed)
        val value=(result as ActionResult.Completed).payload
        assertTrue("${case.phrase}: $value",case.kind!!.isInstance(value))
        when(value) {
            is TourReply.Price -> { assertEquals(2_500_000_000L,value.value.averagePriceKrw);assertFalse(value.value.metadata.synthetic) }
            is TourReply.SavedArea -> assertTrue(f.store.rows.value.any { it.area.id==value.value.saved.area.id })
            is TourReply.Overview -> {
                assertTrue(value.value.membership.state in setOf(Membership.INSIDE,Membership.UNCERTAIN))
                assertEquals("b",value.value.recommendations.single().apartment.id)
                assertEquals("a",value.value.priceLeader!!.id) // Recommendation != price crown.
            }
            is TourReply.Briefing -> {
                assertEquals("PRICE_SUMMARY_UPDATE",value.value.items.single().type)
                assertEquals("UNSUPPORTED",value.value.sourceStates["SUBSCRIPTION"])
                assertFalse(value.value.metadata.synthetic)
                assertNull(value.value.items.single().publishedAt)
                assertNull(f.store.briefings.single().readVersion) // Returning data is not a read receipt.
            }
            is TourReply.Navigation -> { assertEquals("SUBMITTED",value.value.phase);assertEquals(1,f.navigator.calls) }
            is TourReply.Analytics -> assertEquals(f.detail,value.value)
            is TourReply.Confirmed -> assertEquals(1,value.count)
            else -> Unit
        }
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}") fun cases(): List<Array<Case>> = listOf(
            Case("이쪽 동네 집값이 얼마 정도야?",{ TourIntent.Price(AreaTarget(AreaTargetKind.CURRENT_LOCATION)) },TourReply.Price::class.java),
            Case("영통구 집값 알려줘",{ TourIntent.Price(AreaTarget(AreaTargetKind.NAMED_AREA,"영통구")) },TourReply.Price::class.java),
            Case("이 동네 관심 지역으로 저장해 줘",{ TourIntent.SaveArea() },TourReply.SavedArea::class.java),
            Case("지금 현재 위치를 관심 지역으로 저장해 줘",{ TourIntent.SaveArea(preferCurrent=true) },TourReply.SavedArea::class.java),
            Case("광교중앙역 인근 관심 지역으로 저장해 줘",{ TourIntent.SaveArea("광교중앙역") },TourReply.SavedArea::class.java),
            Case("여기가 광교중앙역 인근인 건가?",{ TourIntent.CheckArea("광교중앙역") },TourReply.Overview::class.java),
            Case("지금 내가 저장해 둔 관심 지역에 들어온 거야?",{ TourIntent.CheckArea() },TourReply.Overview::class.java),
            Case("여기서 우리 조건에 맞는 아파트는 어디야?",{ TourIntent.Recommend() },TourReply.Overview::class.java),
            Case("이 단지 스크랩해 줘",{ TourIntent.Scrap },TourReply.Scrapped::class.java),
            Case("이 단지 상세 분석 보여줘",{ TourIntent.Analytics() },TourReply.Analytics::class.java),
            Case("선택한 단지 출입구로 안내해 줘",{ TourIntent.Navigate(it.entrance) },TourReply.Navigation::class.java),
            Case("주변 카메라로 보여줘",{ TourIntent.CameraScan() },null,true),
            Case("관심 지역 새 소식 알려줘",{ TourIntent.Briefing },TourReply.Briefing::class.java),
            Case("브리핑 확인했어",{ TourIntent.ConfirmBriefing(listOf(BriefingReceipt(it.area.id,"v1"))) },TourReply.Confirmed::class.java),
            Case("다음 시동 때 자동 브리핑",{ TourIntent.AutomaticBriefing },null,true),
        ).map { arrayOf(it) }
    }
}
