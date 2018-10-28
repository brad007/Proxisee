package com.see.proxi.proxisee.ui.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import com.see.proxi.proxisee.ProxiApplication
import com.see.proxi.proxisee.ui.proxi.ProxiActivity
import com.see.proxi.proxisee.utils.Constants
import com.see.proxi.proxisee.utils.setUniqueValue
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import mobi.inthepocket.android.beacons.ibeaconscanner.utils.ConversionUtils
import java.util.*
import com.see.proxi.proxisee.R


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivities"
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var rxPermissions: RxPermissions
    private lateinit var rxBleClient: RxBleClient
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        requestPermissions()
        promptUserForBluetooth()

        viewModel.majorNumber.observe(this, androidx.lifecycle.Observer {
            val intent = Intent(this@MainActivity, ProxiActivity::class.java)
            intent.putExtra(Constants.EXTRA_MAJOR_NUMBER, it)
            startActivity(intent)
        })

        setupBluetoothBeaconScanner()
    }

    private fun setupBluetoothBeaconScanner() {
        rxBleClient = ProxiApplication.getRxBleClient(this)
        compositeDisposable.add(
                rxBleClient.scanBleDevices(
                        ScanSettings.Builder()
                                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                .build(),
                        ScanFilter.Builder()
                                .build()
                ).observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ scanResult ->
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

                                viewModel.majorNumber.setUniqueValue(major)
                            }
                        }, {


                        })
        )

    }


    private fun requestPermissions() {
        rxPermissions = RxPermissions(this)
        val permission = rxPermissions.request(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.VIBRATE
        ).subscribe { granted ->
            if (granted) {
                //has location
            } else {
                //does not
            }
        }
    }

    private fun promptUserForBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        val REQUEST_ENABLE_BT = 1
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }


}
