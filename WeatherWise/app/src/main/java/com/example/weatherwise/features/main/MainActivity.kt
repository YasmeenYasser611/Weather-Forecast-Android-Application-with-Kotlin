package com.example.weatherwise.features.main

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.weatherwise.R
import com.example.weatherwise.databinding.ActivityMainBinding
import com.example.weatherwise.features.mainscreen.view.HomeFragment
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.view.SettingsFragment
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    lateinit var drawerLayout: DrawerLayout
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        preferencesManager = PreferencesManager(this)
        setAppLocale(preferencesManager.getLanguageCode())

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (preferencesManager.getLanguage() == PreferencesManager.LANGUAGE_ARABIC) {
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        } else {
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        drawerLayout = binding.drawerLayout
        setupNavigationDrawer()
    }

    private fun setupNavigationDrawer() {
        val isRtl = preferencesManager.getLanguage() == PreferencesManager.LANGUAGE_ARABIC
        val drawerGravity = if (isRtl) GravityCompat.END else GravityCompat.START

        binding.drawerLayout.apply {
            if (isRtl) {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
            } else {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
            }
        }

        val navViewLayoutParams = binding.navView.layoutParams as DrawerLayout.LayoutParams
        navViewLayoutParams.gravity = drawerGravity
        binding.navView.layoutParams = navViewLayoutParams

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(drawerGravity)
            Handler(Looper.getMainLooper()).postDelayed({
                handleNavigation(menuItem.itemId)
            }, 200)
            true
        }
    }

    private fun handleNavigation(menuItemId: Int) {
        try {
            when (menuItemId) {
                R.id.nav_home -> {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        navController.navigate(R.id.homeFragment)
                    }
                }
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                R.id.nav_fav -> navController.navigate(R.id.favoritesFragment)
                R.id.nav_alarms -> navController.navigate(R.id.alertsFragment)
            }
        } catch (e: Exception) {
            Log.e("NavigationError", "Navigation failed: ${e.message}", e)
            android.widget.Toast.makeText(this, "Navigation error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    fun updateLocale(languageCode: String) {
        if (preferencesManager.getLanguageCode() == languageCode) {
            Log.d("Localization", "Language already set to $languageCode")
            return
        }

        Log.d("Localization", "Updating locale to: $languageCode")
        preferencesManager.setLanguage(languageCode)
        setAppLocale(languageCode)

        val layoutDirection = if (languageCode == "ar") {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
        window.decorView.layoutDirection = layoutDirection

        setupNavigationDrawer()
        binding.navView.menu.clear()
        binding.navView.inflateMenu(R.menu.nav_menu)
        notifyFragmentsOfLanguageChange()

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("Localization", "Recreating activity")
            recreate()
        }, 300)
    }

    private fun notifyFragmentsOfLanguageChange() {
        supportFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is HomeFragment -> fragment.updateLanguage()
                is SettingsFragment -> fragment.updateLanguage()
            }
        }
    }
}