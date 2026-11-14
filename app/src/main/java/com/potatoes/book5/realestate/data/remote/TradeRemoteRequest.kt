package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import org.xmlpull.v1.XmlPullParserException
import retrofit2.HttpException

internal suspend fun <T> tradeRemoteRequest(block: suspend () -> T): Outcome<T> {
    repeat(2) { attempt ->
        currentCoroutineContext().ensureActive()
        try {
            return Outcome.Success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: HttpException) {
            error.response()?.errorBody()?.close()
            return Outcome.Failure(DomainError.Http(error.code()))
        } catch (_: TradeProviderRejected) {
            return Outcome.Failure(DomainError.ProviderRejected)
        } catch (_: InvalidTradeResponse) {
            return Outcome.Failure(DomainError.InvalidResponse)
        } catch (_: XmlPullParserException) {
            return Outcome.Failure(DomainError.InvalidResponse)
        } catch (_: InterruptedIOException) {
            currentCoroutineContext().ensureActive()
            if (attempt == 1) return Outcome.Failure(DomainError.Timeout)
        } catch (_: IOException) {
            currentCoroutineContext().ensureActive()
            if (attempt == 1) return Outcome.Failure(DomainError.Offline)
        }
        delay(300)
    }
    error("Unreachable")
}
