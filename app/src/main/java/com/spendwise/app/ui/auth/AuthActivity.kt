package com.spendwise.app.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.spendwise.app.databinding.ActivityAuthBinding
import com.spendwise.app.ui.main.MainActivity

/** Chứa NavHost cho Login / Register / ForgotPassword. */
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        /** Đăng nhập xong: mở Main và xóa back stack để không Back về Login được. */
        fun goToMain(activity: Activity) {
            activity.startActivity(
                Intent(activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }
}
