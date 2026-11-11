package com.spendwise.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.spendwise.app.adapter.CategoryAdapter
import com.spendwise.app.databinding.BottomSheetCategoryPickerBinding
import com.spendwise.app.model.TransactionType
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.CategoryPickerViewModel

/** BottomSheet chọn danh mục, dùng ở AddEditTransaction và bộ lọc tab Giao dịch. */
class CategoryPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCategoryPickerBinding? = null
    private val binding get() = _binding!!

    // arguments của Fragment tự động được đưa vào SavedStateHandle của ViewModel
    private val viewModel: CategoryPickerViewModel by viewModels { CategoryPickerViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCategoryPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val requestKey = args.getString(ARG_REQUEST_KEY).orEmpty()

        val adapter = CategoryAdapter(onItemClick = { category ->
            setFragmentResult(requestKey, bundleOf(RESULT_CATEGORY_ID to category.id))
            dismiss()
        })
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        binding.btnClear.isVisible = args.getBoolean(ARG_SHOW_CLEAR)
        binding.btnClear.setOnClickListener {
            setFragmentResult(requestKey, bundleOf(RESULT_CATEGORY_ID to NO_CATEGORY))
            dismiss()
        }

        viewModel.categories.collectWithLifecycle(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CategoryPicker"
        const val RESULT_CATEGORY_ID = "category_id"
        const val NO_CATEGORY = -1L
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_SHOW_CLEAR = "show_clear"

        fun newInstance(requestKey: String, type: TransactionType?, showClear: Boolean = false) =
            CategoryPickerBottomSheet().apply {
                arguments = bundleOf(
                    ARG_REQUEST_KEY to requestKey,
                    CategoryPickerViewModel.ARG_TYPE to type?.name,
                    ARG_SHOW_CLEAR to showClear
                )
            }
    }
}
