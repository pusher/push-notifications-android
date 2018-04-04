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

sealed class ReportEvent(
  val event: ReportEventType,
  val deviceId: String,
  val publishId: String,
  val timestampSecs: Long,
  val appInBackground: Boolean? = null,
  val hasDisplayableContent: Boolean? = null,
  val hasData: Boolean? = null
)

class OpenEvent (
  deviceId: String,
  publishId: String,
  timestampSecs: Long
): ReportEvent(ReportEventType.Open, deviceId, publishId, timestampSecs)

class DeliveryEvent (
  deviceId: String,
  publishId: String,
  timestampSecs: Long,
  appInBackground: Boolean,
  hasDisplayableContent: Boolean,
  hasData: Boolean
): ReportEvent(ReportEventType.Delivery, deviceId, publishId, timestampSecs, appInBackground, hasDisplayableContent, hasData)

