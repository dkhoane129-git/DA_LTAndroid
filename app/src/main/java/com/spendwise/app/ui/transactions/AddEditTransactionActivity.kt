package com.spendwise.app.ui.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.spendwise.app.R
import com.spendwise.app.databinding.ActivityAddEditTransactionBinding
import com.spendwise.app.model.TransactionType
import com.spendwise.app.ui.common.CategoryPickerBottomSheet
import com.spendwise.app.ui.common.DatePickerDialogFragment
import com.spendwise.app.util.Formatters
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.AddEditTransactionViewModel

class AddEditTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTransactionBinding
    private val viewModel: AddEditTransactionViewModel by viewModels { AddEditTransactionViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.setTitle(
            if (viewModel.isEditMode) R.string.title_edit_transaction else R.string.title_add_transaction
        )

        binding.etTitle.doAfterTextChanged { viewModel.onTitleChanged(it?.toString().orEmpty()) }
        binding.etAmount.doAfterTextChanged { viewModel.onAmountChanged(it?.toString().orEmpty()) }
        binding.etNote.doAfterTextChanged { viewModel.onNoteChanged(it?.toString().orEmpty()) }

        checkType(viewModel.form.value.type)
        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.onTypeChanged(
                if (checkedId == R.id.btnIncome) TransactionType.INCOME else TransactionType.EXPENSE
            )
        }

        setupPickers()
        binding.btnSave.setOnClickListener { viewModel.save() }
        observeViewModel()
    }

    private fun setupPickers() {
        supportFragmentManager.setFragmentResultListener(REQUEST_CATEGORY, this) { _, bundle ->
            viewModel.onCategorySelected(bundle.getLong(CategoryPickerBottomSheet.RESULT_CATEGORY_ID))
        }
        supportFragmentManager.setFragmentResultListener(REQUEST_DATE, this) { _, bundle ->
            viewModel.onDateSelected(bundle.getLong(DatePickerDialogFragment.RESULT_MILLIS))
        }

        val openCategoryPicker = {
            CategoryPickerBottomSheet.newInstance(REQUEST_CATEGORY, viewModel.form.value.type)
                .show(supportFragmentManager, CategoryPickerBottomSheet.TAG)
        }
        binding.etCategory.setOnClickListener { openCategoryPicker() }
        binding.tilCategory.setEndIconOnClickListener { openCategoryPicker() }

        binding.etDate.setOnClickListener {
            DatePickerDialogFragment.newInstance(REQUEST_DATE, viewModel.form.value.date)
                .show(supportFragmentManager, DatePickerDialogFragment.TAG)
        }
    }

    private fun observeViewModel() {
        viewModel.form.collectWithLifecycle(this) { f ->
            binding.tilTitle.error = f.titleError?.let(::getString)
            binding.tilAmount.error = f.amountError?.let(::getString)
            binding.tilCategory.error = f.categoryError?.let(::getString)
            binding.etDate.setText(Formatters.date(f.date))
            binding.btnSave.isEnabled = !f.isSaving
        }
        viewModel.selectedCategory.collectWithLifecycle(this) { category ->
            binding.etCategory.setText(category?.let { "${it.icon}  ${it.name}" }.orEmpty())
        }
        viewModel.events.collectWithLifecycle(this) { event ->
            when (event) {
                // Chế độ Sửa: dữ liệu cũ nạp xong từ Room → đổ vào form (chỉ 1 lần / ViewModel,
                // xoay màn hình thì EditText tự khôi phục nên không bị ghi đè)
                is AddEditTransactionViewModel.Event.Loaded -> {
                    binding.etTitle.setText(event.form.title)
                    binding.etAmount.setText(event.form.amountText)
                    binding.etNote.setText(event.form.note)
                    checkType(event.form.type)
                }
                AddEditTransactionViewModel.Event.Saved -> {
                    Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun checkType(type: TransactionType) {
        val id = if (type == TransactionType.INCOME) R.id.btnIncome else R.id.btnExpense
        if (binding.toggleType.checkedButtonId != id) binding.toggleType.check(id)
    }

    companion object {
        private const val REQUEST_CATEGORY = "add_edit_category"
        private const val REQUEST_DATE = "add_edit_date"

        fun newIntent(
            context: Context,
            transactionId: Long = AddEditTransactionViewModel.NEW_ID
        ) = Intent(context, AddEditTransactionActivity::class.java)
            .putExtra(AddEditTransactionViewModel.EXTRA_TRANSACTION_ID, transactionId)
    }
}
