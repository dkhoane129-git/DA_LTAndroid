package com.spendwise.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.databinding.ItemCategoryStatBinding
import com.spendwise.app.model.CategoryStat
import com.spendwise.app.util.Formatters

class CategoryStatAdapter : ListAdapter<CategoryStat, CategoryStatAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemCategoryStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stat: CategoryStat) = with(binding) {
            tvIcon.text = stat.category.icon
            tvName.text = stat.category.name
            tvAmount.text = Formatters.money(stat.total)
            tvPercent.text = "${stat.percent}%"
            progress.setProgressCompat(stat.percent, false)
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CategoryStat>() {
            override fun areItemsTheSame(old: CategoryStat, new: CategoryStat) = old.category.id == new.category.id
            override fun areContentsTheSame(old: CategoryStat, new: CategoryStat) = old == new
        }
    }
}
