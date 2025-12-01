package com.example.androidapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.example.androidapp.data.model.Converters

@Entity(
    tableName = "countries"
)
@TypeConverters(Converters::class)
data class Country(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @SerializedName("name")
    val name: Name
)

data class Name(
    @SerializedName("common")
    val common: String
)
