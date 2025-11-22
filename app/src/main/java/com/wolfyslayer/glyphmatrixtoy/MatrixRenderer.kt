package com.wolfyslayer.glyphmatrixtoy

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renders time and battery information on a 16x32 pixel matrix
 */
class MatrixRenderer {
    
    companion object {
        private const val TAG = "MatrixRenderer"
        const val MATRIX_WIDTH = 32
        const val MATRIX_HEIGHT = 16
        private const val MAX_BRIGHTNESS = 4095  // Maximum brightness for Glyph SDK
        
        // 3x5 font for digits and characters
        private val FONT_3x5 = mapOf(
            '0' to listOf(
                0b111,
                0b101,
                0b101,
                0b101,
                0b111
            ),
            '1' to listOf(
                0b010,
                0b110,
                0b010,
                0b010,
                0b111
            ),
            '2' to listOf(
                0b111,
                0b001,
                0b111,
                0b100,
                0b111
            ),
            '3' to listOf(
                0b111,
                0b001,
                0b111,
                0b001,
                0b111
            ),
            '4' to listOf(
                0b101,
                0b101,
                0b111,
                0b001,
                0b001
            ),
            '5' to listOf(
                0b111,
                0b100,
                0b111,
                0b001,
                0b111
            ),
            '6' to listOf(
                0b111,
                0b100,
                0b111,
                0b101,
                0b111
            ),
            '7' to listOf(
                0b111,
                0b001,
                0b010,
                0b010,
                0b010
            ),
            '8' to listOf(
                0b111,
                0b101,
                0b111,
                0b101,
                0b111
            ),
            '9' to listOf(
                0b111,
                0b101,
                0b111,
                0b001,
                0b111
            ),
            ':' to listOf(
                0b000,
                0b010,
                0b000,
                0b010,
                0b000
            ),
            '%' to listOf(
                0b101,
                0b001,
                0b010,
                0b100,
                0b101
            )
        )
    }
    
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var chargingAnimationFrame = 0
    private var pulseAnimationFrame = 0
    
    /**
     * Render the current time and battery status to a boolean matrix
     * @return 2D array representing the matrix state (true = LED on, false = LED off)
     */
    fun render(batteryLevel: Int, isCharging: Boolean, isFull: Boolean): Array<BooleanArray> {
        val matrix = Array(MATRIX_HEIGHT) { BooleanArray(MATRIX_WIDTH) { false } }
        
        // Get current time
        val currentTime = dateFormat.format(Date())
        
        // Render time at top (line 1-6, centered)
        renderText(matrix, currentTime, 0, 1)
        
        // Render battery percentage (line 9-13, left side)
        val batteryText = "$batteryLevel%"
        renderText(matrix, batteryText, 1, 9)
        
        // Render battery icon (right side, starting at column 20)
        renderBatteryIcon(matrix, batteryLevel, isCharging, isFull, 20, 9)
        
        return matrix
    }
    
    /**
     * Render text on the matrix using the 3x5 font
     */
    private fun renderText(matrix: Array<BooleanArray>, text: String, startX: Int, startY: Int) {
        var x = startX
        for (char in text) {
            val glyph = FONT_3x5[char]
            if (glyph != null && x + 3 < MATRIX_WIDTH) {
                for (row in glyph.indices) {
                    val y = startY + row
                    if (y < MATRIX_HEIGHT) {
                        for (col in 0..2) {
                            val bit = (glyph[row] shr (2 - col)) and 1
                            if (x + col < MATRIX_WIDTH) {
                                matrix[y][x + col] = bit == 1
                            }
                        }
                    }
                }
                x += 4 // 3 pixels width + 1 pixel spacing
            }
        }
    }
    
    /**
     * Render battery icon with charging animation or full pulse
     */
    private fun renderBatteryIcon(
        matrix: Array<BooleanArray>,
        level: Int,
        isCharging: Boolean,
        isFull: Boolean,
        startX: Int,
        startY: Int
    ) {
        // Battery outline (7 pixels wide, 5 pixels tall)
        // Top cap
        if (startX + 3 < MATRIX_WIDTH && startY < MATRIX_HEIGHT) {
            matrix[startY][startX + 2] = true
            matrix[startY][startX + 3] = true
        }
        
        // Main body outline
        for (row in 1..4) {
            if (startY + row < MATRIX_HEIGHT) {
                if (startX < MATRIX_WIDTH) matrix[startY + row][startX] = true
                if (startX + 6 < MATRIX_WIDTH) matrix[startY + row][startX + 6] = true
            }
        }
        
        // Bottom
        if (startY + 5 < MATRIX_HEIGHT) {
            for (col in 0..6) {
                if (startX + col < MATRIX_WIDTH) {
                    matrix[startY + 5][startX + col] = true
                }
            }
        }
        
        // Fill based on battery level
        val fillSegments = (level / 20).coerceIn(0, 5)
        
        // If full and not charging, pulse effect
        if (isFull && !isCharging) {
            pulseAnimationFrame++
            val showFill = (pulseAnimationFrame / 10) % 2 == 0
            if (showFill) {
                for (segment in 0 until 5) {
                    fillSegment(matrix, startX, startY, segment)
                }
            }
        } else if (isCharging) {
            // Charging animation - fill from bottom to top
            chargingAnimationFrame++
            val animSegments = (chargingAnimationFrame / 5) % 6
            for (segment in 0 until animSegments) {
                fillSegment(matrix, startX, startY, segment)
            }
        } else {
            // Static fill based on level
            for (segment in 0 until fillSegments) {
                fillSegment(matrix, startX, startY, segment)
            }
        }
    }
    
    /**
     * Fill a segment of the battery icon
     */
    private fun fillSegment(matrix: Array<BooleanArray>, startX: Int, startY: Int, segment: Int) {
        val row = startY + 4 - segment // Fill from bottom to top
        if (row > startY && row < startY + 5 && row < MATRIX_HEIGHT) {
            for (col in 1..5) {
                if (startX + col < MATRIX_WIDTH) {
                    matrix[row][startX + col] = true
                }
            }
        }
    }
    
    /**
     * Convert the matrix to a flat array for Glyph SDK
     */
    fun matrixToArray(matrix: Array<BooleanArray>): IntArray {
        val array = IntArray(MATRIX_WIDTH * MATRIX_HEIGHT)
        for (y in 0 until MATRIX_HEIGHT) {
            for (x in 0 until MATRIX_WIDTH) {
                array[y * MATRIX_WIDTH + x] = if (matrix[y][x]) MAX_BRIGHTNESS else 0
            }
        }
        return array
    }
    
    fun resetAnimations() {
        chargingAnimationFrame = 0
        pulseAnimationFrame = 0
    }
}
