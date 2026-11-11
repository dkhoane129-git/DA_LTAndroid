package com.spendwise.app.ui.main

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.spendwise.app.R
import com.spendwise.app.databinding.ActivityMainBinding

/** BottomNavigationView + Navigation Component điều hướng 4 tab chính. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)
    }

    /** Cho phép Fragment con chuyển tab (vd: nút "Xem tất cả" ở Home). */
    fun selectTab(@IdRes menuItemId: Int) {
        binding.bottomNav.selectedItemId = menuItemId
    }
}
