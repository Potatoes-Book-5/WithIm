package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.PreparedScrapCandidate

/** Synchronous memory-only read. Never initiate IO or wait on a collector. */
fun interface PreparedScrapReader {
    fun readScrapCandidate(apartmentId: String): Outcome<PreparedScrapCandidate>
}
