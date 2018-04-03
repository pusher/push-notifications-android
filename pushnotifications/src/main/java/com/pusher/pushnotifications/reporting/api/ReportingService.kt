package com.pusher.pushnotifications.reporting.api

import retrofit2.Call
import retrofit2.http.*

interface ReportingService {
  @POST("instances/{instanceId}/events")
  fun submit(
    @Path("instanceId") instanceId: String,
    @Body reportingRequest: ReportEvent
  ): Call<Void>
}

enum class ReportEventType {
  Delivery, Open,
}

sealed class ReportEvent {

  data class OpenEvent (
    val event: ReportEventType,
    val deviceId: String,
    val publishId: String,
    val timestampSecs: Long
  ): ReportEvent()

  data class DeliveryEvent (
    val event: ReportEventType,
    val deviceId: String,
    val publishId: String,
    val timestampSecs: Long,
    val appInBackground: Boolean,
    val hasDisplayableContent: Boolean,
    val hasData: Boolean
  ): ReportEvent()
}
