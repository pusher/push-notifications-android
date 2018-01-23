package com.pusher.pushnotifications.reporting.api

import retrofit2.Call
import retrofit2.http.*

interface ReportingService {
  @POST("instances/{instanceId}/event-type/{eventType}")
  fun submit(
    @Path("instanceId") instanceId: String,
    @Path("eventType") eventType: String,
    @Body registerRequest: ReportingMetadata
  ): Call<Void>
}

data class ReportingMetadata(
  val publishId: String,
  val timestamp: Long
)

enum class ReportEventType {
  Delivery, Open,
}

data class ReportEvent(
  val eventType: ReportEventType,
  val publishId: String,
  val timestampMs: Long
)
