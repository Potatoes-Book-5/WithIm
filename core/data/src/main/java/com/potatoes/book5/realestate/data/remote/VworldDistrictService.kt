package com.potatoes.book5.realestate.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

internal interface VworldDistrictService {
    @GET("req/data")
    suspend fun getDistricts(
        @Query("key") key: String,
        @Query("geomfilter") box: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("service") service: String = "data",
        @Query("request") request: String = "getfeature",
        @Query("version") version: String = "2.0",
        @Query("data") data: String = "LT_C_ADSIGG_INFO",
        @Query("geometry") geometry: Boolean = false,
        @Query("attribute") attribute: Boolean = true,
        @Query("crs") crs: String = "EPSG:4326",
        @Query("format") format: String = "json",
    ): Response<ResponseBody>
}
