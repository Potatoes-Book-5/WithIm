package com.potatoes.book5.realestate.vehicle.gleo
import com.potatoes.book5.realestate.domain.error.*
import org.junit.Assert.*
import org.junit.Test
class GleoResponseMapperTest {
 @Test fun providerErrorsAreMappedWithoutDetails() {
  val errors = listOf(DomainError.MissingApiKey, DomainError.InvalidResponse, DomainError.ProviderRejected, DomainError.Http(401))
  val codes = listOf("SERVICE_UNAVAILABLE","INVALID_PROVIDER_RESPONSE","PROVIDER_REJECTED","PROVIDER_ERROR")
  errors.zip(codes).forEach { (error,code) ->
   val callback = GleoResponseMapper().map(GleoAction.GET_AREA_PRICE_SUMMARY, Outcome.Failure(error))
   assertEquals("fail",callback.status)
   assertEquals(code,callback.data.single()["code"])
  }
 }
}
