package com.owenlejeune.whosinspace.api

import com.owenlejeune.whosinspace.api.model.AstronautsResponse
import retrofit2.Response
import retrofit2.http.GET

interface OpenNotifyApi {

    @GET("astros.json")
    suspend fun getAstronautsInSpace(): Response<AstronautsResponse>

}