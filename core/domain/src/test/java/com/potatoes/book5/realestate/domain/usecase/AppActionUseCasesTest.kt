package com.potatoes.book5.realestate.domain.usecase
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class AppActionUseCasesTest {
 private val context = AppCallContext("r", "s", "p")
 @Test fun invalidTargetsNeverReachPort() = runTest {
  var calls = 0
  val port = object : AreaPriceSummaryRepository {
   override suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> { calls++; return Outcome.Failure(DomainError.Offline) }
  }
  val api = GetAreaPriceSummaryUseCase(port)
  assertEquals(Outcome.Success(ActionResult.NeedsInput("areaQuery")), api(AreaTarget(AreaTargetKind.NAMED_AREA),context))
  assertTrue(api(AreaTarget(AreaTargetKind.CURRENT_LOCATION,"unexpected"),context) is Outcome.Failure)
  assertTrue(api(AreaTarget(AreaTargetKind.CURRENT_LOCATION),context.copy(profileScope="")) is Outcome.Failure)
  assertEquals(0,calls)
 }
 @Test fun providerFailureIsPropagated() = runTest {
  val port = object : AreaPriceSummaryRepository {
   override suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> = Outcome.Failure(DomainError.MissingApiKey)
  }
  assertEquals(Outcome.Failure(DomainError.MissingApiKey),GetAreaPriceSummaryUseCase(port)(AreaTarget(AreaTargetKind.CURRENT_LOCATION),context))
 }
}
