package com.example.androidapp.ui.country

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.remote.api.CountryApi
import com.example.androidapp.data.repository.CountryRepository
import com.example.androidapp.databinding.FragmentCountryBinding
import com.example.androidapp.ui.factory.CountryViewModelFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CountryFragment : Fragment() {

    private var _binding: FragmentCountryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CountryViewModel
    private val adapter = CountryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCountryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.countryRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        binding.countryRecyclerView.adapter = adapter

        // Setup Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://restcountries.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CountryApi::class.java)

        // Setup Room + Repository
        val dao = AppDatabase.getDatabase(requireContext()).countryDao()
        val repository = CountryRepository(api, dao)

        // Setup ViewModel
        val factory = CountryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CountryViewModel::class.java]

        // Observe LiveData from Room
        viewModel.countries.observe(viewLifecycleOwner) { countries ->
            adapter.setData(countries)
        }

        // Fetch fresh data from API → save to Room
        viewModel.fetchCountries()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
