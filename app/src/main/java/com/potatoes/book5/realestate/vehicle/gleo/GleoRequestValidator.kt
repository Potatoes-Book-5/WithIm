package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.model.appaction.AreaTarget
import com.potatoes.book5.realestate.domain.model.appaction.AreaTargetKind

class GleoRequestValidator {
    fun validate(input: GleoInput): GleoValidation {
        input.decodingError?.let { return GleoValidation.Invalid(it) }
        val parameters = input.parameters
        val allowed = if (input.action == GleoAction.GET_REAL_ESTATE_BRIEFING) emptySet() else setOf("target", "areaQuery")
        if (parameters.keys.any { it !in allowed }) return GleoValidation.Invalid("UNEXPECTED_PARAMETER")
        if (input.action == GleoAction.GET_REAL_ESTATE_BRIEFING) return GleoValidation.Valid(GleoRequest(input.action))
        if (!parameters.containsKey("target")) return GleoValidation.Invalid("MISSING_TARGET", "target")
        val raw = parameters["target"] as? String ?: return GleoValidation.Invalid("INVALID_TARGET_TYPE")
        val kind = AreaTargetKind.entries.firstOrNull { it.name == raw }
            ?: return GleoValidation.Invalid("INVALID_TARGET", "target")
        if (parameters.containsKey("areaQuery") && parameters["areaQuery"] !is String)
            return GleoValidation.Invalid("INVALID_AREA_QUERY_TYPE")
        val query = (parameters["areaQuery"] as? String)?.trim()
        if (query != null && query.length > 256) return GleoValidation.Invalid("AREA_QUERY_TOO_LONG")
        if (kind == AreaTargetKind.NAMED_AREA && query.isNullOrBlank())
            return GleoValidation.Invalid("MISSING_AREA_QUERY", "areaQuery")
        if (kind != AreaTargetKind.NAMED_AREA && query != null)
            return GleoValidation.Invalid("TARGET_QUERY_CONFLICT")
        return GleoValidation.Valid(GleoRequest(input.action, AreaTarget(kind, query)))
    }
}
