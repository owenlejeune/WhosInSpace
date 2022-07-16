package com.owenlejeune.whosinspace.api

import com.owenlejeune.whosinspace.BuildConfig
import com.owenlejeune.whosinspace.api.model.PhotoOfTheDayResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NasaApi {

    @GET("planetary/apod?api_key=${BuildConfig.NasaApiKey}")
    suspend fun getAstronomyPhotoOfTheDay(): Response<PhotoOfTheDayResponse>

}