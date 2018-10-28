package com.see.proxi.proxisee.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.see.proxi.proxisee.data.model.PointOfInterest
import com.see.proxi.proxisee.utils.FirebaseConstants
import durdinapps.rxfirebase2.RxFirebaseChildEvent
import durdinapps.rxfirebase2.RxFirebaseDatabase
import io.reactivex.Flowable

class Api {
    fun getPOI(majorNumber: String): Flowable<RxFirebaseChildEvent<PointOfInterest>> {
        val query = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.POI_REF)
                .child(majorNumber)

        return RxFirebaseDatabase.observeChildEvent(query, PointOfInterest::class.java)
    }
}