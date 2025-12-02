package com.example.androidapp.ui.country

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.repository.CountryRepository
import kotlinx.coroutines.launch

class CountryViewModel(private val repository: CountryRepository) : ViewModel() {


    val countries = repository.countries.asLiveData()

    fun fetchCountries() {
        viewModelScope.launch {
            try {
                repository.fetchAndSaveCountries()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
