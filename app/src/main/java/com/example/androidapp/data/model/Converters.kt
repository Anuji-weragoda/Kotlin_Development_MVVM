package com.example.androidapp.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromName(name: Name): String {
        return gson.toJson(name)
    }

    @TypeConverter
    fun toName(value: String): Name {
        val type = object : TypeToken<Name>() {}.type
        return gson.fromJson(value, type)
    }
}
