package com.wolfyslayer.glyphmatrixtoy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Monitor battery status including level and charging state
 */
class BatteryMonitor(private val context: Context) {
    
    data class BatteryStatus(
        val level: Int,
        val isCharging: Boolean,
        val isFull: Boolean
    )
    
    private var listener: ((BatteryStatus) -> Unit)? = null
    private var isRegistered = false
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val status = getBatteryStatus(intent)
                listener?.invoke(status)
            }
        }
    }
    
    fun start(listener: (BatteryStatus) -> Unit) {
        this.listener = listener
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            // For API 33+ (TIRAMISU), we need to specify RECEIVER_NOT_EXPORTED
            // since ACTION_BATTERY_CHANGED is a system broadcast
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
            isRegistered = true
        }
        
        // Get initial battery status
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryStatus?.let {
            listener(getBatteryStatus(it))
        }
    }
    
    fun stop() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (e: IllegalArgumentException) {
                // Receiver not registered
            }
            isRegistered = false
        }
        listener = null
    }
    
    private fun getBatteryStatus(intent: Intent): BatteryStatus {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = (level * 100 / scale.toFloat()).toInt()
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL
        
        return BatteryStatus(batteryPct, isCharging, isFull)
    }
}
