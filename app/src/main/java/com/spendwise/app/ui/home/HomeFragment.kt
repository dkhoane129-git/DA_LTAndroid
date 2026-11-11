package com.spendwise.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.spendwise.app.R
import com.spendwise.app.databinding.FragmentHomeBinding
import com.spendwise.app.repository.SyncManager
import com.spendwise.app.ui.common.TransactionListFragment
import com.spendwise.app.ui.main.MainActivity
import com.spendwise.app.ui.transactions.AddEditTransactionActivity
import com.spendwise.app.util.Formatters
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.HomeViewModel
import com.spendwise.app.viewmodel.TransactionListSource

class HomeFragment : Fragment(), TransactionListFragment.Host {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    override val transactionListSource: TransactionListSource get() = viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(R.id.recentListContainer, TransactionListFragment())
            }
        }

        binding.fabAdd.setOnClickListener {
            startActivity(AddEditTransactionActivity.newIntent(requireContext()))
        }
        binding.btnSeeAll.setOnClickListener {
            (requireActivity() as MainActivity).selectTab(R.id.transactionsFragment)
        }

        viewModel.userName.collectWithLifecycle(viewLifecycleOwner) { name ->
            binding.tvGreeting.text = getString(R.string.home_greeting, name)
        }
        binding.tvSyncStatus.setOnClickListener { viewModel.syncNow() }
        viewModel.syncState.collectWithLifecycle(viewLifecycleOwner) { state ->
            binding.tvSyncStatus.text = when (state) {
                SyncManager.State.Idle -> ""
                SyncManager.State.Syncing -> getString(R.string.sync_syncing)
                is SyncManager.State.Done -> getString(R.string.sync_done, Formatters.dateTime(state.at))
                is SyncManager.State.Failed -> getString(R.string.sync_failed, state.message)
            }
        }
        viewModel.summary.collectWithLifecycle(viewLifecycleOwner) { summary ->
            binding.tvBalance.text = Formatters.money(summary.balance)
            binding.tvIncome.text = Formatters.money(summary.income)
            binding.tvExpense.text = Formatters.money(summary.expense)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
