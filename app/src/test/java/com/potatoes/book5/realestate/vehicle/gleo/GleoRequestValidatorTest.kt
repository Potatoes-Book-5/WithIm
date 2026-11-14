package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.model.appaction.AreaTargetKind
import org.junit.Assert.*
import org.junit.Test

class GleoRequestValidatorTest {
    private val validator = GleoRequestValidator()
    private val price = GleoAction.GET_AREA_PRICE_SUMMARY
    @Test fun missingTargetDoesNotSilentlySelectCurrentLocation() {
        assertEquals(GleoValidation.Invalid("MISSING_TARGET", "target"), validator.validate(GleoInput(price)))
    }
    @Test fun rejectsMistypedAndNullTarget() {
        listOf(42, null, true, emptyList<String>()).forEach {
            assertEquals(GleoValidation.Invalid("INVALID_TARGET_TYPE"), validator.validate(GleoInput(price, mapOf("target" to it))))
        }
    }
    @Test fun invalidEnumAsksForTarget() {
        assertEquals(GleoValidation.Invalid("INVALID_TARGET", "target"), validator.validate(GleoInput(price, mapOf("target" to "here"))))
    }
    @Test fun namedAreaRequiresName() {
        assertEquals(GleoValidation.Invalid("MISSING_AREA_QUERY", "areaQuery"), validator.validate(GleoInput(price, mapOf("target" to "NAMED_AREA"))))
    }
    @Test fun currentAndPreviousAreDifferentRequests() {
        val current = validator.validate(GleoInput(price, mapOf("target" to "CURRENT_LOCATION"))) as GleoValidation.Valid
        val previous = validator.validate(GleoInput(price, mapOf("target" to "LAST_REFERENCED_AREA"))) as GleoValidation.Valid
        assertEquals(AreaTargetKind.CURRENT_LOCATION, current.request.target?.kind)
        assertEquals(AreaTargetKind.LAST_REFERENCED_AREA, previous.request.target?.kind)
    }
    @Test fun rejectsTargetQueryConflicts() {
        assertEquals(GleoValidation.Invalid("TARGET_QUERY_CONFLICT"), validator.validate(GleoInput(price,
            mapOf("target" to "CURRENT_LOCATION", "areaQuery" to "가상마을 A"))))
    }
    @Test fun rejectsProfileDestinationAndOtherInjectedParameters() {
        listOf("profileId", "callbackPackage", "requestId", "allowDriving").forEach {
            assertEquals(GleoValidation.Invalid("UNEXPECTED_PARAMETER"), validator.validate(GleoInput(price,
                mapOf("target" to "CURRENT_LOCATION", it to "injected"))))
        }
    }
    @Test fun briefingIsStrictlyArgumentFree() {
        assertTrue(validator.validate(GleoInput(GleoAction.GET_REAL_ESTATE_BRIEFING)) is GleoValidation.Valid)
        assertTrue(validator.validate(GleoInput(GleoAction.GET_REAL_ESTATE_BRIEFING, mapOf("target" to "CURRENT_LOCATION"))) is GleoValidation.Invalid)
    }
    @Test fun namedQueryTypeAndLengthAreBounded() {
        assertEquals(GleoValidation.Invalid("INVALID_AREA_QUERY_TYPE"), validator.validate(GleoInput(price,
            mapOf("target" to "NAMED_AREA", "areaQuery" to 84))))
        assertEquals(GleoValidation.Invalid("AREA_QUERY_TOO_LONG"), validator.validate(GleoInput(price,
            mapOf("target" to "NAMED_AREA", "areaQuery" to "a".repeat(257)))))
    }
}
