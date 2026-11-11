package com.spendwise.app.ui.profile

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.spendwise.app.R
import com.spendwise.app.adapter.CategoryAdapter
import com.spendwise.app.databinding.ActivityCategoryBinding
import com.spendwise.app.databinding.DialogAddCategoryBinding
import com.spendwise.app.model.TransactionType
import com.spendwise.app.ui.common.ConfirmDialogFragment
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.CategoryViewModel

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private val viewModel: CategoryViewModel by viewModels { CategoryViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = CategoryAdapter(
            onItemClick = { },
            onDeleteClick = { category ->
                ConfirmDialogFragment.newInstance(
                    REQUEST_DELETE,
                    getString(R.string.confirm_delete_category, category.name),
                    null,
                    payload = category.id
                ).show(supportFragmentManager, ConfirmDialogFragment.TAG)
            }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        supportFragmentManager.setFragmentResultListener(REQUEST_DELETE, this) { _, bundle ->
            viewModel.deleteCategory(bundle.getLong(ConfirmDialogFragment.RESULT_PAYLOAD))
        }

        binding.toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.selectType(if (checkedId == R.id.btnIncome) TransactionType.INCOME else TransactionType.EXPENSE)
        }
        binding.fabAdd.setOnClickListener { showAddDialog() }

        viewModel.selectedType.collectWithLifecycle(this) { type ->
            val id = if (type == TransactionType.INCOME) R.id.btnIncome else R.id.btnExpense
            if (binding.toggleType.checkedButtonId != id) binding.toggleType.check(id)
        }
        viewModel.categories.collectWithLifecycle(this) { adapter.submitList(it) }
        viewModel.messages.collectWithLifecycle(this) { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_category)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.addCategory(
                    dialogBinding.etName.text?.toString().orEmpty(),
                    dialogBinding.etIcon.text?.toString().orEmpty()
                )
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private companion object {
        const val REQUEST_DELETE = "category_delete"
    }
}
