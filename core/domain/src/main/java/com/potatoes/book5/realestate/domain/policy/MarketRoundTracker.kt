package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.MarketRound
import com.potatoes.book5.realestate.domain.port.MarketDistrict
import java.time.Duration
import java.time.Instant

/** A district change requires consecutive verified fixes; missing fixes never advance the timer. */
class MarketRoundTracker(
    private val dwell: Duration = Duration.ofSeconds(30),
    private val maxGap: Duration = Duration.ofSeconds(15),
) {
    var current: MarketRound? = null
        private set
    private var candidate: MarketDistrict? = null
    private var since: Instant? = null
    private var lastFix: Instant? = null
    private var sequence = 0L

    @Synchronized
    fun observe(district: MarketDistrict, at: Instant): MarketRound {
        val active = current
        if (active == null) return enter(district, at)
        if (active.districtCode == district.code) {
            interrupt()
            return active
        }
        val gap = lastFix?.let { Duration.between(it, at) }
        if (candidate?.code != district.code || gap == null || gap.isNegative || gap > maxGap) {
            candidate = district
            since = at
        }
        lastFix = at
        return if (Duration.between(since, at) >= dwell) enter(district, at) else active
    }

    @Synchronized
    fun interrupt() {
        candidate = null
        since = null
        lastFix = null
    }

    private fun enter(district: MarketDistrict, at: Instant): MarketRound {
        interrupt()
        return MarketRound(at.toEpochMilli().toString() + ":" + (++sequence), district.code, district.name)
            .also { current = it }
    }
}
