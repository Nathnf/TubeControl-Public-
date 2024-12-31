package fr.youtubedetector

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.TreeMap


class YoutubeMonitoring : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var monitorTask: MyRunnable? = null

    inner class MyRunnable(private val frequency: Int) : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            // Logs
            Log.d("INFO", "Frequency : $frequency")
            Toast.makeText(applicationContext, "Frequency : $frequency", Toast.LENGTH_LONG)
                .show()
            // Verification
            verifyYouTubeScrolling()
            // Schedule the next executions
            handler.postDelayed(this, frequency.toLong())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_MONITORING") {
            Log.d("MONITORING INFO", "Starting monitoring")
            // Getting the frequency
            var frequency = intent.getIntExtra("FREQUENCY", 2)
            frequency *= 60*1000            // conversion to milliseconds
            Log.d("INFO", "Frequency : $frequency")
            Toast.makeText(applicationContext, "Frequency : $frequency", Toast.LENGTH_LONG).show()
            // Loop over the monitoring task
            monitorTask = MyRunnable(frequency)
            handler.post(monitorTask!!)
            // Start foreground service (permanent notification)
            // showStopOverlay() // to test if it works
            startForeground()
        }  else if (intent?.action == "STOP_MONITORING") {
            Log.d("MONITORING INFO", "Ending monitoring")
            monitorTask?.let {
                handler.removeCallbacks(it)
            }
            stopAppMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun verifComponent(verifTime: Int): Boolean {
        val packageName = getForegroundApplication(this@YoutubeMonitoring, verifTime)
        val currentOrientation = resources.configuration.orientation
        
        // Logging
        /*
        val booleanResult = (packageName=="YouTube" && currentOrientation==1)        
        Log.d("VERIF", "Package Name: $packageName, Orientation: $currentOrientation, Result: $booleanResult")
        Toast.makeText(
            applicationContext,
            "Package Name: $packageName, Orientation: $currentOrientation, Result: $booleanResult",
            Toast.LENGTH_LONG
        ).show()
        return booleanResult
        */

        return packageName == "YouTube" && currentOrientation == 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifyYouTubeScrolling() {
        val durationShortVerif = 5000
        val durationLongVerif = 10000
        val handler = Handler(Looper.getMainLooper())

        // Two verifications
        /*
        // We execute the short verif and if needed the long one
        if (verifComponent(durationShortVerif)) {
            Log.d("VERIF", "YouTube scrolling detected in the short task")
            showStopOverlay()
        } else {
            handler.postDelayed({
                Log.d("VERIF", "Didn't detect with the short task")
                if (verifComponent(durationLongVerif)) {showStopOverlay()}
            }, durationLongVerif.toLong())
        }
         */

        // Only one verification (seemed to work similarly)
        if (verifComponent(durationShortVerif)) {
            Log.d("VERIF", "YouTube scrolling detected in the short task")
            showStopOverlay()
        }
    }

    @SuppressLint("InflateParams")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showStopOverlay() {
        Log.d("LAYOUT", "We are in the showStopOverlay function")
        // Inflate the overlay layout from XML
        val overlayView = LayoutInflater.from(this).inflate(R.layout.test_pop_up, null)
        Log.d("Overlay", "Layout inflated: $overlayView")

        val buttonClose = overlayView.findViewById<Button>(R.id.noButton_)
        val yesButton = overlayView.findViewById<Button>(R.id.yesButton_)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        // Add the overlay view to the window manager
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, layoutParams)

        buttonClose.setOnClickListener {
            // Close the overlay when the button is clicked
            windowManager.removeView(overlayView)
            showNoOverlay()
        }

        yesButton.setOnClickListener {
            // Remove overlay
            windowManager.removeView(overlayView)

            // Return to home screen
            val homeScreenIntent = Intent(Intent.ACTION_MAIN)
            homeScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            homeScreenIntent.addCategory(Intent.CATEGORY_HOME)
            startActivity(homeScreenIntent)

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNoOverlay() {
        Log.d("Overlay", "showNoOverlay layout inflated")
        // Inflate the no_overlay layout from XML
        val noOverlayView = LayoutInflater.from(this).inflate(R.layout.no_overlay, null)

        // Add the no_overlay view to the window manager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(noOverlayView, layoutParams)

        // Close the no_overlay pop-up after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            windowManager.removeView(noOverlayView)
        }, 5000)
    }

    private fun startForeground() {
        val channelId = "YourChannelId"
        createNotificationChannel(channelId)

        // Intent to open the main activity
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.logo)
            .setContentTitle("Monitoring")
            .setContentText("Go back to the application to disable monitoring.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Your Channel Name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getForegroundApplication(context: Context, milliseconds: Int) : String? {
        Log.d("INFO", "Checking apps being used")
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            currentTime - milliseconds,
            currentTime
        )
        if (usageStats != null) {
            val sortedMap = TreeMap<Long, UsageStats>()
            for (usageStat in usageStats) {
                val time: Long = usageStat.lastTimeUsed
                sortedMap.put(time, usageStat)
            }
            if (sortedMap.isNotEmpty()) {
                val lastKey = sortedMap.lastKey()
                val foregroundAppUsageStats = sortedMap[lastKey]
                if (foregroundAppUsageStats != null && foregroundAppUsageStats.packageName != null) {
                    val applicationName = context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(
                            foregroundAppUsageStats.packageName,
                            0
                        )
                    ).toString()
                    Log.d("INFO", applicationName)
                    return applicationName
                }
            }
        }
            return null
    }

    private fun stopAppMonitoring() {
        Log.d("INFO", "In the stopAppMonitoring")
        stopService(Intent(this, YoutubeMonitoring::class.java).apply {
            action = "STOP_MONITORING"
        })
    }
}



