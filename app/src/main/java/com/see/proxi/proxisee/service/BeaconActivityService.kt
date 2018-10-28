package com.see.proxi.proxisee.service

import android.content.Intent
import androidx.core.app.JobIntentService
import com.see.proxi.proxisee.ui.proxi.ProxiActivity
import com.see.proxi.proxisee.utils.Constants
import mobi.inthepocket.android.beacons.ibeaconscanner.Beacon
import mobi.inthepocket.android.beacons.ibeaconscanner.BluetoothScanBroadcastReceiver

class BeaconActivityService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {

        // This is the beacon object containing UUID, major and minor info
        val beacon = intent.getParcelableExtra(BluetoothScanBroadcastReceiver.IBEACON_SCAN_BEACON_DETECTION) as Beacon

        // This flag will be true if it is an enter event that triggered this service
        val enteredBeacon = intent.getBooleanExtra(BluetoothScanBroadcastReceiver.IBEACON_SCAN_BEACON_ENTERED, false);

        // This flag will be true if it is an exit event that triggered this service
        val exitedBeacon = intent.getBooleanExtra(BluetoothScanBroadcastReceiver.IBEACON_SCAN_BEACON_EXITED, false);

        if (enteredBeacon) {
            val proxiIntent = Intent(this, ProxiActivity::class.java)
            proxiIntent.putExtra(Constants.EXTRA_MAJOR_NUMBER, beacon.major)
            startActivity(proxiIntent)
        }

    }
}