package com.potatoes.book5.realestate.domain.model.tour

import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import java.time.Instant

sealed interface AreaBoundary {
    data class Circle(val center: GeoPoint, val radiusMeters: Double = 500.0) : AreaBoundary
    /** A code is an identifier, not polygon geometry. Membership is unknown without a boundary. */
    data class District(val code: String) : AreaBoundary
}
data class TourArea(val id: String, val name: String, val districtCode: String, val boundary: AreaBoundary,
                    val sourceId: String, val sourceAsOf: Instant, val districtName: String? = null)
data class SavedTourArea(val profile: String, val area: TourArea, val savedAt: Instant, val generation: String = java.util.UUID.randomUUID().toString())
data class AreaSaveResult(val saved: SavedTourArea, val alreadySaved: Boolean)
enum class Membership { INSIDE, OUTSIDE, UNCERTAIN }
data class AreaMembership(val state: Membership, val reason: String, val distanceMeters: Double? = null)
data class AreaEntry(val profile: String, val areaId: String, val session: String, val visit: Long, val at: Instant, val contextRevision: Long = 0, val areaGeneration: String = "")
data class TourCandidate(val id: String, val name: String, val districtCode: String, val neighborhood: String,
                         val point: GeoPoint?, val price: PriceRecord, val areaPyeong: Int,
                         val households: Int?, val builtYear: Int?, val sourceAsOf: Instant, val rankingPrice: PriceRecord = price)
data class TourMarket(val districtCode: String, val candidates: List<TourCandidate>, val sourceAsOf: Instant,
                      val coverage: String = "LOCATED_3KM", val radiusKm: Int = 3, val priceLeaderId: String? = null)
data class RecommendedApartment(val apartment: TourCandidate, val reasons: List<String>)
data class AreaOverview(val area: TourArea, val membership: AreaMembership, val saved: Boolean,
                        val entry: AreaEntry?, val recommendations: List<RecommendedApartment>,
                        val recommendationStatus: String, val priceLeader: TourCandidate?, val marketScope: String?, val savedGeneration: String? = null)
data class TourBriefing(val areaId: String, val areaName: String, val version: String,
                        val averagePriceKrw: Long?, val highestPriceKrw: Long?, val sampleCount: Int,
                        val periodStart: java.time.LocalDate, val periodEndInclusive: java.time.LocalDate,
                        val sourceAsOf: Instant, val observedAt: Instant, val readVersion: String? = null)
data class BriefingReceipt(val itemId: String, val version: String)
enum class EntranceEvidence { UNVERIFIED, USER_CONFIRMED, OFFICIAL_VEHICLE_ENTRY }
data class VerifiedEntrance(val apartmentId: String, val point: GeoPoint, val address: String, val sourceId: String, val evidence: EntranceEvidence = EntranceEvidence.UNVERIFIED,
                            val navigationPoiId: String? = null, val navigationPoiSubId: String? = null)
data class NavigationReceipt(val phase: String = "SUBMITTED", val requestId: String = "")
data class NavigationEvent(val requestId: String, val result: com.potatoes.book5.realestate.domain.error.Outcome<NavigationReceipt>)

/** Inputs after natural-language interpretation. No parser here claims to implement Gleo. */
sealed interface TourIntent {
    data class Price(val target: com.potatoes.book5.realestate.domain.model.appaction.AreaTarget) : TourIntent
    data object Scrap : TourIntent
    data class SaveArea(val query: String? = null, val preferCurrent: Boolean = false) : TourIntent
    data class CheckArea(val query: String? = null) : TourIntent
    data class Recommend(val query: String? = null) : TourIntent
    data class Analytics(val apartmentId: String? = null) : TourIntent
    data class Navigate(val entrance: VerifiedEntrance?, val replaceConfirmed: Boolean = false) : TourIntent
    data class CameraScan(val record: Boolean = false) : TourIntent
    data object AutomaticBriefing : TourIntent
    data object Briefing : TourIntent
    data class ConfirmBriefing(val receipts: List<BriefingReceipt>) : TourIntent
}

sealed interface TourReply {
    data class Price(val value: com.potatoes.book5.realestate.domain.model.appaction.PriceSummaryPayload) : TourReply
    data class SavedArea(val value: AreaSaveResult) : TourReply
    data class Scrapped(val value: com.potatoes.book5.realestate.domain.usecase.VoiceScrapResult) : TourReply
    data class Overview(val value: AreaOverview) : TourReply
    data class Analytics(val value: ApartmentDetail) : TourReply
    data class Navigation(val value: NavigationReceipt) : TourReply
    data class Briefing(val value: com.potatoes.book5.realestate.domain.model.appaction.DailyBriefingPayload) : TourReply
    data class Confirmed(val count: Int) : TourReply
    data object Camera : TourReply
}
