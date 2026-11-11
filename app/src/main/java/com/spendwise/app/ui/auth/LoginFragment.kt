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
import com.spendwise.app.R
import com.spendwise.app.databinding.FragmentLoginBinding
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.LoginViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels { LoginViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etEmail.text?.toString().orEmpty(),
                binding.etPassword.text?.toString().orEmpty()
            )
        }
        binding.btnGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        binding.btnForgot.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }

        viewModel.uiState.collectWithLifecycle(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.isLoading
            binding.btnLogin.isEnabled = !state.isLoading
            binding.tilEmail.error = state.emailError?.let(::getString)
            binding.tilPassword.error = state.passwordError?.let(::getString)
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
