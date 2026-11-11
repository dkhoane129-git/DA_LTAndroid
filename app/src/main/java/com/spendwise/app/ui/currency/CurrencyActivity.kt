package com.spendwise.app.ui.currency

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.spendwise.app.R
import com.spendwise.app.adapter.CurrencyAdapter
import com.spendwise.app.databinding.ActivityCurrencyBinding
import com.spendwise.app.ui.common.render
import com.spendwise.app.util.Formatters
import com.spendwise.app.util.Resource
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.CurrencyViewModel

/** Phân hệ Tỷ giá: GET JSON online → Loading / Success / Error. */
class CurrencyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrencyBinding
    private val viewModel: CurrencyViewModel by viewModels { CurrencyViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = CurrencyAdapter()
        binding.rvRates.layoutManager = LinearLayoutManager(this)
        binding.rvRates.setHasFixedSize(true)
        binding.rvRates.adapter = adapter

        if (savedInstanceState == null) {
            binding.etAmount.setText(CurrencyViewModel.DEFAULT_AMOUNT.toString())
        }
        binding.etAmount.doAfterTextChanged { viewModel.setAmount(it?.toString().orEmpty()) }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.state.collectWithLifecycle(this) { state ->
            val cached = (state as? Resource.Error)?.data
            binding.stateView.render(state, hasData = !cached.isNullOrEmpty()) { viewModel.refresh() }
            if (state !is Resource.Loading) binding.swipeRefresh.isRefreshing = false
            val rates = (state as? Resource.Success)?.data ?: cached
            binding.tvUpdated.text = rates?.firstOrNull()?.let {
                getString(R.string.rates_updated, Formatters.dateTime(it.updatedAt))
            }.orEmpty()
        }
        viewModel.rows.collectWithLifecycle(this) { adapter.submitList(it) }
    }
}
