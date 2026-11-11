package com.spendwise.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.spendwise.app.databinding.FragmentRegisterBinding
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.RegisterViewModel

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels { RegisterViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.etName.text?.toString().orEmpty(),
                binding.etEmail.text?.toString().orEmpty(),
                binding.etPassword.text?.toString().orEmpty(),
                binding.etConfirm.text?.toString().orEmpty()
            )
        }
        binding.btnGoLogin.setOnClickListener { findNavController().popBackStack() }

        viewModel.uiState.collectWithLifecycle(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.isLoading
            binding.btnRegister.isEnabled = !state.isLoading
            binding.tilName.error = state.nameError?.let(::getString)
            binding.tilEmail.error = state.emailError?.let(::getString)
            binding.tilPassword.error = state.passwordError?.let(::getString)
            binding.tilConfirm.error = state.confirmError?.let(::getString)
            state.message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.messageShown()
            }
            if (state.isSuccess) AuthActivity.goToMain(requireActivity())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
