package com.see.proxi.proxisee

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.see.proxi.proxisee.ShakeDetector.OnShakeListener



class ShakeDetector: SensorEventListener{

    interface OnShakeListener {
        fun onShake(count: Int)
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7F
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SHAKE_COUNT_RESET_TIME_MS = 3000
    }

    fun setOnShakeListener(listener: OnShakeListener) {
        this.listener = listener
    }

    private var listener: OnShakeListener? = null
    private var shakeTimestamp: Long = 0L
    private var shakeCount: Int = 0


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //ignore
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (listener != null){
            val x = event!!.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            //gForce will be close to 1 when there is no movement
            val gForce = Math.sqrt((gX*gX+gY*gY + gZ+gZ).toDouble()).toFloat()
            if(gForce > SHAKE_THRESHOLD_GRAVITY){
                val now = System.currentTimeMillis()
                if(shakeTimestamp + SHAKE_SLOP_TIME_MS > now){
                    return
                }

                if(shakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now){
                    shakeCount = 0
                }

                shakeTimestamp = now
                shakeCount++
                listener?.onShake(shakeCount)
            }


        }
    }

}