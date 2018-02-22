package com.pusher.pushnotifications.api;

import retrofit2.Call
import retrofit2.http.*

interface PushNotificationService {
  @POST("instances/{instanceId}/devices/fcm")
  fun register(
    @Path("instanceId") instanceId: String,
    @Body registerRequest: RegisterRequest
  ): Call<RegisterResponse>

  @PUT("instances/{instanceId}/devices/fcm/{deviceId}/token")
  fun refreshToken(
    @Path("instanceId") instanceId: String,
    @Path("deviceId") deviceId: String,
    @Body refreshToken: RefreshToken
  ): Call<Void>

  @POST("instances/{instanceId}/devices/fcm/{deviceId}/interests/{interest}")
  fun subscribe(
    @Path("instanceId") instanceId: String,
    @Path("deviceId") deviceId: String,
    @Path("interest") interest: String
  ): Call<Void>

  @DELETE("instances/{instanceId}/devices/fcm/{deviceId}/interests/{interest}")
  fun unsubscribe(
    @Path("instanceId") instanceId: String,
    @Path("deviceId") deviceId: String,
    @Path("interest") interest: String
  ): Call<Void>

  @PUT("instances/{instanceId}/devices/fcm/{deviceId}/interests")
  fun setSubscriptions(
    @Path("instanceId") instanceId: String,
    @Path("deviceId") deviceId: String,
    @Body interests: SetSubscriptionsRequest
  ): Call<Void>
}

data class NOKResponse(
  val error: String,
  val desc: String
): RuntimeException() {
  override val message: String?
    get() = this.toString()
}

data class RefreshToken(
  val token: String
)

data class RegisterRequest(
  val token: String,
  val metadata: DeviceMetadata
)

data class DeviceMetadata (
  val sdkVersion: String,
  val androidVersion: String
)

data class RegisterResponse(
  val id: String
)

data class RegisterResponseError(
  val error: String,
  val desc: String,
  val tokenValidationResponse: TokenValidationResponse
): RuntimeException() {
  override val message: String?
    get() = this.toString()
}

data class TokenValidationResponse(
  val error: String,
  val details: String,
  val clientId: String,
  val messageId: String,
  val sentDeviceToken: String,
  val success: Boolean,
  val platform: String,
  val receivedDeviceToken: String
)

data class SetSubscriptionsRequest(
  val interests: Set<String>
)
