package com.example.androidapp.ui.country

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.remote.api.CountryApi
import com.example.androidapp.data.repository.CountryRepository
import com.example.androidapp.databinding.FragmentCountryBinding
import com.example.androidapp.ui.factory.CountryViewModelFactory
import kotlinx.coroutines.launch
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

        // TESTING SSL PINNING: Clear database to force API call
        // TODO: Remove this after testing SSL pinning
        lifecycleScope.launch {
            AppDatabase.getDatabase(requireContext()).countryDao().deleteAll()
            android.util.Log.d("CountryFragment", "🗑 Database cleared - will fetch from API to test SSL pinning")
        }

        setupRecyclerView()
        setupViewModel()
        setupSearch()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.countryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CountryFragment.adapter
        }
    }

    private fun setupViewModel() {
        // Use secure OkHttpClient with SSL certificate pinning
        val secureClient = com.example.androidapp.data.remote.ssl.SslConfig.createSecureOkHttpClient()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://restcountries.com/")
            .client(secureClient) // Add secure client with certificate pinning
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CountryApi::class.java)
        val dao = AppDatabase.getDatabase(requireContext()).countryDao()
        val repository = CountryRepository(api, dao)
        val factory = CountryViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CountryViewModel::class.java]
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                adapter.filter(query)
                binding.clearSearchButton.isVisible = query.isNotEmpty()
                updateEmptyState()
            }
        })

        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }
    }

    private fun observeData() {
        android.util.Log.d("CountryFragment", "Setting up country observer...")
        binding.progressBar.isVisible = true

        viewModel.countries.observe(viewLifecycleOwner) { countries ->
            android.util.Log.d("CountryFragment", "Countries updated: ${countries.size} items")
            if (countries.isNotEmpty()) {
                android.util.Log.d("CountryFragment", " Countries loaded from database (cached)")
                binding.progressBar.isVisible = false
                adapter.setData(countries)
                binding.countryCountText.text = "${countries.size} countries available"
                updateEmptyState()
            } else {
                android.util.Log.d("CountryFragment", "⚠ No cached countries, fetching from API...")
                viewModel.fetchCountries()
            }
        }
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.emptyStateLayout.isVisible = isEmpty
        binding.countryRecyclerView.isVisible = !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
