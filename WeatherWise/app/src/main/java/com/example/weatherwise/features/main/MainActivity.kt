package com.example.weatherwise.features.main

import android.content.Context
import android.content.ContextWrapper
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.weatherwise.R
import com.example.weatherwise.databinding.ActivityMainBinding
import com.example.weatherwise.features.mainscreen.view.HomeFragment
import com.example.weatherwise.features.settings.model.PreferencesManager
import com.example.weatherwise.features.settings.view.SettingsFragment
import com.example.weatherwise.features.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    lateinit var drawerLayout: DrawerLayout
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {


        preferencesManager = PreferencesManager(this)
        setAppLocale(preferencesManager.getLanguageCode())

        forceRtlIfArabic()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set layout direction for the entire activity
        updateLayoutDirection()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        drawerLayout = binding.drawerLayout
        setupNavigationDrawer()

        // Observe settings changes
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SettingsViewModel.SettingsEventBus.settingsChanged.collect {
                    refreshActivityUI()
                }
            }
        }
    }

    private fun forceRtlIfArabic() {
        val isRtl = preferencesManager.getLanguage() == PreferencesManager.LANGUAGE_ARABIC
        if (isRtl) {
            window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
    }

    private fun setupNavigationDrawer() {
        val isRtl = preferencesManager.getLanguage() == PreferencesManager.LANGUAGE_ARABIC

        // This is the key line - force the drawer to respect RTL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            binding.drawerLayout.layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        // Set navigation item selection
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            Handler(Looper.getMainLooper()).postDelayed({
                handleNavigation(menuItem.itemId)
            }, 200)
            true
        }

        // Inflate menu after setting direction
        binding.navView.menu.clear()
        binding.navView.inflateMenu(R.menu.nav_menu)
    }

    fun updateLocale(languageCode: String) {
        if (preferencesManager.getLanguageCode() == languageCode) return

        preferencesManager.setLanguage(languageCode)
        setAppLocale(languageCode)

        // This is crucial - recreate the activity to properly apply RTL changes
        recreate()
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


    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            createConfigurationContext(config)
        }
    }

    private fun updateLayoutDirection() {
        val isRtl = preferencesManager.getLanguage() == PreferencesManager.LANGUAGE_ARABIC
        window.decorView.layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        // Update layout direction for the root view
        binding.root.layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    fun refreshActivityUI() {
        // Update navigation drawer menu items
        binding.navView.menu.clear()
        binding.navView.inflateMenu(R.menu.nav_menu)

        // Force navigation view to refresh its layout
        binding.navView.invalidate()
        binding.navView.requestLayout()

        // Update drawer configuration
        setupNavigationDrawer()
    }

    private fun notifyFragmentsOfLanguageChange() {
        supportFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is HomeFragment -> fragment.updateLanguage()
                is SettingsFragment -> fragment.updateLanguage()
                // Add other fragments if needed
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.let {
            val uiMode = it.uiMode
            it.setTo(baseContext.resources.configuration)
            it.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun attachBaseContext(newBase: Context) {
        val preferencesManager = PreferencesManager(newBase)
        val languageCode = preferencesManager.getLanguageCode()
        val locale = Locale(languageCode)
        val newContext = ContextWrapper(newBase)

        val resources = newBase.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            newContext.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        super.attachBaseContext(newContext)
    }
}