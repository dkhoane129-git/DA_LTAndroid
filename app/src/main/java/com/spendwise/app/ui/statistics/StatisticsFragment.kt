package com.spendwise.app.ui.statistics

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spendwise.app.R
import com.spendwise.app.adapter.CategoryStatAdapter
import com.spendwise.app.databinding.DialogSetBudgetBinding
import com.spendwise.app.databinding.FragmentStatisticsBinding
import com.spendwise.app.model.TransactionType
import com.spendwise.app.util.Formatters
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.StatisticsViewModel

/** Màn hình thống kê và hạn mức chi tiêu theo tháng. */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: StatisticsViewModel by viewModels { StatisticsViewModel.Factory }
    private var budgetDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Adapter chỉ tồn tại theo vòng đời View, không lưu trong Fragment.
        val adapter = CategoryStatAdapter()
        binding.rvStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStats.setHasFixedSize(true)
        binding.rvStats.adapter = adapter

        childFragmentManager.setFragmentResultListener(REQUEST_MONTH, viewLifecycleOwner) { _, result ->
            viewModel.selectMonth(
                result.getInt(MonthPickerDialogFragment.RESULT_YEAR),
                result.getInt(MonthPickerDialogFragment.RESULT_MONTH)
            )
        }
        binding.btnMonth.setOnClickListener {
            val state = viewModel.uiState.value
            MonthPickerDialogFragment.newInstance(REQUEST_MONTH, state.year, state.month)
                .show(childFragmentManager, MonthPickerDialogFragment.TAG)
        }
        binding.btnSetBudget.setOnClickListener { showBudgetDialog() }

        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.selectType(
                    if (checkedId == R.id.btnIncome) TransactionType.INCOME else TransactionType.EXPENSE
                )
            }
        }

        viewModel.uiState.collectWithLifecycle(viewLifecycleOwner) { state ->
            binding.btnMonth.text = Formatters.month(state.year, state.month)
            binding.tvIncome.text = Formatters.money(state.summary.income)
            binding.tvExpense.text = Formatters.money(state.summary.expense)
            val checkedId = if (state.type == TransactionType.INCOME) R.id.btnIncome else R.id.btnExpense
            if (binding.toggleType.checkedButtonId != checkedId) binding.toggleType.check(checkedId)
            renderBudget(state)
            adapter.submitList(state.stats)
            binding.tvEmpty.isVisible = state.stats.isEmpty()
        }
    }

    private fun renderBudget(state: StatisticsViewModel.UiState) {
        val budget = state.budget
        val indicatorColor = if (budget != null && state.budgetPercent > 100) {
            R.color.expense
        } else {
            R.color.primary
        }
        binding.progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), indicatorColor))

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
        binding.progressBudget.setProgressCompat(state.budgetPercent.coerceIn(0, 100), true)
    }

    private fun showBudgetDialog() {
        val dialogBinding = DialogSetBudgetBinding.inflate(layoutInflater)
        viewModel.uiState.value.budget?.let { dialogBinding.etAmount.setText(it.toString()) }

        // Không gắn callback trực tiếp vào setPositiveButton: Android sẽ tự đóng dialog
        // dù nhập sai. Bắt sự kiện sau show() để người dùng có thể sửa tại chỗ.
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.budget_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        budgetDialog = dialog
        dialog.setOnDismissListener {
            if (budgetDialog === dialog) budgetDialog = null
        }
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val amountText = dialogBinding.etAmount.text?.toString().orEmpty().trim()
                if (viewModel.setBudget(amountText)) {
                    dialog.dismiss()
                } else {
                    dialogBinding.tilAmount.error = getString(R.string.error_budget)
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        // Tránh giữ tham chiếu tới View/Context sau khi rời tab hoặc xoay màn hình.
        budgetDialog?.dismiss()
        budgetDialog = null
        binding.rvStats.adapter = null
        binding.rvStats.layoutManager = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val REQUEST_MONTH = "statistics_month"
    }
}
