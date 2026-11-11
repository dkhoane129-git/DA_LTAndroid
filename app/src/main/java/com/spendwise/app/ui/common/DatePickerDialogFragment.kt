package com.spendwise.app.ui.common

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import java.util.Calendar

/** Chọn ngày (dùng ở AddEditTransaction) / chọn tháng (dùng ở Statistics). */
class DatePickerDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val cal = Calendar.getInstance().apply { timeInMillis = args.getLong(ARG_INITIAL) }
        val requestKey = args.getString(ARG_REQUEST_KEY).orEmpty()

        return DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val millis = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                }.timeInMillis
                setFragmentResult(
                    requestKey,
                    bundleOf(RESULT_YEAR to year, RESULT_MONTH to month, RESULT_MILLIS to millis)
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        const val TAG = "DatePicker"
        const val RESULT_YEAR = "year"
        const val RESULT_MONTH = "month"
        const val RESULT_MILLIS = "millis"
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_INITIAL = "initial"

        fun newInstance(requestKey: String, initialMillis: Long) = DatePickerDialogFragment().apply {
            arguments = bundleOf(ARG_REQUEST_KEY to requestKey, ARG_INITIAL to initialMillis)
        }
    }
}
