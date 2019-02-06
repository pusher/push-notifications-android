package com.pusher.pushnotifications.api;

import retrofit2.Call
import retrofit2.http.*
import java.io.Serializable

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

  @PUT("instances/{instanceId}/devices/fcm/{deviceId}/metadata")
  fun setMetadata(
    @Path("instanceId") instanceId: String,
    @Path("deviceId") deviceId: String,
    @Body metadata: DeviceMetadata
  ): Call<Void>

  @PUT("instances/{instanceId}/devices/fcm/{deviceId}/user")
  fun setUserId(
      @Path("instanceId") instanceId: String,
      @Path("deviceId") deviceId: String,
      @Header("Authorization") authorizationHeader: String
  ): Call<Void>

  @DELETE("instances/{instanceId}/devices/fcm/{deviceId}")
  fun delete(
    @Path("instanceId") instanceId: String,
    @Path("deviceId") deviceId: String
  ): Call<Void>
}

data class NOKResponse(
  val error: String?,
  val description: String?
): RuntimeException() {
  override val message: String?
    get() = this.toString()
}

val unknownNOKResponse = NOKResponse("Unknown Service Error", "Something went wrong")

data class RefreshToken(
  val token: String
)

data class RegisterRequest(
  val token: String,
  val knownPreviousClientIds: List<String>,
  val metadata: DeviceMetadata
)

data class DeviceMetadata (
  val sdkVersion: String,
  val androidVersion: String
): Serializable

data class RegisterResponse(
  val id: String,
  val initialInterestSet: Set<String>
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
