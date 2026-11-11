package com.spendwise.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.databinding.ItemCategoryBinding
import com.spendwise.app.model.Category

/**
 * Dùng chung cho CategoryPickerBottomSheet (chọn) và CategoryActivity (quản lý, có nút xóa).
 * Truyền [onDeleteClick] = null để ẩn nút xóa.
 */
class CategoryAdapter(
    private val onItemClick: (Category) -> Unit,
    private val onDeleteClick: ((Category) -> Unit)? = null
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) = with(binding) {
            tvIcon.text = category.icon
            tvName.text = category.name
            btnDelete.isVisible = onDeleteClick != null
            btnDelete.setOnClickListener { onDeleteClick?.invoke(category) }
            root.setOnClickListener { onItemClick(category) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(old: Category, new: Category) = old.id == new.id
            override fun areContentsTheSame(old: Category, new: Category) = old == new
        }
    }
}
