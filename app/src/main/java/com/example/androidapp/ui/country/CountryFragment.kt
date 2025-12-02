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
        val retrofit = Retrofit.Builder()
            .baseUrl("https://restcountries.com/")
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

        binding.progressBar.isVisible = true

        viewModel.countries.observe(viewLifecycleOwner) { countries ->
            if (countries.isNotEmpty()) {

                binding.progressBar.isVisible = false
                adapter.setData(countries)
                binding.countryCountText.text = "${countries.size} countries available"
                updateEmptyState()
            } else {

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
