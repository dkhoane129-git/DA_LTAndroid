package com.spendwise.app.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.spendwise.app.databinding.ActivitySplashBinding
import com.spendwise.app.ui.auth.AuthActivity
import com.spendwise.app.ui.main.MainActivity
import com.spendwise.app.util.collectWithLifecycle
import com.spendwise.app.viewmodel.SplashViewModel

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels { SplashViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.destination.collectWithLifecycle(this) { destination ->
            val target = when (destination) {
                SplashViewModel.Destination.MAIN -> MainActivity::class.java
                SplashViewModel.Destination.AUTH -> AuthActivity::class.java
                null -> return@collectWithLifecycle
            }
            startActivity(Intent(this, target))
            finish()
        }
    }
}
