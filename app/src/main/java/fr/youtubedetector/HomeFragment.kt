package fr.youtubedetector

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar


class HomeFragment : Fragment() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var switchButton: Switch
    private lateinit var buttonOverlay: Button
    private lateinit var buttonUsage: Button

    private val requestOverlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("INFO", result.resultCode.toString())
        Log.d("INFO", Activity.RESULT_OK.toString())
        if (result.resultCode == Activity.RESULT_OK) {
            // Overlay permission request was handled
            if (Settings.canDrawOverlays(requireContext())) {
                // Overlay permission granted
                Log.d("INFO OVERLAY", "Permission Granted")
            } else {
                // Overlay permission not granted
                Log.d("INFO OVERLAY", "Permission not Granted")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Saved parameters
        sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)

        // Frequency - V2
        var frequency = sharedPref.getInt(getString(R.string.saved_frequency_key), 2)
        var spinner = view.findViewById<NumberPicker>(R.id.frequencySpinner)
        spinner.minValue = 1
        spinner.maxValue = 60
        spinner.value = frequency
        spinner.wrapSelectorWheel = true
        spinner.setOnValueChangedListener { _, _, newVal ->
            frequency = newVal
            saveFrequencySetting(frequency)
        }

        // Switch Management
        switchButton = view.findViewById(R.id.switchButton)

        // Load switch state from SharedPreferences
        val switchState = sharedPref.getBoolean(getString(R.string.switch_state_key), false)
        switchButton.isChecked = switchState


        switchButton.isEnabled = enableSwitch()
        buttonUsage = view.findViewById(R.id.buttonUsage)
        buttonOverlay = view.findViewById(R.id.buttonOverlay)

        buttonOverlay.setOnClickListener {
            // Draw over other apps - Ask for permission if not already granted
            if (!isOverlayAccessGranted()) {
                showToast("Overlay access is needed.", Toast.LENGTH_LONG)
                requestOverlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        }

        buttonUsage.setOnClickListener {
            // Usage stats - Ask for permission if not already granted
            if (!isUsageAccessGranted()) {
                showToast("Usage access is needed.", Toast.LENGTH_LONG)
                requestUsageAccess()
            }
        }

        switchButton.setOnCheckedChangeListener { _, isChecked ->

            val message = if (isChecked) "Switch:ON" else "Switch:OFF"
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

            // Save switch state to SharedPreferences
            saveSwitchState(isChecked)

            // Start or stop monitoring service
            if (isChecked) {
                Log.d("INFO", "Button Checked")
                startAppMonitoring(frequency)
                val homeScreenIntent = Intent(Intent.ACTION_MAIN)
                homeScreenIntent.addCategory(Intent.CATEGORY_HOME)
                startActivity(homeScreenIntent)
            } else {
                Log.d("INFO", "Button Unchecked")
                stopAppMonitoring()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        if (isOverlayAccessGranted()) {
            buttonOverlay.setBackgroundColor(Color.GREEN)
        } else {
            buttonOverlay.setBackgroundColor(Color.RED)
        }
        if (isUsageAccessGranted()) {
            buttonUsage.setBackgroundColor(Color.GREEN)
        } else {
            buttonUsage.setBackgroundColor(Color.RED)
        }
        switchButton.isEnabled = enableSwitch()
    }

    private fun saveSwitchState(isChecked: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(getString(R.string.switch_state_key), isChecked)
            apply()
        }
    }

    private fun saveFrequencySetting(frequency: Int) {
        with(sharedPref.edit()) {
            putInt(getString(R.string.saved_frequency_key), frequency)
            apply()
        }
    }

    private fun startAppMonitoring(frequency: Int) {
        val intent = Intent(requireActivity(), YoutubeMonitoring::class.java)
        intent.action = "START_MONITORING"
        intent.putExtra("FREQUENCY", frequency)
        requireActivity().startService(intent)
    }

    private fun stopAppMonitoring() {
        val intent = Intent(requireActivity(), YoutubeMonitoring::class.java)
        intent.action = "STOP_MONITORING"
        requireActivity().startService(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun enableSwitch(): Boolean {
        return (isOverlayAccessGranted() and isUsageAccessGranted())
    }

    private fun isOverlayAccessGranted(): Boolean {
        return Settings.canDrawOverlays(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isUsageAccessGranted(): Boolean {
        try {
            val appOpsManager = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

            // Use reflection to check for the method
            val method = appOpsManager.javaClass.getMethod(
                "unsafeCheckOpNoThrow",
                String::class.java,
                Int::class.javaPrimitiveType,
                String::class.java
            )

            val mode = method.invoke(appOpsManager, AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().packageName) as Int
            return mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e("UsageAccess", "Error checking usage access", e)
            return false
        }
    }

    private fun requestUsageAccess() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
        Log.d("INFO_req", "Usage Access Requested")
    }

    private fun showToast(text: String, length: Int) {
        Toast.makeText(
            requireContext(), text,
            length
        ).show()
    }
}
