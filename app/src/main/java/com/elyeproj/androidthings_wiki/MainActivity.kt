package com.elyeproj.androidthings_wiki

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.things.contrib.driver.apa102.Apa102
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.io.IOException
import java.util.*

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var disposable: Disposable? = null
    private var segmentDisplay: AlphanumericDisplay? = null
    private var ledStrip: Apa102? = null
    private var progressJob: Job? = null

    private var colorRed = 255
    private var colorBlue = 255
    private var colorGreen = 255

    private val wikiApiServe by lazy {
        WikiApiService.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_search.setOnClickListener {
            if (edit_search.text.toString().isNotEmpty()) {
                beginSearch(edit_search.text.toString())
            }
        }

        initSegmentDisplay()
        initLedStrip()
    }

    private fun initSegmentDisplay() {
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

    private fun initLedStrip() {
        try {
            ledStrip = RainbowHat.openLedStrip()
            ledStrip?.let {
                it.brightness = 1
                val colors = IntArray(7)
                it.write(colors)
                Log.d(TAG, "Initialized SPI LED strip")
            } ?: throw IllegalStateException("Error initializing LED strip")
        } catch (e: IOException) {
            throw RuntimeException("Error initializing LED strip", e)
        }
    }

    private fun beginSearch(searchString: String) {

        progressJob = launch { startProgress() }

        disposable = wikiApiServe.hitCountCheck("query", "json", "search", searchString)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { result ->
                            updateResult(result)
                            endProgress()
                        },
                        { error ->
                            Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                            endProgress()
                        }
                )
    }

    private fun endProgress() {
        progressJob?.cancel()
        val colors = IntArray(7)
        ledStrip?.write(colors)

    }

    private suspend fun startProgress() {
        val colors = IntArray(7)
        while (true) {
            (6 downTo 0).forEach { colorized(colors, it) }
            (0..6).forEach { colorized(colors, it) }
        }
    }

    private suspend fun colorized(colors: IntArray, index: Int) {
        delay(100)
        Arrays.fill(colors, 0)

        colorRed = (0..7).random() * 32
        colorGreen = (0..7).random() * 32
        colorBlue = (0..7).random() * 32

        colors[index] = Color.rgb(colorRed, colorGreen, colorBlue)
        ledStrip?.write(colors)
    }

    private fun updateResult(result: Model.Result) {
        val value = result.query.searchinfo.totalhits
        txt_search_result.text = "$value result found"

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

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeSegmentDisplay()
        closeLedStrip()

        progressJob?.cancel()
    }

    private fun closeSegmentDisplay() {
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

    private fun closeLedStrip() {
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


    fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) +  start

}
