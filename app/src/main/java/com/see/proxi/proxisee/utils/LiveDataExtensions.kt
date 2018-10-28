package com.see.proxi.proxisee.utils

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.setUniqueValue(t: T?) {
    if (t != null && this.value != t) {
        postValue(t)
    }
}