package com.example.adchaosdemo

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.doOnLayout
import kotlin.math.roundToInt

class MiniGame3Activity : AppCompatActivity(), SensorEventListener {

    private lateinit var mazeView: VolumeMazeView
    private lateinit var volumeBar: SeekBar
    private lateinit var statusText: TextView
    private lateinit var volumeIndicatorText: TextView
    private lateinit var resetButton: Button

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var goalHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minigame3)

        mazeView = findViewById(R.id.mazeView)
        volumeBar = findViewById(R.id.volumeBar)
        statusText = findViewById(R.id.miniGameStatusText)
        volumeIndicatorText = findViewById(R.id.volumeIndicatorText)
        resetButton = findViewById(R.id.resetButton)

        volumeBar.max = 100
        volumeBar.isEnabled = false
        volumeBar.progress = 0
        volumeBar.setOnTouchListener { _, _ -> true }
        statusText.text = getString(R.string.minigame3_status_ingame)
        updateVolumeIndicator(0)

        mazeView.onBallXChanged = { x ->
            val progress = (x * 100f).roundToInt().coerceIn(0, 100)
            volumeBar.progress = progress
            updateVolumeIndicator(progress)
        }
        mazeView.onGoalReached = {
            if (!goalHandled) {
                goalHandled = true
                statusText.text = getString(R.string.minigame3_status_clear)
                Toast.makeText(this, R.string.minigame3_toast_clear, Toast.LENGTH_SHORT).show()
            }
        }
        volumeBar.doOnLayout {
            updateVolumeIndicator(volumeBar.progress)
        }
        resetButton.setOnClickListener {
            goalHandled = false
            statusText.text = getString(R.string.minigame3_status_ingame)
            volumeBar.progress = 0
            updateVolumeIndicator(0)
            mazeView.resetGame()
        }

        sensorManager = getSystemService()
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        mazeView.start()
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        mazeView.stop()
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        mazeView.setTilt(event.values[0], event.values[1])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateVolumeIndicator(progress: Int) {
        volumeIndicatorText.text = progress.toString()
        volumeBar.post {
            val trackWidthPx = volumeBar.width - volumeBar.paddingStart - volumeBar.paddingEnd
            val fraction = (progress / 100f).coerceIn(0f, 1f)
            val thumbCenterX =
                volumeBar.x + volumeBar.paddingStart + (trackWidthPx * fraction) - volumeBar.thumbOffset
            val indicatorX = thumbCenterX - (volumeIndicatorText.width / 2f)
            val indicatorY = volumeBar.y - volumeIndicatorText.height - dpToPx(10f)

            volumeIndicatorText.x = indicatorX
            volumeIndicatorText.y = indicatorY
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
