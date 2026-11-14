package com.potatoes.book5.realestate.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

internal interface VworldService {
    @GET("req/address")
    suspend fun getCoordinates(
        @Query("key") key: String,
        @Query("address") address: String,
        @Query("type") type: String,
        @Query("service") service: String = "address",
        @Query("request") request: String = "getcoord",
        @Query("version") version: String = "2.0",
        @Query("crs") crs: String = "epsg:4326",
        @Query("format") format: String = "xml",
        @Query("refine") refine: Boolean = true,
        @Query("simple") simple: Boolean = false,
    ): Response<ResponseBody>
}
