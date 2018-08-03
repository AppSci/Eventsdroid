package com.betterme.eventsdroid.entities

import com.google.gson.annotations.SerializedName

data class AnalyticsCategoryEntity(
        @SerializedName("name") val categoryName: String,
        @SerializedName("events") val events: List<EventEntity>
)

data class EventEntity(
        @SerializedName("name") val eventName: String,
        @SerializedName("parameters") val parameters: List<EventParameterEntity>
)

data class EventParameterEntity(
        @SerializedName("name") val param: String,
        @SerializedName("value") val value: String
)
