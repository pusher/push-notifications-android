package com.example.pushnotificationsintegrationtests

import com.google.gson.Gson
import com.pusher.pushnotifications.api.PusherLibraryHeaderInterceptor
import com.pusher.pushnotifications.logging.Logger
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.RuntimeException

class ErrolAPI(private val instanceId: String, private val overrideHostURL: String?) {
  private val baseUrl =
          overrideHostURL ?: "https://$instanceId.pushnotifications.pusher.com/device_api/v1/"

  private val log = Logger.get(this::class)

  private val gson = Gson()
  private val client =
          OkHttpClient.Builder()
                  .addInterceptor(PusherLibraryHeaderInterceptor())
                  .build()

  private val service =
          Retrofit.Builder()
                  .baseUrl(baseUrl)
                  .addConverterFactory(GsonConverterFactory.create(gson))
                  .client(client)
                  .build()
                  .create(ErrolService::class.java)

  fun getDevice(deviceId: String): GetDeviceResponse? {
    val response =
      service.getDevice(
          instanceId,
          deviceId
      ).execute()

    if (response != null && response.code() >= 200 && response.code() < 300) {
      return response.body()
    }

    if (response?.code() == 404) {
      return null
    }

    val responseErrorBody = response?.errorBody()
    if (responseErrorBody != null) {
      log.w("Failed request body: $responseErrorBody")
    }

    throw RuntimeException("Failed request to get device. " + response.raw())
  }

  fun deleteDevice(deviceId: String) {
    val response =
      service.deleteDevice(
          instanceId,
          deviceId
      ).execute()

    if (response != null && response.code() >= 200 && response.code() < 300) {
      return
    }

    val responseErrorBody = response?.errorBody()
    if (responseErrorBody != null) {
      log.w("Failed request body: $responseErrorBody")
    }

    throw RuntimeException("Failed request to delete device. " + response.raw())
  }

  fun getDeviceInterests(deviceId: String): Set<String> {
    val response =
        service.getDeviceInterests(
            instanceId,
            deviceId
        ).execute()

    if (response != null && response.code() >= 200 && response.code() < 300) {
      val responseBody = response.body()
      if (responseBody == null) {
        throw RuntimeException("No body in server response")
      } else {
        return responseBody.interests
      }
    }

    val responseErrorBody = response?.errorBody()
    if (responseErrorBody != null) {
      log.w("Failed request body: $responseErrorBody")
    }

    throw RuntimeException("Failed request to get device interests. " + response.raw())
  }
}