package com.pusher.pushnotifications.reporting.api

import retrofit2.Call
import retrofit2.http.*

interface ReportingService {
  @POST("instances/{instanceId}/events")
  fun submit(
    @Path("instanceId") instanceId: String,
    @Body reportingRequest: ReportingRequest
  ): Call<Void>
}

data class ReportingRequest(
  val eventType: String,
  val publishId: String,
  val deviceId: String,
  val timestamp: Long
)

enum class ReportEventType {
  Delivery, Open,
}

data class ReportEvent(
  val eventType: ReportEventType,
  val deviceId: String,
  val publishId: String,
  val timestampMs: Long
)
