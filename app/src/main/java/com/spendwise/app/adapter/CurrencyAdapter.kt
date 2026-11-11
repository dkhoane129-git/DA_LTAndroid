package com.spendwise.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.spendwise.app.R
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spendwise.app.databinding.ItemCurrencyBinding
import com.spendwise.app.util.Formatters
import com.spendwise.app.viewmodel.CurrencyViewModel.RateRow

class CurrencyAdapter : ListAdapter<RateRow, CurrencyAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    /** Hủy request Glide khi ViewHolder bị tái sử dụng → không hiện nhầm ảnh, không giữ bitmap thừa. */
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    class ViewHolder(private val binding: ItemCurrencyBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: RateRow): Unit = with(binding) {
            tvCode.text = row.rate.code
            tvName.text = row.rate.name
            tvConverted.text = Formatters.foreign(row.converted, row.rate.code)
            tvRate.text = "1 ${row.rate.code} = ${Formatters.money(Math.round(1 / row.rate.rate))}"
            Glide.with(ivFlag)
                .load(row.rate.flagUrl)
                .override(FLAG_WIDTH, FLAG_HEIGHT)   // tải đúng kích thước hiển thị, tiết kiệm RAM
                .placeholder(R.color.divider)
                .error(R.color.divider)
                .into(ivFlag)
            Unit
        }

        fun clear(): Unit = Glide.with(binding.ivFlag).clear(binding.ivFlag)
    }

    private companion object {
        const val FLAG_WIDTH = 96
        const val FLAG_HEIGHT = 64

        val DIFF = object : DiffUtil.ItemCallback<RateRow>() {
            override fun areItemsTheSame(old: RateRow, new: RateRow) = old.rate.code == new.rate.code
            override fun areContentsTheSame(old: RateRow, new: RateRow) = old == new
        }
    }
}
