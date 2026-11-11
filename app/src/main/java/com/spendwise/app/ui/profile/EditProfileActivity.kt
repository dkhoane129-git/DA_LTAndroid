package com.spendwise.app.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.spendwise.app.R
import com.spendwise.app.databinding.ActivityEditProfileBinding
import com.spendwise.app.viewmodel.EditProfileViewModel

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels { EditProfileViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState != null) viewModel.formFilled = true
        viewModel.user.observe(this) { user ->
            if (user == null) return@observe
            binding.etEmail.setText(user.email)
            if (!viewModel.formFilled) {
                binding.etName.setText(user.fullName)
                binding.etPhone.setText(user.phone)
                viewModel.formFilled = true
            }
        }

        binding.btnSave.setOnClickListener {
            viewModel.save(
                binding.etName.text?.toString().orEmpty(),
                binding.etPhone.text?.toString().orEmpty()
            )
        }

        viewModel.nameError.observe(this) { error ->
            binding.tilName.error = error?.let(::getString)
        }
        viewModel.saved.observe(this) { saved ->
            if (saved) {
                Toast.makeText(this, R.string.msg_profile_saved, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
