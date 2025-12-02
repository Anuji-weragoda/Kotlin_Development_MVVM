package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.androidapp.data.model.Country
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryDao {

    @Upsert
    suspend fun upsertCountries(countries: List<Country>)

    @Query("SELECT * FROM countries ORDER BY nameCommon ASC")
    fun getCountries(): Flow<List<Country>>
}
