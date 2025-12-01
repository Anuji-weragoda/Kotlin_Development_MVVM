package com.example.androidapp.data.remote.api

import com.example.androidapp.data.model.Country
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CountryApi {

    // Fetch ALL countries (requires fields)
    @GET("v3.1/all")
    suspend fun getAllCountries(
        @Query("fields") fields: String = "name"
    ): Response<List<Country>>

    // Fetch ONLY independent countries
    @GET("v3.1/independent")
    suspend fun getIndependentCountries(
        @Query("status") status: Boolean = true,
        @Query("fields") fields: String = "name"
    ): Response<List<Country>>

    // Search by name
    @GET("v3.1/name/{name}")
    suspend fun getCountryByName(
        @Query("fields") fields: String = "name,capital"
    ): Response<List<Country>>
}
