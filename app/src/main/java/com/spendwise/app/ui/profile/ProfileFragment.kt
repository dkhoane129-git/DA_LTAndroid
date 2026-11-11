package com.spendwise.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.spendwise.app.R
import com.spendwise.app.databinding.FragmentProfileBinding
import com.spendwise.app.ui.auth.AuthActivity
import com.spendwise.app.ui.common.ConfirmDialogFragment
import com.spendwise.app.ui.currency.CurrencyActivity
import com.spendwise.app.ui.sync.CloudSyncActivity
import com.spendwise.app.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels { ProfileViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // LiveData: observe theo viewLifecycleOwner
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.tvName.text = user?.fullName.orEmpty()
            binding.tvEmail.text = user?.email.orEmpty()
            // Glide tải ảnh bất đồng bộ, gắn với vòng đời Fragment nên tự hủy khi rời màn hình
            Glide.with(this)
                .load(user?.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.bg_icon_circle)
                .error(R.drawable.ic_person)
                .into(binding.ivAvatar)
        }
        viewModel.transactionCount.observe(viewLifecycleOwner) { count ->
            binding.tvCount.text = getString(R.string.profile_transactions, count)
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        binding.btnCategories.setOnClickListener {
            startActivity(Intent(requireContext(), CategoryActivity::class.java))
        }

        binding.btnCurrency.setOnClickListener {
            startActivity(Intent(requireContext(), CurrencyActivity::class.java))
        }
        binding.btnCloudSync.setOnClickListener {
            startActivity(Intent(requireContext(), CloudSyncActivity::class.java))
        }
        binding.btnSampleData.setOnClickListener {
            viewModel.addSampleData()
            Snackbar.make(binding.root, R.string.msg_sample_added, Snackbar.LENGTH_SHORT).show()
        }

        childFragmentManager.setFragmentResultListener(REQUEST_LOGOUT, viewLifecycleOwner) { _, _ ->
            viewModel.logout()
            startActivity(
                Intent(requireContext(), AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        binding.btnLogout.setOnClickListener {
            ConfirmDialogFragment.newInstance(
                REQUEST_LOGOUT,
                getString(R.string.confirm_logout_title),
                getString(R.string.confirm_logout_message)
            ).show(childFragmentManager, ConfirmDialogFragment.TAG)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val REQUEST_LOGOUT = "profile_logout"
    }
}
