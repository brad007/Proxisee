package com.see.proxi.proxisee.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator


fun Context.vibrateDevice() {
    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    // Vibrate for 500 milliseconds
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
        //deprecated in API 26
        v.vibrate(500)
    }
}