package com.see.proxi.proxisee.ui.proxi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.github.nisrulz.sensey.RotationAngleDetector
import com.github.nisrulz.sensey.Sensey
import com.github.nisrulz.sensey.ShakeDetector
import com.github.nisrulz.sensey.TouchTypeDetector
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mapzen.speakerbox.Speakerbox
import com.see.proxi.proxisee.data.model.PointOfInterest
import com.see.proxi.proxisee.utils.Constants
import com.see.proxi.proxisee.utils.firstWordCaps
import com.see.proxi.proxisee.utils.setUniqueValue
import com.see.proxi.proxisee.utils.vibrateDevice
import kotlinx.android.synthetic.main.content_proxi.*
import com.see.proxi.proxisee.R
import com.tbruyelle.rxpermissions2.RxPermissions
import java.util.concurrent.TimeUnit


class ProxiActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    override fun onLocationChanged(location: Location?) {
        viewModel.location.setUniqueValue(location)
    }

    private val FROM_RADS_TO_DEGS = -57

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor == rotationSensor) {
            if (event?.values?.size!! > 4) {
                val truncatedRoationVector = FloatArray(4)
                System.arraycopy(event.values, 0, truncatedRoationVector, 0, 4)
                update(truncatedRoationVector)
            } else {
                update(event.values)
            }
        }
    }

    private fun update(vectors: FloatArray) {
        try {
            if (viewModel.pointOfInterest.value != null) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors)
                val worldAxisX = SensorManager.AXIS_MINUS_X
                val worldAxisZ = SensorManager.AXIS_MINUS_Z
                val adjustRotationMatrix = FloatArray(9)
                SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustRotationMatrix)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(adjustRotationMatrix, orientation)
                var azimuth = orientation[0] * FROM_RADS_TO_DEGS
                azimuth = if (azimuth < 0) azimuth * (-1) + 180 else azimuth



                for (i in 0 until viewModel.pointOfInterest.value?.specialDirectionMax!!.size) {
                    val max = viewModel.pointOfInterest.value?.specialDirectionMax!![i]
                    val min = viewModel.pointOfInterest.value?.specialDirectionMin!![i]


                    val maxLocation = Location("")
                    maxLocation.latitude = max.lat!!
                    maxLocation.longitude = max.long!!

                    val minLocation = Location("")
                    minLocation.latitude = min.lat!!
                    minLocation.longitude = min.long!!

                    val maxBearing = viewModel.location.value?.bearingTo(maxLocation)!!
                    val minBearing = viewModel.location.value?.bearingTo(minLocation)!!


                    if (azimuth in minBearing..maxBearing) {
                        if (!directionSpoken[i]) {
                            viewModel.displayText.setUniqueValue(viewModel.pointOfInterest.value?.specialDirectionText!![i])
                            playSound(i)


                            directionSpoken[i] = true
                        }
                    } else {
                        viewModel.displayText.setUniqueValue("Searching For Directions")

                        directionSpoken[i] = false
                    }
                }
            }
        }catch (e: Exception){

        }

    }

    private lateinit var shakeListener: ShakeDetector.ShakeListener
    private var majorNumber: Int? = null
    private var lat: Double = -33.33
    private var long: Double = 18.0


    private var tts: TextToSpeech? = null

    private lateinit var sensey: Sensey

    private val directionSpoken = booleanArrayOf(false, false, false, false)

    private var mediaPlayer = MediaPlayer()
    private lateinit var poi: PointOfInterest
    private var mainTextSpoken = false

    private lateinit var viewModel: ProxiViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxi)
        viewModel = ViewModelProviders.of(this).get(ProxiViewModel::class.java)
        majorNumber = intent.getIntExtra(Constants.EXTRA_MAJOR_NUMBER, -1)

        viewModel.majorNumber.setUniqueValue(majorNumber)

        shakeListener = object : ShakeDetector.ShakeListener {
            override fun onShakeDetected() {
                if (!mainTextSpoken)
                    vibrateDevice()
            }

            override fun onShakeStopped() {
                FirebaseDatabase.getInstance()
                        .getReference("${viewModel.majorNumber.value}")
                        .addListenerForSingleValueEvent(object : ValueEventListener, TextToSpeech.OnInitListener {
                            override fun onInit(status: Int) {

                            }

                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if (p0.exists() && !mainTextSpoken) {
                                    poi = p0.getValue(PointOfInterest::class.java)!!
                                    viewModel.pointOfInterest.setUniqueValue(poi)

                                    val speakBox = Speakerbox(this@ProxiActivity.application)
                                    speakBox.play(poi.text)
                                    setupOrientation()
                                    mainTextSpoken = true
                                } else {

                                }
                            }
                        })
            }


        }
        sensey = Sensey.getInstance()
        sensey.init(this@ProxiActivity)
        sensey.startShakeDetection(10.0f, 0, shakeListener)
        sensey.startTouchTypeDetection(this@ProxiActivity, setupDownSwipe())

        viewModel.displayText.observe(this, Observer {
            info_textview.text = it.firstWordCaps()
        })


        if (viewModel.displayText.value == null) {
            viewModel.displayText.setUniqueValue("please shake device")
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

      val rxPermissions =  RxPermissions(this) // where this is an Activity or Fragment instance

        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe {
                    if(it){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TimeUnit.SECONDS.toMillis(2),0.5f, this)
                    }
                }

    }

    private var sensorManager: SensorManager? = null

    private var rotationSensor: Sensor? = null
    private val SENSOR_DELAY = 500 * 1000

    fun setupOrientation() {
        try {
            sensorManager = getSystemService(Activity.SENSOR_SERVICE) as SensorManager
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            sensorManager?.registerListener(this, rotationSensor, SENSOR_DELAY)
        } catch (e: Exception) {

        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        sensey.setupDispatchTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun setupDownSwipe(): TouchTypeDetector.TouchTypListener {
        return object : TouchTypeDetector.TouchTypListener {
            override fun onDoubleTap() {

            }

            override fun onSwipe(p0: Int) {

                when (p0) {
                    TouchTypeDetector.SWIPE_DIR_DOWN -> {
                        val index = directionSpoken.indexOf(true)
                        if (index != -1) {
                            val speakBox = Speakerbox(this@ProxiActivity.application)
                            speakBox.play(poi.specialDirectionText!![index])
                        }
                    }
                    else -> {

                    }
                }
            }

            override fun onSingleTap() {
            }

            override fun onScroll(p0: Int) {
            }

            override fun onLongPress() {
            }

            override fun onThreeFingerSingleTap() {
            }

            override fun onTwoFingerSingleTap() {
            }
        }
    }

    fun setupRotation(): RotationAngleDetector.RotationAngleListener {

        return RotationAngleDetector.RotationAngleListener { _, _, z ->
            val tilt = if (z >= 0) z else z * (-1) + 180
            for (i in 0 until viewModel.pointOfInterest.value?.specialDirectionMax!!.size) {
                val maxLocation = viewModel.pointOfInterest.value?.specialDirectionMax!![i]
                val minLocation = viewModel.pointOfInterest.value?.specialDirectionMin!![i]

                var maxBeta = getBeta(maxLocation.long!!, maxLocation.lat!!)
                var minBeta = getBeta(minLocation.long!!, minLocation.lat!!)

                if (minBeta > maxBeta) {
                    val temp = maxBeta
                    maxBeta = minBeta
                    minBeta = temp
                }


                if (tilt in minBeta..maxBeta) {
                    if (!directionSpoken[i]) {
                        viewModel.displayText.setUniqueValue(viewModel.pointOfInterest.value?.specialDirectionText!![i])
                        playSound(i)


                        directionSpoken[i] = true
                    }
                } else {
                    viewModel.displayText.setUniqueValue("Searching For Directions")

                    directionSpoken[i] = false
                }
            }

        }
    }

    private fun getBeta(long: Double, lat: Double): Double {
        return 360 - (Math.atan((long - this.long) / (lat - this.lat)) * 180)
    }

    fun playSound(id: Int) {
        mediaPlayer = MediaPlayer()
        val fileName = when (id) {
            0 -> "beep_short.mp3"
            1 -> "metal_twang.mp3"
            2 -> "pop.mp3"
            else -> "ship_bell.mp3"
        }

        val descriptor = this.assets.openFd(fileName)
        val start = descriptor.startOffset
        val end = descriptor.length

        try {
            mediaPlayer.setDataSource(descriptor.fileDescriptor, start, end)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error setting data source", e)
        }

        mediaPlayer.prepare()
        mediaPlayer.setVolume(1f, 1f)
        mediaPlayer.start()
    }

}
