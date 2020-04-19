package com.ahmed.android_mvi.tasks

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.ahmed.android_mvi.R
import com.ahmed.android_mvi.util.addFragmentToActivity
import com.google.android.material.navigation.NavigationView

class TasksActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.run {
            setHomeAsUpIndicator(R.drawable.ic_menu)
            setDisplayHomeAsUpEnabled(true)
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setStatusBarBackground(R.color.colorPrimaryDark)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        if (navigationView != null) {
            setupDrawerContent(navigationView)
        }

        if (supportFragmentManager.findFragmentById(R.id.contentFrame) == null) {
            addFragmentToActivity(supportFragmentManager, TasksFragment(), R.id.contentFrame)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Open the navigation drawer when the home icon is selected from the toolbar.
                drawerLayout.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.list_navigation_menu_item -> {
                    // Do nothing, we're already on that screen
                }
                else -> {
                }
            }
            // Close the navigation drawer when an item is selected.
            menuItem.isChecked = true
            drawerLayout.closeDrawers()
            true
        }
    }
}
