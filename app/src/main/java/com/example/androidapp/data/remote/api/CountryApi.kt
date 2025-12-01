package com.example.androidapp.data.remote.api

import com.example.androidapp.data.model.Country
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CountryApi {

    @GET("v3.1/all")
    suspend fun getAllCountries(
        @Query("fields") fields: String = "name,region,subregion"
    ): Response<List<Country>>

    @GET("v3.1/independent")
    suspend fun getIndependentCountries(
        @Query("status") status: Boolean = true,
        @Query("fields") fields: String = "name,region,subregion"
    ): Response<List<Country>>

    @GET("v3.1/name/{name}")
    suspend fun getCountryByName(
        @Path("name") name: String,
        @Query("fields") fields: String = "name,capital,region,subregion"
    ): Response<List<Country>>
}