package com.spendwise.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.spendwise.app.R
import com.spendwise.app.databinding.FragmentForgotPasswordBinding
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.ForgotPasswordViewModel

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels { ForgotPasswordViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnSend.setOnClickListener {
            viewModel.sendReset(binding.etEmail.text?.toString().orEmpty())
        }

        viewModel.uiState.collectWithLifecycle(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.isLoading
            binding.btnSend.isEnabled = !state.isLoading
            binding.tilEmail.error = state.emailError?.let(::getString)
            state.message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.messageShown()
            }
            if (state.isSuccess) {
                Toast.makeText(requireContext(), R.string.msg_reset_sent, Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
