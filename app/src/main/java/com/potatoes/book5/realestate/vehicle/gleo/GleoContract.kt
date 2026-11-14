package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.model.appaction.AreaTarget

enum class GleoAction {
    GET_AREA_PRICE_SUMMARY, SAVE_INTEREST_AREA, GET_REAL_ESTATE_BRIEFING;
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
            action, "fail", "The test request could not be completed. No user data was changed.",
            listOf(mapOf("code" to code, "executionMode" to "DUMMY", "synthetic" to true)),
        )
        fun asking(action: GleoAction, field: String) = GleoCallback(
            action, "asking", "Additional target information is required for this test request.",
            listOf(mapOf("requiredField" to field, "executionMode" to "DUMMY", "synthetic" to true)),
        )
    }
}
fun interface GleoCallbackSender { suspend fun send(callback: GleoCallback) }
