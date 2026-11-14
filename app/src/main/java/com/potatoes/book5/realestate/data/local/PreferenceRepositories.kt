package com.potatoes.book5.realestate.data.local

import android.content.SharedPreferences
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.InterestRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * 동의와 알림 설정을 기기에 저장한다(F-007). 서버로 보내지 않는다.
 * DataStore 도입은 ADR이 필요하므로 현재 환경의 SharedPreferences를 쓴다.
 */
class PreferenceConsentRepository(
    private val prefs: SharedPreferences,
    private val io: CoroutineDispatcher,
) : ConsentRepository {

    override suspend fun get(): ConsentSettings? = withContext(io) {
        if (!prefs.getBoolean(KEY_DECIDED, false)) return@withContext null
        ConsentSettings(
            locationConsented = prefs.getBoolean(KEY_LOCATION, false),
            interestAlertEnabled = prefs.getBoolean(KEY_ALERT_INTEREST, true),
            leaderAlertEnabled = prefs.getBoolean(KEY_ALERT_LEADER, true),
        )
    }

    override suspend fun save(settings: ConsentSettings) = withContext(io) {
        prefs.edit()
            .putBoolean(KEY_DECIDED, true)
            .putBoolean(KEY_LOCATION, settings.locationConsented)
            .putBoolean(KEY_ALERT_INTEREST, settings.interestAlertEnabled)
            .putBoolean(KEY_ALERT_LEADER, settings.leaderAlertEnabled)
            .apply()
    }

    private companion object {
        const val KEY_DECIDED = "consent_decided"
        const val KEY_LOCATION = "location_consented"
        const val KEY_ALERT_INTEREST = "alert_interest"
        const val KEY_ALERT_LEADER = "alert_leader"
    }
}

/** 관심 조건을 기기에 저장한다(F-004). 서버로 보내지 않는다. */
class PreferenceInterestRepository(
    private val prefs: SharedPreferences,
    private val io: CoroutineDispatcher,
) : InterestRepository {

    override suspend fun get(): InterestCondition? = withContext(io) {
        if (!prefs.getBoolean(KEY_SAVED, false)) return@withContext null
        InterestCondition(
            neighborhoods = prefs.getStringSet(KEY_NEIGHBORHOODS, emptySet()).orEmpty()
                .mapNotNull(::decodeNeighborhood).toSet(),
            priceRanges = prefs.getStringSet(KEY_PRICES, emptySet()).orEmpty()
                .mapNotNull { name -> PriceRange.entries.firstOrNull { it.name == name } }.toSet(),
            areaRanges = prefs.getStringSet(KEY_AREAS, emptySet()).orEmpty()
                .mapNotNull { name -> AreaRange.entries.firstOrNull { it.name == name } }.toSet(),
        )
    }

    override suspend fun save(condition: InterestCondition) = withContext(io) {
        prefs.edit()
            .putBoolean(KEY_SAVED, true)
            .putBoolean(KEY_DECIDED, true)
            .putStringSet(KEY_NEIGHBORHOODS, condition.neighborhoods.map(::encodeNeighborhood).toSet())
            .putStringSet(KEY_PRICES, condition.priceRanges.map { it.name }.toSet())
            .putStringSet(KEY_AREAS, condition.areaRanges.map { it.name }.toSet())
            .apply()
    }

    override suspend fun markSkipped() = withContext(io) {
        prefs.edit().putBoolean(KEY_DECIDED, true).apply()
    }

    override suspend fun isDecided(): Boolean = withContext(io) { prefs.getBoolean(KEY_DECIDED, false) }

    private fun encodeNeighborhood(n: Neighborhood) = "${n.code}$SEP${n.name}"

    private fun decodeNeighborhood(raw: String): Neighborhood? {
        val parts = raw.split(SEP, limit = 2)
        return if (parts.size == 2) Neighborhood(parts[0], parts[1]) else null
    }

    private companion object {
        const val SEP = "|"
        const val KEY_SAVED = "interest_saved"
        const val KEY_DECIDED = "interest_decided"
        const val KEY_NEIGHBORHOODS = "interest_neighborhoods"
        const val KEY_PRICES = "interest_prices"
        const val KEY_AREAS = "interest_areas"
    }
}
