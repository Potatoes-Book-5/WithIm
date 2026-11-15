package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.MarketDistrictResolver
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

private interface PlaceService {
    @GET("req/search")
    suspend fun search(@Query("key") key: String,@Query("query") query: String,
        @Query("request") request: String="search",@Query("service") service: String="search",
        @Query("version") version: String="2.0",@Query("type") type: String="PLACE",
        @Query("format") format: String="json",@Query("crs") crs: String="EPSG:4326",
        @Query("page") page: Int=1,@Query("size") size: Int=10): Response<ResponseBody>
}
object VworldTourPlaceFactory {
    fun create(apiKey: String,resolver: MarketDistrictResolver,clock: Clock=Clock.systemUTC()): suspend (String)->Outcome<TourArea> {
        val client=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS)
            .callTimeout(20,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
        val service=Retrofit.Builder().baseUrl("https://api.vworld.kr/").client(client).build().create(PlaceService::class.java)
        return lookup@ { query ->
            if(apiKey.isBlank()) return@lookup Outcome.Failure(DomainError.MissingApiKey)
            withContext(Dispatchers.IO) {
                try {
                    val response=service.search(apiKey.trim(),query)
                    if(!response.isSuccessful) { response.errorBody()?.close(); return@withContext Outcome.Failure(DomainError.Http(response.code())) }
                    val raw=response.body()?.use { it.string() } ?: return@withContext Outcome.Failure(DomainError.InvalidResponse)
                    if(raw.length>500_000) return@withContext Outcome.Failure(DomainError.InvalidResponse)
                    val root=JSONObject(raw).getJSONObject("response")
                    if(root.getString("status")!="OK") return@withContext Outcome.Failure(DomainError.ProviderRejected)
                    val items=root.getJSONObject("result").getJSONArray("items")
                    val total=root.getJSONObject("record").getString("total").toInt()
                    if(total!=1 || items.length()!=1) return@withContext Outcome.Failure(DomainError.Invalid("ambiguous_place"))
                    val item=items.getJSONObject(0);val id=item.getString("id");val name=item.getString("title")
                    val p=item.getJSONObject("point");val point=GeoPoint(p.getString("y").toDouble(),p.getString("x").toDouble())
                    if(id.isBlank() || name.isBlank() || !point.latitude.isFinite() || !point.longitude.isFinite() ||
                        point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return@withContext Outcome.Failure(DomainError.InvalidResponse)
                    when(val district=resolver.districtAt(point)) {
                        is Outcome.Failure -> district
                        is Outcome.Success -> Outcome.Success(TourArea("vworld:place:$id",name,district.value.code,AreaBoundary.Circle(point),"vworld-place",clock.instant(),district.value.name))
                    }
                } catch(e:CancellationException) { throw e }
                catch(_:java.io.IOException) { Outcome.Failure(DomainError.Offline) }
                catch(_:Exception) { Outcome.Failure(DomainError.InvalidResponse) }
            }
        }
    }
}
