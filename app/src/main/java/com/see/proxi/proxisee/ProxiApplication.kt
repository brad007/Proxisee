package com.see.proxi.proxisee

import android.app.Application
import android.content.Context
import android.content.Intent
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import com.see.proxi.proxisee.ui.proxi.ProxiActivity
import com.see.proxi.proxisee.utils.Constants
import io.reactivex.android.schedulers.AndroidSchedulers
import mobi.inthepocket.android.beacons.ibeaconscanner.utils.ConversionUtils
import java.util.*

class ProxiApplication : Application() {

    private var majorNumber: Int = -1

    companion object {
        private var rxBleClient: RxBleClient? = null
        fun getRxBleClient(context: Context): RxBleClient {
            if (rxBleClient == null) {
                rxBleClient = RxBleClient.create(context)
            }
            return rxBleClient!!
        }
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
        setupBluetoothBeaconScanner()
    }

    private fun setupBluetoothBeaconScanner() {
        rxBleClient = getRxBleClient(applicationContext)
        rxBleClient?.scanBleDevices(
                ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build(),
                ScanFilter.Builder()
                        .build()
        )?.observeOn(AndroidSchedulers.mainThread())?.subscribe({ scanResult ->
            val scanRecord = scanResult.scanRecord.bytes

            var startByte = 2
            var patternFound = false

            while (startByte <= 5) {
                if (scanRecord[startByte + 2].toInt() and 0xff == 0x02 && // identifies an iBeacon
                        scanRecord[startByte + 3].toInt() and 0xff == 0x15) {
                    // identifies correct data length
                    patternFound = true
                    break
                }
                startByte++
            }

            if (patternFound) {
                val uuidBytes = ByteArray(16)
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16)

                val major = ConversionUtils.byteArrayToInteger(Arrays.copyOfRange(scanRecord, startByte + 20, startByte + 22))
                if (major != majorNumber && majorNumber != -1) {
                    majorNumber = major
                    val intent = Intent(applicationContext, ProxiActivity::class.java)
                    intent.putExtra(Constants.EXTRA_MAJOR_NUMBER, major)
                    startActivity(intent)
                }
            }
        }, {


        })


    }

}