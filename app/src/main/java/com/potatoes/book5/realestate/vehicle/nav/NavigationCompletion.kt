package com.potatoes.book5.realestate.vehicle.nav

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.tour.*
import java.util.concurrent.atomic.AtomicBoolean

/** One terminal event per SDK request. Submission freshness is checked separately. */
internal class NavigationCompletion(private val requestId:String,private val eligible:()->Boolean) {
    private val closed=AtomicBoolean(false)
    fun cancel() { closed.set(true) }
    fun finish(result:Outcome<NavigationReceipt>): NavigationEvent? {
        if(!closed.compareAndSet(false,true)) return null
        return NavigationEvent(requestId,if(eligible()) result else Outcome.Failure(DomainError.Invalid("context_invalidated")))
    }
}
