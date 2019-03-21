package com.betterme.eventsdroid.entities

open class Event(
        val categoryName: String,
        val eventName: String,
        open val params: Map<String, String>
)
