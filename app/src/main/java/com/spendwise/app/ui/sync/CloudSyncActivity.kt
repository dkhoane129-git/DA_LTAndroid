package com.spendwise.app.ui.sync

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.spendwise.app.R
import com.spendwise.app.adapter.TransactionAdapter
import com.spendwise.app.databinding.ActivityCloudSyncBinding
import com.spendwise.app.ui.common.ConfirmDialogFragment
import com.spendwise.app.ui.common.render
import com.spendwise.app.util.Resource
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.CloudSyncViewModel

/** Phân hệ Đồng bộ: GET / POST / PUT / DELETE lên Firebase Realtime Database (REST). */
class CloudSyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCloudSyncBinding
    private val viewModel: CloudSyncViewModel by viewModels { CloudSyncViewModel.Factory }

    /** remoteId đang chờ xác nhận xóa (giữ qua xoay màn hình). */
    private var pendingDeleteKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCloudSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        pendingDeleteKey = savedInstanceState?.getString(KEY_PENDING)

        val adapter = TransactionAdapter { item ->
            val key = item.transaction.remoteId ?: return@TransactionAdapter
            pendingDeleteKey = key
            ConfirmDialogFragment.newInstance(
                REQUEST_DELETE,
                getString(R.string.confirm_delete_remote),
                getString(R.string.confirm_delete_remote_message)
            ).show(supportFragmentManager, ConfirmDialogFragment.TAG)
        }
        binding.rvRemote.layoutManager = LinearLayoutManager(this)
        binding.rvRemote.adapter = adapter

        supportFragmentManager.setFragmentResultListener(REQUEST_DELETE, this) { _, _ ->
            pendingDeleteKey?.let(viewModel::deleteRemote)
            pendingDeleteKey = null
        }

        binding.btnUpload.setOnClickListener { viewModel.syncNow() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        viewModel.remote.collectWithLifecycle(this) { state ->
            binding.stateView.render(state, hasData = false) { viewModel.refresh() }
            if (state !is Resource.Loading) binding.swipeRefresh.isRefreshing = false
            val items = (state as? Resource.Success)?.data.orEmpty()
            adapter.submitList(items)
            binding.tvEmpty.isVisible = state is Resource.Success && items.isEmpty()
        }
        viewModel.busy.collectWithLifecycle(this) { busy ->
            binding.btnUpload.isEnabled = !busy
        }
        viewModel.messages.collectWithLifecycle(this) { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING, pendingDeleteKey)
    }

    private companion object {
        const val REQUEST_DELETE = "cloud_delete"
        const val KEY_PENDING = "pending_delete"
    }
}
