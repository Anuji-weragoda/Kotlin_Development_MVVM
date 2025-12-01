package com.example.androidapp.ui.country

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.androidapp.data.model.Country
import com.example.androidapp.databinding.ItemCountryBinding

class CountryAdapter : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    private var countryList = listOf<Country>()
    private var filteredList = listOf<Country>()

    fun setData(list: List<Country>) {
        countryList = list
        filteredList = list
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) {
            countryList
        } else {
            countryList.filter { country ->
                country.name.common.contains(query, ignoreCase = true) ||
                        country.region?.contains(query, ignoreCase = true) == true ||
                        country.subregion?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val binding = ItemCountryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount() = filteredList.size

    inner class CountryViewHolder(private val binding: ItemCountryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(country: Country) {
            // Set country name
            binding.countryNameText.text = country.name.common

            // Set region and subregion
            binding.regionText.text = when {
                !country.region.isNullOrEmpty() && !country.subregion.isNullOrEmpty() ->
                    "${country.region} • ${country.subregion}"
                !country.region.isNullOrEmpty() ->
                    country.region
                !country.subregion.isNullOrEmpty() ->
                    country.subregion
                else ->
                    "Unknown Region"
            }
        }
    }
}