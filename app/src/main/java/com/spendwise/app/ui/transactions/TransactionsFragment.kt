package com.spendwise.app.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.spendwise.app.R
import com.spendwise.app.databinding.FragmentTransactionsBinding
import com.spendwise.app.ui.common.CategoryPickerBottomSheet
import com.spendwise.app.ui.common.TransactionListFragment
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.TransactionListSource
import com.spendwise.app.viewmodel.TransactionsViewModel
import com.spendwise.app.viewmodel.TransactionsViewModel.TypeFilter

class TransactionsFragment : Fragment(), TransactionListFragment.Host {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionsViewModel by viewModels { TransactionsViewModel.Factory }

    override val transactionListSource: TransactionListSource get() = viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.commit { replace(R.id.listContainer, TransactionListFragment()) }
        }

        setupTypeChips()
        setupCategoryFilter()
        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
    }

    private fun setupTypeChips() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipIncome -> TypeFilter.INCOME
                R.id.chipExpense -> TypeFilter.EXPENSE
                else -> TypeFilter.ALL
            }
            viewModel.setTypeFilter(filter)
        }
        viewModel.typeFilter.collectWithLifecycle(viewLifecycleOwner) { filter ->
            val chipId = when (filter) {
                TypeFilter.ALL -> R.id.chipAll
                TypeFilter.INCOME -> R.id.chipIncome
                TypeFilter.EXPENSE -> R.id.chipExpense
            }
            if (binding.chipGroupType.checkedChipId != chipId) binding.chipGroupType.check(chipId)
        }
    }

    private fun setupCategoryFilter() {
        childFragmentManager.setFragmentResultListener(REQUEST_CATEGORY, viewLifecycleOwner) { _, bundle ->
            val id = bundle.getLong(CategoryPickerBottomSheet.RESULT_CATEGORY_ID)
            if (id == CategoryPickerBottomSheet.NO_CATEGORY) viewModel.clearCategoryFilter()
            else viewModel.setCategoryFilter(id)
        }
        binding.chipCategory.setOnClickListener {
            CategoryPickerBottomSheet.newInstance(REQUEST_CATEGORY, type = null, showClear = true)
                .show(childFragmentManager, CategoryPickerBottomSheet.TAG)
        }
        binding.chipCategory.setOnCloseIconClickListener { viewModel.clearCategoryFilter() }

        viewModel.selectedCategory.collectWithLifecycle(viewLifecycleOwner) { category ->
            binding.chipCategory.text =
                category?.let { "${it.icon} ${it.name}" } ?: getString(R.string.filter_category)
            binding.chipCategory.isCloseIconVisible = category != null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val REQUEST_CATEGORY = "transactions_category_filter"
    }
}
