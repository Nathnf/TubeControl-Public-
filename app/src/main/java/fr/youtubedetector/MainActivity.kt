package fr.youtubedetector

import android.app.Activity
import android.app.AppOpsManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main) // old activity
        setContentView(R.layout.activity_main_swipe)

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val fragments = listOf(HomeFragment(), TutoFragment(), InfoFragment())
        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Monitoring"
                1 -> "Tutorial"
                2 -> "About"
                else -> "Tab $position"
            }
        }.attach()

        // Contact button
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        emailTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:tubecontrolapp@gmail.com")
            startActivity(intent)
        }
    }

    private fun showToastAndLog(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("ShowToastAndLog", message)
    }

    fun showGreenSnackbar(view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.GREEN)
        snackbar.show()
    }
}
