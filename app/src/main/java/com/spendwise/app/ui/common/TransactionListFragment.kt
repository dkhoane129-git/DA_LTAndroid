package com.spendwise.app.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.spendwise.app.adapter.TransactionAdapter
import com.spendwise.app.databinding.FragmentTransactionListBinding
import com.spendwise.app.ui.transactions.TransactionDetailActivity
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.TransactionListSource

/**
 * Fragment dùng chung hiển thị danh sách giao dịch (ở Home và Transactions).
 * Fragment cha phải implement [Host] để cung cấp nguồn dữ liệu từ ViewModel của nó.
 */
class TransactionListFragment : Fragment() {

    interface Host {
        val transactionListSource: TransactionListSource
    }

    private var _binding: FragmentTransactionListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = TransactionAdapter { item ->
            startActivity(TransactionDetailActivity.newIntent(requireContext(), item.transaction.id))
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        val source = (requireParentFragment() as Host).transactionListSource
        source.transactionItems.collectWithLifecycle(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.tvEmpty.isVisible = items.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
