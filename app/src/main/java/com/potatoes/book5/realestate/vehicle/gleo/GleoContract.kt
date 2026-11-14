package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.model.appaction.AreaTarget

enum class GleoAction {
    GET_AREA_PRICE_SUMMARY, SAVE_INTEREST_AREA, GET_REAL_ESTATE_BRIEFING, SCRAP_APARTMENT;
    companion object { fun fromWire(value: String?): GleoAction? = entries.firstOrNull { it.name == value } }
}
data class GleoInput(
    val action: GleoAction,
    val parameters: Map<String, Any?> = emptyMap(),
    val decodingError: String? = null,
    val receivedAtMillis: Long? = null,
)
data class GleoRequest(val action: GleoAction, val target: AreaTarget? = null)
sealed interface GleoValidation {
    data class Valid(val request: GleoRequest) : GleoValidation
    data class Invalid(val code: String, val requiredField: String? = null) : GleoValidation
}
data class GleoCallback(
    val action: GleoAction,
    val status: String,
    val message: String,
    val data: List<Map<String, Any?>>,
) {
    companion object {
        fun failure(action: GleoAction, code: String) = GleoCallback(
            action, "fail", "The request could not be completed. Please check the current state before retrying.",
            listOf(mapOf("code" to code, "executionMode" to if(action == GleoAction.SCRAP_APARTMENT) "LOCAL" else "DUMMY", "synthetic" to (action != GleoAction.SCRAP_APARTMENT))),
        )
        fun asking(action: GleoAction, field: String) = GleoCallback(
            action, "asking", if(action == GleoAction.SCRAP_APARTMENT) "Select an apartment or use an active scrap suggestion before trying again." else "Additional target area information is required.",
            listOf(mapOf("requiredField" to field, "executionMode" to if(action == GleoAction.SCRAP_APARTMENT) "LOCAL" else "DUMMY", "synthetic" to (action != GleoAction.SCRAP_APARTMENT))),
        )
    }
}
fun interface GleoCallbackSender { suspend fun send(callback: GleoCallback) }
