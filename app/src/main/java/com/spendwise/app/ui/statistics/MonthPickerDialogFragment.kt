package com.spendwise.app.ui.statistics

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spendwise.app.R

/** Chọn riêng tháng/năm; không làm thay đổi DatePicker dùng cho giao dịch. */
class MonthPickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year = requireArguments().getInt(ARG_YEAR)
        val month = requireArguments().getInt(ARG_MONTH)
        val pickerMonth = NumberPicker(requireContext()).apply {
            minValue = 1
            maxValue = 12
            displayedValues = (1..12).map { "Tháng $it" }.toTypedArray()
            value = month + 1
            wrapSelectorWheel = true
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            contentDescription = getString(R.string.choose_month)
        }
        val pickerYear = NumberPicker(requireContext()).apply {
            minValue = MIN_YEAR
            maxValue = MAX_YEAR
            value = year.coerceIn(MIN_YEAR, MAX_YEAR)
            wrapSelectorWheel = false
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(padding, padding, padding, padding)
            addView(pickerMonth, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(pickerYear, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_month)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    requireArguments().getString(ARG_REQUEST_KEY).orEmpty(),
                    bundleOf(RESULT_YEAR to pickerYear.value, RESULT_MONTH to pickerMonth.value - 1)
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    companion object {
        const val TAG = "MonthPicker"
        const val RESULT_YEAR = "year"
        const val RESULT_MONTH = "month"  // 0-11, đồng bộ với java.util.Calendar
        private const val ARG_YEAR = "initial_year"
        private const val ARG_MONTH = "initial_month"
        private const val ARG_REQUEST_KEY = "request_key"
        private const val MIN_YEAR = 1970
        private const val MAX_YEAR = 2100

        fun newInstance(requestKey: String, year: Int, month: Int) = MonthPickerDialogFragment().apply {
            arguments = bundleOf(ARG_REQUEST_KEY to requestKey, ARG_YEAR to year, ARG_MONTH to month)
        }
    }
}
