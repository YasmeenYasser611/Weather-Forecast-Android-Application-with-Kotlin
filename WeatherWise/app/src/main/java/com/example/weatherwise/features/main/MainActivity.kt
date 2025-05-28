package com.example.weatherwise.features.main

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
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





    fun updateLocale(languageCode: String) {
        if (preferencesManager.getLanguageCode() == languageCode) {
            return
        }

        // Save the new language
        preferencesManager.setLanguage(languageCode)

        // Update locale
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Update layout direction
        window.decorView.layoutDirection = if (languageCode == "ar") {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }

        // Update navigation drawer direction
        setupNavigationDrawer()

        // Notify fragments to update their UI
        notifyFragmentsOfLanguageChange()

        // Refresh activity UI (e.g., toolbar, navigation drawer menu)
        refreshActivityUI()
    }

    private fun refreshActivityUI() {
        // Update navigation drawer menu items
        binding.navView.menu.clear()
        binding.navView.inflateMenu(R.menu.nav_menu)

        // Update any other activity-level UI elements (e.g., toolbar title)
        // If you have a toolbar, update its title or other text
        // Example: setSupportActionBar(binding.toolbar)
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

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
            createConfigurationContext(config)
        } else {
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        // Update resources for the entire app
        resources.updateConfiguration(config, resources.displayMetrics)
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