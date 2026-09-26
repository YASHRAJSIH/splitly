package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * FILE PURPOSE: Detects a phone shake using the accelerometer, so shaking
 * the phone opens Add Expense from anywhere in the app.
 *
 * No permission needed — TYPE_ACCELEROMETER is a normal sensor, not a
 * dangerous/runtime-permission one.
 *
 * Threshold/cooldown are tuned so ordinary handling (pulling the phone out
 * of a pocket, walking) doesn't fire this, but a deliberate shake does.
 * These are starting values — if it fires too easily or not easily enough
 * on your phone, raise/lower SHAKE_THRESHOLD.
 */
class ShakeDetector(
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var smoothedDelta = 0f
    private var lastShakeTimeMs = 0L

    companion object {
        private const val SHAKE_THRESHOLD = 12f
        // One shake gesture crosses the threshold on several consecutive
        // sensor samples, not just once — this stops that single gesture
        // from being counted (and navigated) multiple times.
        private const val SHAKE_COOLDOWN_MS = 1500L
    }

    fun register(context: Context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun unregister(context: Context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        if (e.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = e.values[0]
        val y = e.values[1]
        val z = e.values[2]

        val acceleration = sqrt(x * x + y * y + z * z)
        val delta = acceleration - lastAcceleration
        lastAcceleration = acceleration

        // Light smoothing so one noisy sample doesn't fire it on its own.
        smoothedDelta = smoothedDelta * 0.9f + delta

        if (smoothedDelta > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimeMs > SHAKE_COOLDOWN_MS) {
                lastShakeTimeMs = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}