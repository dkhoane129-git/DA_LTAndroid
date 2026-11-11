package com.spendwise.app.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spendwise.app.R

/**
 * Dialog xác nhận dùng chung (Xóa giao dịch, Xóa danh mục, Đăng xuất).
 * Kết quả trả về qua Fragment Result API nên không bị mất khi xoay màn hình.
 */
class ConfirmDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val requestKey = args.getString(ARG_REQUEST_KEY).orEmpty()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.getString(ARG_TITLE))
            .setMessage(args.getString(ARG_MESSAGE))
            .setPositiveButton(R.string.action_ok) { _, _ ->
                setFragmentResult(requestKey, bundleOf(RESULT_PAYLOAD to args.getLong(ARG_PAYLOAD)))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }

    companion object {
        const val TAG = "ConfirmDialog"
        const val RESULT_PAYLOAD = "payload"
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_PAYLOAD = "payload"

        fun newInstance(
            requestKey: String,
            title: String,
            message: String?,
            payload: Long = 0L
        ) = ConfirmDialogFragment().apply {
            arguments = bundleOf(
                ARG_REQUEST_KEY to requestKey,
                ARG_TITLE to title,
                ARG_MESSAGE to message,
                ARG_PAYLOAD to payload
            )
        }
    }
}
