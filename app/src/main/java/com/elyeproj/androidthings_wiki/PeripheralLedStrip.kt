package com.elyeproj.androidthings_wiki

import android.graphics.Color
import android.util.Log
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.io.IOException
import java.util.*

class PeripheralLedStrip {

    companion object {
        private const val TAG = "PeripheralLedStrip"
        private const val FULL_COLOR = 255
        private const val LED_COUNT = 7
    }

    private var ledStrip: Apa102? = null
    private var progressJob: Job? = null

    private var colorRed = FULL_COLOR
    private var colorBlue = FULL_COLOR
    private var colorGreen = FULL_COLOR

    init {
        try {
            ledStrip = RainbowHat.openLedStrip()
            ledStrip?.let {
                it.brightness = 1
                val colors = IntArray(LED_COUNT)
                it.write(colors)
                Log.d(TAG, "Initialized SPI LED strip")
            } ?: throw IllegalStateException("Error initializing LED strip")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing LED strip", e)
        }
    }

    fun showProgress() {
        progressJob = launch { startProgress() }

    }

    private suspend fun startProgress() {
        val colors = IntArray(7)
        while (true) {
            listOf((0 until LED_COUNT), (LED_COUNT - 1 downTo 0))
                    .flatMap { it }
                    .forEach { colorized(colors, it) }
        }
    }

    private suspend fun colorized(colors: IntArray, index: Int) {
        delay(100)
        Arrays.fill(colors, 0)

        colorRed = randomizeColor()
        colorGreen = randomizeColor()
        colorBlue = randomizeColor()

        colors[index] = Color.rgb(colorRed, colorGreen, colorBlue)
        ledStrip?.write(colors)
    }

    fun endProgress() {
        progressJob?.cancel()
        val colors = IntArray(LED_COUNT)
        ledStrip?.write(colors)

    }
    fun closeLedStrip() {
        progressJob?.cancel()
        try {
            ledStrip?.run {
                brightness = 0
                write(IntArray(7))
                close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error closing LED strip", e)
        } finally {
            ledStrip = null
        }
    }

    private fun randomizeColor() = (0..7).random() * 32
    private fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start

}
