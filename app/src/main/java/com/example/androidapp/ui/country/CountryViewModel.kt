package com.example.androidapp.ui.country

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.repository.CountryRepository
import kotlinx.coroutines.launch
import javax.net.ssl.SSLPeerUnverifiedException

class CountryViewModel(private val repository: CountryRepository) : ViewModel() {

    private val TAG = "CountryViewModel"

    val countries = repository.countries.asLiveData()

    fun fetchCountries() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching countries from API...")
                repository.fetchAndSaveCountries()
                Log.d(TAG, "Countries fetched successfully")
            } catch (e: SSLPeerUnverifiedException) {
                Log.e(TAG, " SSL CERTIFICATE PINNING FAILED! ")
                Log.e(TAG, "This means the server certificate does not match the pinned certificates")
                Log.e(TAG, "Error: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching countries: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
