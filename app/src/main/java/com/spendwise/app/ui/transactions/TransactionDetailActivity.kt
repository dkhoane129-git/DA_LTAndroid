package com.spendwise.app.ui.transactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.spendwise.app.R
import com.spendwise.app.databinding.ActivityTransactionDetailBinding
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.model.TransactionType
import com.spendwise.app.ui.common.ConfirmDialogFragment
import com.spendwise.app.util.Formatters
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.TransactionDetailViewModel

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding
    private val viewModel: TransactionDetailViewModel by viewModels { TransactionDetailViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        supportFragmentManager.setFragmentResultListener(REQUEST_DELETE, this) { _, _ ->
            viewModel.delete()
            Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show()
        }

        viewModel.state.collectWithLifecycle(this) { state ->
            when (state) {
                TransactionDetailViewModel.State.Loading -> Unit
                TransactionDetailViewModel.State.NotFound -> finish()
                is TransactionDetailViewModel.State.Loaded -> render(state.item)
            }
        }
    }

    private fun render(item: TransactionItem) = with(binding) {
        val t = item.transaction
        tvIcon.text = item.category?.icon ?: "❔"
        tvTitle.text = t.title
        tvAmount.text = Formatters.signedMoney(t.amount, t.type)
        val color = if (t.type == TransactionType.INCOME) R.color.income else R.color.expense
        tvAmount.setTextColor(ContextCompat.getColor(this@TransactionDetailActivity, color))
        tvType.text = getString(if (t.type == TransactionType.INCOME) R.string.income else R.string.expense)
        tvCategory.text = item.category?.name ?: getString(R.string.no_category)
        tvDate.text = Formatters.date(t.date)
        tvNote.text = t.note.ifBlank { "—" }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_transaction_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit -> {
            startActivity(AddEditTransactionActivity.newIntent(this, viewModel.transactionId))
            true
        }
        R.id.action_delete -> {
            ConfirmDialogFragment.newInstance(
                REQUEST_DELETE,
                getString(R.string.confirm_delete_title),
                getString(R.string.confirm_delete_message)
            ).show(supportFragmentManager, ConfirmDialogFragment.TAG)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val REQUEST_DELETE = "detail_delete"

        fun newIntent(context: Context, transactionId: Long) =
            Intent(context, TransactionDetailActivity::class.java)
                .putExtra(TransactionDetailViewModel.EXTRA_TRANSACTION_ID, transactionId)
    }
}
