/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.gripactions.sensor

import android.content.Context
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.util.RotationUtils

internal class GripSensor(context: Context) : SensorEventListener {
    fun interface Listener {
        fun onGripChanged(state: State)
    }

    enum class State(val value: Int) {
        BIG_ACTION(-2),
        STILL_DESK(-1),
        FREE(0),
        BOTTOM_LEFT(1),
        BOTTOM_RIGHT(2),
        BOTH_HANDS(4),
        TOP_RIGHT(5),
        TOP_LEFT(6),
        UNKNOWN(Int.MIN_VALUE);

        fun isSingleHanded(): Boolean = when (this) {
            BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT -> true
            else -> false
        }

        fun resolveSide(rotation: Int): Side? {
            val point = when (this) {
                BOTTOM_LEFT -> Point(0, 1)
                BOTTOM_RIGHT -> Point(1, 1)
                TOP_LEFT -> Point(0, 0)
                TOP_RIGHT -> Point(1, 0)
                else -> return null
            }
            RotationUtils.rotatePoint(point, rotation, 1, 1)
            return if (point.x == 0) Side.LEFT else Side.RIGHT
        }
    }

    enum class Side { LEFT, RIGHT }

    companion object {
        private const val TAG = "GripSensor"
        private const val DEBUG = true
        private const val STRING_TYPE = "xiaomi.sensor.gpd"

        fun isSupported(context: Context): Boolean = findSensor(
            context.getSystemService(SensorManager::class.java)
        ) != null

        private fun findSensor(sensors: SensorManager): Sensor? =
            sensors.getSensorList(Sensor.TYPE_ALL).firstOrNull {
                it.stringType == STRING_TYPE
            }
    }

    private val sensors = context.getSystemService(SensorManager::class.java)
    private val listeners = mutableSetOf<Listener>()
    private var registered = false
    private var lastValue: Float? = null

    fun addListener(listener: Listener) {
        listeners.add(listener)
        if (registered) return

        val sensor = findSensor(sensors)
        if (sensor == null) {
            Log.w(TAG, "Grip posture sensor not found")
            return
        }

        try {
            registered = sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            if (DEBUG) Log.d(TAG, "Grip listener registered=$registered")
            if (!registered) Log.w(TAG, "Cannot register grip listener")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Cannot register grip listener", e)
        }
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
        if (listeners.isNotEmpty()) return

        if (registered) sensors.unregisterListener(this)

        registered = false
        lastValue = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!registered) return

        val value = event.values.firstOrNull() ?: return
        if (value == lastValue) return
        lastValue = value

        val state = State.entries.firstOrNull { it.value.toFloat() == value } ?: State.UNKNOWN
        if (DEBUG) Log.d(TAG, "Grip state=$state values=${event.values.contentToString()}")

        listeners.toList().forEach { it.onGripChanged(state) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
