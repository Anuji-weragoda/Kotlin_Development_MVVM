package com.example.androidapp.data.repository

import com.example.androidapp.data.local.CountryDao
import com.example.androidapp.data.model.Country
import com.example.androidapp.data.remote.api.CountryApi
import kotlinx.coroutines.flow.Flow

class CountryRepository(
    private val api: CountryApi,
    private val dao: CountryDao
) {
    val countries: Flow<List<Country>> = dao.getCountries()

    suspend fun fetchAndSaveCountries() {
        val response = api.getAllCountries()
        if (response.isSuccessful) {
            response.body()?.let { list ->

                val mappedList = list.map { country ->
                    country.copy(nameCommon = country.name.common)
                }
                dao.upsertCountries(mappedList)
            }
        } else {
            throw Exception("API error: ${response.code()} - ${response.message()}")
        }
    }

}
