package com.example.androidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName

@Entity(tableName = "countries")
@TypeConverters(Converters::class)
data class Country(
    @PrimaryKey
    val nameCommon: String,
    @SerializedName("name")
    val name: Name,
    @SerializedName("region")
    val region: String? = null,
    @SerializedName("subregion")
    val subregion: String? = null
)

data class Name(
    @SerializedName("common")
    val common: String,
    @SerializedName("official")
    val official: String? = null
)
