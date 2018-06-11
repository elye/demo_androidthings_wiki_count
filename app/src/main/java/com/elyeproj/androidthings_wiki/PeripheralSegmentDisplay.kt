package com.elyeproj.androidthings_wiki

import android.util.Log
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import java.io.IOException

class PeripheralSegmentDisplay {
    private var segmentDisplay: AlphanumericDisplay? = null

    companion object {
        private const val TAG = "PeripheralSegmentDisplay"
    }

    init {
        try {
            segmentDisplay = RainbowHat.openDisplay()
            segmentDisplay?.let {
                it.setEnabled(true)
                it.display("GOOD")
            } ?: throw IllegalStateException("Error initializing display")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing display", e)
        }
    }

    fun updateResult(result: Model.Result) {
        val value = result.query.searchinfo.totalhits
        val valueLength = value.toString().length

        when {
            valueLength <= 4 -> segmentDisplay?.display(value)
            valueLength <= 6 -> unitize(value, 1000.0, "k")
            valueLength <= 9 -> unitize(value, 1000000.0, "M")
            else -> segmentDisplay?.display("HUGE")
        }
    }

    private fun unitize(value: Int, demonimator: Double, unit: String) {
        val shrinkValue = Math.round(value / demonimator).toInt()
        segmentDisplay?.display("${shrinkValue}${unit}")
    }

    fun closeSegmentDisplay() {
        try {
            segmentDisplay?.run {
                clear()
                setEnabled(false)
                close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error closing display", e)
        } finally {
            segmentDisplay = null
        }
    }
}
