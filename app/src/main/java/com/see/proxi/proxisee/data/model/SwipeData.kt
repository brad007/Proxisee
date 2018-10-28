package com.see.proxi.proxisee.data.model

import com.github.pwittchen.swipe.library.rx2.SwipeEvent

data class SwipeData(
        val swipeEvent: SwipeEvent,
        val numOfSwipes: Int
)