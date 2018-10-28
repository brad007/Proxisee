package com.see.proxi.proxisee.ui.proxi

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.see.proxi.proxisee.data.model.PointOfInterest

class ProxiViewModel : ViewModel() {
    val pointOfInterest: MutableLiveData<PointOfInterest> = MutableLiveData()
    val majorNumber: MutableLiveData<Int> = MutableLiveData()
    val displayText: MutableLiveData<String> = MutableLiveData()
    val location: MutableLiveData<Location> = MutableLiveData()

}