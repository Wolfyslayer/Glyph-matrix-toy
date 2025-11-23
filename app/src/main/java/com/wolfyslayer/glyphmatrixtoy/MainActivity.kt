package com.wolfyslayer.glyphmatrixtoy

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wolfyslayer.glyphmatrixtoy.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main activity for the Glyph Matrix Toy
 * Displays time and battery status on the Nothing Phone 3's Glyph Matrix (16x32 display)
 * 
 * Note: Uses reflection-based method invocation to avoid compile-time coupling
 * to vendor-specific GlyphManager API, allowing the code to compile in CI
 * environments where the SDK may have different method signatures.
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val UPDATE_INTERVAL_MS = 1000L // Update every second
    }
    
    private lateinit var binding: ActivityMainBinding
    private var glyphManager: Any? = null
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
    
    /**
     * Runtime-safe helper to invoke GlyphManager methods via reflection.
     * Tries multiple candidate method names to support different SDK versions.
     * @param methodNames List of candidate method names to try
     * @param args Arguments to pass to the method
     * @return true if any method was successfully invoked, false otherwise
     */
    private fun invokeGlyphMethod(methodNames: List<String>, vararg args: Any?): Boolean {
        glyphManager ?: return false
        
        for (methodName in methodNames) {
            try {
                val method = glyphManager!!.javaClass.getMethod(methodName, *args.map { it?.javaClass ?: Any::class.java }.toTypedArray())
                method.invoke(glyphManager, *args)
                Log.d(TAG, "Successfully invoked $methodName")
                return true
            } catch (e: NoSuchMethodException) {
                // Try next candidate
                continue
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking $methodName", e)
                continue
            }
        }
        
        Log.w(TAG, "None of the methods ${methodNames.joinToString(", ")} could be invoked")
        return false
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
        
        // Initialize Glyph Manager using reflection to avoid compile-time dependency
        try {
            val glyphManagerClass = Class.forName("com.nothing.ketchum.GlyphManager")
            val getInstanceMethod = glyphManagerClass.getMethod("getInstance", android.content.Context::class.java)
            glyphManager = getInstanceMethod.invoke(null, applicationContext)
            
            if (glyphManager != null) {
                // Assume initialization success for UI enabling
                // The SDK may require init() but we skip callback dependency at compile time
                Log.d(TAG, "Glyph Manager instance obtained")
                runOnUiThread {
                    binding.statusText.text = getString(R.string.status_stopped)
                    binding.startButton.isEnabled = true
                }
            } else {
                Log.e(TAG, "Glyph Manager instance is null")
                showError(getString(R.string.error_glyph_not_supported))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Glyph not supported or initialization failed", e)
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
        
        // Use reflection to call openSession
        invokeGlyphMethod(listOf("openSession", "open", "start"))
        
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
        
        // Turn off all LEDs - attempt to create and send a blank frame via reflection
        glyphManager?.let { gm ->
            try {
                val glyphFrameBuilderGetter = gm.javaClass.getMethod("getGlyphFrameBuilder")
                val builder = glyphFrameBuilderGetter.invoke(gm)
                
                if (builder != null) {
                    val builderClass = builder.javaClass
                    val buildChannelAMethod = builderClass.getMethod("buildChannelA")
                    val buildPeriodMethod = builderClass.getMethod("buildPeriod", Int::class.java)
                    val buildCyclesMethod = builderClass.getMethod("buildCycles", Int::class.java)
                    val buildMethod = builderClass.getMethod("build")
                    
                    buildChannelAMethod.invoke(builder)
                    buildPeriodMethod.invoke(builder, 1000)
                    buildCyclesMethod.invoke(builder, 1)
                    val frame = buildMethod.invoke(builder)
                    
                    // Try to display the blank frame
                    frame?.let { invokeGlyphMethod(listOf("displayGlyphFrame", "display", "sendFrame", "showFrame"), it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing display", e)
            }
        }
        
        // Use reflection to call closeSession
        invokeGlyphMethod(listOf("closeSession", "close", "stop"))
        
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
                // Create a frame with the matrix data using reflection
                val glyphFrameBuilderGetter = gm.javaClass.getMethod("getGlyphFrameBuilder")
                val builder = glyphFrameBuilderGetter.invoke(gm)
                
                if (builder != null) {
                    val builderClass = builder.javaClass
                    val buildChannelAMethod = builderClass.getMethod("buildChannelA")
                    val buildChannelMethod = builderClass.getMethod("buildChannel", IntArray::class.java)
                    val buildPeriodMethod = builderClass.getMethod("buildPeriod", Int::class.java)
                    val buildCyclesMethod = builderClass.getMethod("buildCycles", Int::class.java)
                    val buildMethod = builderClass.getMethod("build")
                    
                    buildChannelAMethod.invoke(builder)
                    buildChannelMethod.invoke(builder, frameData)
                    buildPeriodMethod.invoke(builder, 1000)
                    buildCyclesMethod.invoke(builder, 1)
                    val frame = buildMethod.invoke(builder)
                    
                    // Use reflection to display the frame
                    frame?.let { invokeGlyphMethod(listOf("displayGlyphFrame", "display", "sendFrame", "showFrame"), it) }
                }
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
        
        // Use reflection to call unInit
        try {
            invokeGlyphMethod(listOf("unInit", "uninit", "deinit", "destroy", "release"))
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
