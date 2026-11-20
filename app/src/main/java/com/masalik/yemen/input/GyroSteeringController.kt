package com.masalik.yemen.input

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class GyroSteeringController(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    var steerNorm: Float = 0f
        private set

    private var calibrationOffset: Float = 0f
    private var smoothedTilt: Float = 0f
    private var registered = false

    fun start() {
        if (registered || accelerometer == null) return
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        registered = true
    }

    fun stop() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
        steerNorm = 0f
    }

    fun calibrateCenter() {
        calibrationOffset = smoothedTilt
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.values.size < 2) return
        // In landscape orientation, Y-axis captures left/right steering wheel tilt
        val rawTilt = event.values[1]
        smoothedTilt += (rawTilt - smoothedTilt) * 0.22f
        val delta = smoothedTilt - calibrationOffset
        val deadzone = 0.45f
        val effective = if (abs(delta) < deadzone) 0f else (delta - kotlin.math.sign(delta) * deadzone)
        steerNorm = (effective / 4.2f).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
