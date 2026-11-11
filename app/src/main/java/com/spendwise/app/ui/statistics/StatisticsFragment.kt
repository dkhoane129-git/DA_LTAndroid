package com.spendwise.app.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.spendwise.app.databinding.DialogSetBudgetBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.spendwise.app.R
import com.spendwise.app.adapter.CategoryStatAdapter
import com.spendwise.app.databinding.FragmentStatisticsBinding
import com.spendwise.app.model.TransactionType
import com.spendwise.app.ui.common.DatePickerDialogFragment
import com.spendwise.app.util.Formatters
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.StatisticsViewModel
import java.util.Calendar

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels { StatisticsViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Milestone 5 (LeakCanary): trước đây adapter là field của Fragment nên sống lâu hơn View,
        // giữ tham chiếu tới RecyclerView cũ sau onDestroyView → leak. Nay tạo adapter theo vòng đời View.
        val adapter = CategoryStatAdapter()
        binding.rvStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStats.setHasFixedSize(true)
        binding.rvStats.adapter = adapter

        childFragmentManager.setFragmentResultListener(REQUEST_MONTH, viewLifecycleOwner) { _, bundle ->
            viewModel.selectMonth(
                bundle.getInt(DatePickerDialogFragment.RESULT_YEAR),
                bundle.getInt(DatePickerDialogFragment.RESULT_MONTH)
            )
        }
        binding.btnMonth.setOnClickListener {
            val state = viewModel.uiState.value
            val initial = Calendar.getInstance().apply { set(state.year, state.month, 1) }.timeInMillis
            DatePickerDialogFragment.newInstance(REQUEST_MONTH, initial)
                .show(childFragmentManager, DatePickerDialogFragment.TAG)
        }

        binding.btnSetBudget.setOnClickListener { showBudgetDialog() }

        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.selectType(if (checkedId == R.id.btnIncome) TransactionType.INCOME else TransactionType.EXPENSE)
        }

        viewModel.uiState.collectWithLifecycle(viewLifecycleOwner) { state ->
            binding.btnMonth.text = Formatters.month(state.year, state.month)
            binding.tvIncome.text = Formatters.money(state.summary.income)
            binding.tvExpense.text = Formatters.money(state.summary.expense)
            val toggleId = if (state.type == TransactionType.INCOME) R.id.btnIncome else R.id.btnExpense
            if (binding.toggleType.checkedButtonId != toggleId) binding.toggleType.check(toggleId)
            renderBudget(state)
            adapter.submitList(state.stats)
            binding.tvEmpty.isVisible = state.stats.isEmpty()
        }
    }

    private fun renderBudget(state: StatisticsViewModel.UiState) {
        val budget = state.budget
        if (budget == null) {
            binding.tvBudget.setText(R.string.budget_not_set)
            binding.progressBudget.setProgressCompat(0, false)
            return
        }
        binding.tvBudget.text = getString(
            R.string.budget_progress,
            Formatters.money(state.summary.expense),
            Formatters.money(budget),
            state.budgetPercent
        )
        binding.progressBudget.setProgressCompat(state.budgetPercent.coerceAtMost(100), true)
        val color = if (state.budgetPercent > 100) R.color.expense else R.color.primary
        binding.progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun showBudgetDialog() {
        val dialogBinding = DialogSetBudgetBinding.inflate(layoutInflater)
        viewModel.uiState.value.budget?.let { dialogBinding.etAmount.setText(it.toString()) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.budget_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                if (!viewModel.setBudget(dialogBinding.etAmount.text?.toString().orEmpty())) {
                    Snackbar.make(binding.root, R.string.error_budget, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvStats.adapter = null
        _binding = null
    }

    private companion object {
        const val REQUEST_MONTH = "statistics_month"
    }
}
