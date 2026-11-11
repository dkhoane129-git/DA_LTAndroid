package com.spendwise.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.R
import com.spendwise.app.databinding.ItemTransactionBinding
import com.spendwise.app.model.TransactionItem
import com.spendwise.app.model.TransactionType
import com.spendwise.app.util.Formatters

class TransactionAdapter(
    private val onItemClick: (TransactionItem) -> Unit
) : ListAdapter<TransactionItem, TransactionAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionItem) = with(binding) {
            val t = item.transaction
            tvIcon.text = item.category?.icon ?: "❔"
            tvTitle.text = t.title
            tvSubtitle.text = root.context.getString(
                R.string.transaction_subtitle,
                item.category?.name ?: root.context.getString(R.string.no_category),
                Formatters.date(t.date)
            )
            tvAmount.text = Formatters.signedMoney(t.amount, t.type)
            val color = if (t.type == TransactionType.INCOME) R.color.income else R.color.expense
            tvAmount.setTextColor(ContextCompat.getColor(root.context, color))
            root.setOnClickListener { onItemClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TransactionItem>() {
            override fun areItemsTheSame(old: TransactionItem, new: TransactionItem) =
                old.transaction.id == new.transaction.id

            override fun areContentsTheSame(old: TransactionItem, new: TransactionItem) = old == new
        }
    }
}
