package com.see.proxi.proxisee.data.source

import com.see.proxi.proxisee.data.model.PointOfInterest
import com.see.proxi.proxisee.data.remote.Api
import durdinapps.rxfirebase2.RxFirebaseChildEvent
import io.reactivex.Flowable

class DataSourceFactory(
        val api: Api
) {
    fun getPoi(majorNumber: String): Flowable<RxFirebaseChildEvent<PointOfInterest>> {
        return api.getPOI(majorNumber)
    }
}