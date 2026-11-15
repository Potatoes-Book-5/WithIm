package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.usecase.*

/** Trusted in-app normalized call layer. It never parses natural language or opens Android screens. */
class TourIntentAdapter(private val price: GetAreaPriceSummaryUseCase,private val save: SaveTourAreaUseCase,
    private val scrap: ScrapVoiceTargetUseCase,private val check: CheckTourAreaUseCase,
    private val analytics: GetTourAnalyticsUseCase,private val navigation: NavigateTourApartmentUseCase,
    private val camera: ScanTourAreaUseCase,private val briefing: GetDailyBriefingUseCase,
    private val confirm: ConfirmTourBriefingUseCase,private val access: TourAccess) {
    suspend fun handle(intent: TourIntent,context: AppCallContext): Outcome<ActionResult<TourReply>> {
        if(intent!=TourIntent.AutomaticBriefing && !access.valid(context,intent is TourIntent.Analytics || intent is TourIntent.Navigate || intent is TourIntent.CameraScan)) return access.failure()
        return when(intent) {
        is TourIntent.Price -> price(intent.target,context).reply { TourReply.Price(it) }
        is TourIntent.SaveArea -> save(intent.query,context,intent.preferCurrent).reply { TourReply.SavedArea(it) }
        TourIntent.Scrap -> scrap(context).reply { TourReply.Scrapped(it) }
        is TourIntent.CheckArea -> check(intent.query,context).reply { TourReply.Overview(it) }
        is TourIntent.Recommend -> check(intent.query,context,requireSavedTarget=false).reply { TourReply.Overview(it) }
        is TourIntent.Analytics -> analytics(intent.apartmentId,context).reply { TourReply.Analytics(it) }
        is TourIntent.Navigate -> navigation(intent.entrance,intent.replaceConfirmed,context).reply { TourReply.Navigation(it) }
        is TourIntent.CameraScan -> camera(intent.record,context).reply { TourReply.Camera }
        TourIntent.AutomaticBriefing -> Outcome.Failure(DomainError.Unsupported("automatic_startup_unverified"))
        TourIntent.Briefing -> briefing(context).reply { TourReply.Briefing(it) }
        is TourIntent.ConfirmBriefing -> when(val r=confirm(intent.receipts,context)) {
            is Outcome.Failure -> r
            is Outcome.Success -> Outcome.Success(ActionResult.Completed(TourReply.Confirmed(r.value)))
        }
    }
    }
}
private inline fun <T> Outcome<ActionResult<T>>.reply(transform: (T)->TourReply): Outcome<ActionResult<TourReply>> = when(this) {
    is Outcome.Failure -> this
    is Outcome.Success -> when(val r=value) { is ActionResult.NeedsInput -> Outcome.Success(r); is ActionResult.Completed -> Outcome.Success(ActionResult.Completed(transform(r.payload))) }
}
