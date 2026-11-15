package com.potatoes.book5.realestate.vehicle.gleo
import com.potatoes.book5.realestate.domain.error.*
import org.junit.Assert.*
import org.junit.Test
class GleoResponseMapperTest {
 @org.junit.Test fun publicScrapCallbackDoesNotExposeApartmentId() {
  val result=com.potatoes.book5.realestate.domain.error.Outcome.Success(
   com.potatoes.book5.realestate.domain.model.appaction.ActionResult.Completed(
    com.potatoes.book5.realestate.domain.usecase.VoiceScrapResult("private-id",false)))
  val response=GleoResponseMapper().mapScrap(result,includeApartmentId=false)
  org.junit.Assert.assertEquals("success",response.status)
  org.junit.Assert.assertEquals(true,response.data.single()["persisted"])
  org.junit.Assert.assertFalse(response.data.single().containsKey("apartmentId"))
 }
 @Test fun askingMessagesAndTimeoutMetadataMatchAction() {
  assertTrue(GleoCallback.asking(GleoAction.SCRAP_APARTMENT,"apartment").message.contains("apartment"))
  assertTrue(GleoCallback.asking(GleoAction.GET_AREA_PRICE_SUMMARY,"target").message.contains("area"))
  assertEquals("LOCAL",GleoCallback.failure(GleoAction.SCRAP_APARTMENT,"TIMEOUT").data.single()["executionMode"])
  assertEquals(false,GleoCallback.failure(GleoAction.SCRAP_APARTMENT,"TIMEOUT").data.single()["synthetic"])
 }
 @Test fun providerErrorsAreMappedWithoutDetails() {
  val errors = listOf(DomainError.MissingApiKey, DomainError.InvalidResponse, DomainError.ProviderRejected, DomainError.Http(401))
  val codes = listOf("SERVICE_UNAVAILABLE","INVALID_PROVIDER_RESPONSE","PROVIDER_REJECTED","PROVIDER_ERROR")
  errors.zip(codes).forEach { (error,code) ->
   val callback = GleoResponseMapper().map(GleoAction.GET_AREA_PRICE_SUMMARY, Outcome.Failure(error))
   assertEquals("fail",callback.status)
   assertEquals(code,callback.data.single()["code"])
  }
 }
 @Test fun unavailablePreparedDataHasRecoverableCodes() {
  assertEquals("DATA_NOT_READY", GleoResponseMapper().map(GleoAction.SCRAP_APARTMENT,
   Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.NOT_READY))).data.single()["code"])
  assertEquals("STALE_DATA", GleoResponseMapper().map(GleoAction.SCRAP_APARTMENT,
   Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE))).data.single()["code"])
 }
}
