package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.data.remote.GeocoderProviderRejected
import com.potatoes.book5.realestate.data.remote.InvalidGeocoderResponse
import com.potatoes.book5.realestate.data.remote.VworldService
import com.potatoes.book5.realestate.data.remote.VworldXmlParser
import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.AddressKind
import com.potatoes.book5.realestate.domain.model.GeoCoordinates
import com.potatoes.book5.realestate.domain.port.AddressGeocoder
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import retrofit2.HttpException

internal class VworldAddressGeocoder(
    private val service: VworldService,
    private val apiKey: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : AddressGeocoder {
    override suspend fun geocode(address: String, kind: AddressKind): Outcome<GeoCoordinates> {
        val trimmed = address.trim()
        if (trimmed.isEmpty() || trimmed.length > 256) {
            return Outcome.Failure(DomainError.Invalid("invalid-address"))
        }
        if (apiKey.isBlank()) return Outcome.Failure(DomainError.MissingApiKey)
        return withContext(io) {
            repeat(2) { attempt ->
                currentCoroutineContext().ensureActive()
                try {
                    val response = service.getCoordinates(apiKey, trimmed, kind.name)
                    if (!response.isSuccessful) throw HttpException(response)
                    val body = response.body() ?: throw InvalidGeocoderResponse()
                    return@withContext Outcome.Success(body.use { VworldXmlParser.parse(it.byteStream()) })
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    error.response()?.errorBody()?.close()
                    return@withContext Outcome.Failure(DomainError.Http(error.code()))
                } catch (_: GeocoderProviderRejected) {
                    return@withContext Outcome.Failure(DomainError.ProviderRejected)
                } catch (_: InvalidGeocoderResponse) {
                    return@withContext Outcome.Failure(DomainError.InvalidResponse)
                } catch (_: XmlPullParserException) {
                    return@withContext Outcome.Failure(DomainError.InvalidResponse)
                } catch (_: InterruptedIOException) {
                    currentCoroutineContext().ensureActive()
                    if (attempt == 1) return@withContext Outcome.Failure(DomainError.Timeout)
                } catch (_: IOException) {
                    currentCoroutineContext().ensureActive()
                    if (attempt == 1) return@withContext Outcome.Failure(DomainError.Offline)
                }
                delay(300)
            }
            error("Unreachable")
        }
    }
}
