package com.wolfyslayer.glyphmatrixtoy

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import com.wolfyslayer.glyphmatrixtoy.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main activity for the Glyph Matrix Toy
 * Displays time and battery status on the Nothing Phone 3's Glyph Matrix (16x32 display)
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val UPDATE_INTERVAL_MS = 1000L // Update every second
    }
    
    private lateinit var binding: ActivityMainBinding
    private var glyphManager: GlyphManager? = null
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var matrixRenderer: MatrixRenderer
    
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private var currentBatteryLevel = 0
    private var isCharging = false
    private var isFull = false
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updateDisplay()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize components
        batteryMonitor = BatteryMonitor(this)
        matrixRenderer = MatrixRenderer()
        
        // Set up button listeners
        binding.startButton.setOnClickListener {
            startDisplay()
        }
        
        binding.stopButton.setOnClickListener {
            stopDisplay()
        }
        
        // Initialize Glyph Manager
        try {
            glyphManager = GlyphManager.getInstance(applicationContext)
            glyphManager?.init { result ->
                runOnUiThread {
                    if (result.isSuccess) {
                        Log.d(TAG, "Glyph initialized successfully")
                        binding.statusText.text = getString(R.string.status_stopped)
                        binding.startButton.isEnabled = true
                    } else {
                        Log.e(TAG, "Glyph initialization failed: ${result.exceptionOrNull()}")
                        showError(getString(R.string.error_glyph_init_failed))
                    }
                }
            }
        } catch (e: GlyphException) {
            Log.e(TAG, "Glyph not supported", e)
            showError(getString(R.string.error_glyph_not_supported))
        }
        
        // Start battery monitoring
        batteryMonitor.start { status ->
            currentBatteryLevel = status.level
            isCharging = status.isCharging
            isFull = status.isFull
            updateUIInfo()
        }
        
        // Initially disable start button until Glyph is initialized
        binding.startButton.isEnabled = false
    }
    
    private fun startDisplay() {
        if (isRunning) return
        
        glyphManager?.openSession()
        isRunning = true
        matrixRenderer.resetAnimations()
        
        binding.statusText.text = getString(R.string.status_running)
        binding.startButton.isEnabled = false
        binding.stopButton.isEnabled = true
        
        handler.post(updateRunnable)
        Log.d(TAG, "Display started")
    }
    
    private fun stopDisplay() {
        if (!isRunning) return
        
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        
        // Turn off all LEDs
        glyphManager?.let { gm ->
            try {
                val frame = gm.glyphFrameBuilder
                    .buildChannelA()
                    .buildPeriod(1000)
                    .buildCycles(1)
                    .build()
                
                gm.displayGlyphFrame(frame)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing display", e)
            }
        }
        
        glyphManager?.closeSession()
        
        binding.statusText.text = getString(R.string.status_stopped)
        binding.startButton.isEnabled = true
        binding.stopButton.isEnabled = false
        
        Log.d(TAG, "Display stopped")
    }
    
    private fun updateDisplay() {
        val matrix = matrixRenderer.render(currentBatteryLevel, isCharging, isFull)
        val frameData = matrixRenderer.matrixToArray(matrix)
        
        glyphManager?.let { gm ->
            try {
                // Create a frame with the matrix data
                val frame = gm.glyphFrameBuilder
                    .buildChannelA()
                    .buildChannel(frameData)
                    .buildPeriod(1000)
                    .buildCycles(1)
                    .build()
                
                gm.displayGlyphFrame(frame)
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying frame", e)
            }
        }
    }
    
    private fun updateUIInfo() {
        runOnUiThread {
            binding.timeText.text = timeFormat.format(Date())
            binding.batteryText.text = "$currentBatteryLevel%"
            binding.chargingText.text = if (isCharging) {
                if (isFull) "Full" else "Yes"
            } else {
                "No"
            }
        }
    }
    
    private fun showError(message: String) {
        binding.statusText.text = message
        binding.startButton.isEnabled = false
        binding.stopButton.isEnabled = false
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopDisplay()
        batteryMonitor.stop()
        
        try {
            glyphManager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "Error uninitializing Glyph", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Optionally pause the display when app is not visible
        if (isRunning) {
            handler.removeCallbacks(updateRunnable)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Resume updates if we were running
        if (isRunning) {
            handler.post(updateRunnable)
        }
    }
}
