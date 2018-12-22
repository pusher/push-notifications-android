package com.example.pushnotificationsintegrationtests

import retrofit2.Call
import retrofit2.http.*

interface ErrolService {
  @GET("instances/{instanceId}/devices/fcm/{deviceId}")
  fun getDevice(
          @Path("instanceId") instanceId: String,
          @Path("deviceId") deviceId: String
  ): Call<GetDeviceResponse>

  @DELETE("instances/{instanceId}/devices/fcm/{deviceId}")
  fun deleteDevice(
          @Path("instanceId") instanceId: String,
          @Path("deviceId") deviceId: String
  ): Call<Void>

  @GET("instances/{instanceId}/devices/fcm/{deviceId}/interests")
  fun getDeviceInterests(
          @Path("instanceId") instanceId: String,
          @Path("deviceId") deviceId: String
  ): Call<GetInterestsResponse>
}

data class NOKResponse(
  val error: String,
  val desc: String
): RuntimeException() {
  override val message: String?
    get() = this.toString()
}

val unknownNOKResponse = NOKResponse("Unknown Service Error", "Something went wrong")

data class GetDeviceResponse(
  val id: String,
  val userId: String?,
  val deviceMetadata: DeviceMetadata
)

data class DeviceMetadata (
  val sdkVersion: String,
  val androidVersion: String
)

data class GetInterestsResponse (
    val interests: Set<String>
)
