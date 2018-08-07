package com.pusher.pushnotifications.api

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pusher.pushnotifications.BuildConfig
import com.pusher.pushnotifications.api.retrofit2.RequestCallbackWithExpBackoff
import com.pusher.pushnotifications.logging.Logger
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PushNotificationsAPI(private val instanceId: String) {
  private val baseUrl = "https://$instanceId.pushnotifications.pusher.com/device_api/v1/"

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
      .create(PushNotificationService::class.java)

  var deviceId: String? = null
  var fcmToken: String? = null

  // Handles the JsonSyntaxException properly
  private fun safeExtractJsonError(possiblyJson: String): NOKResponse {
    return try {
      gson.fromJson(possiblyJson, NOKResponse::class.java)
    } catch (jsonException: JsonSyntaxException) {
      log.w("Failed to parse json `$possiblyJson`", jsonException)
      unknownNOKResponse
    }
  }

  data class RegisterDeviceResult(
      val deviceId: String,
      val initialInterests: Set<String>
    )

  // TODO: Separate register and refresh into separate functions
  fun registerOrRefreshFCM(
      token: String,
      knownPreviousClientIds: List<String>,
      operationCallback: OperationCallback<RegisterDeviceResult>
  ) {
    deviceId?.let { dId ->
      if (fcmToken != null && fcmToken != token) {
        fcmToken = token // optimistic, prevents multiple calls for the same fcmToken

        service.refreshToken(instanceId, dId, RefreshToken(token))
          .enqueue(object : RequestCallbackWithExpBackoff<Void>() {
            override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
              if (response?.code() == 404) {
                deviceId = null
                fcmToken = null

                registerOrRefreshFCM(token, knownPreviousClientIds, operationCallback)
                return
              }

              response?.errorBody()?.let { responseErrorBody ->
                fcmToken = null

                val error = safeExtractJsonError(responseErrorBody.string())
                log.w("Failed to register device: $error")
                operationCallback.onFailure(error)
                return
              }

              operationCallback.onSuccess(
                RegisterDeviceResult(
                    deviceId = dId,
                    initialInterests = emptySet()))
            }
          })
      }
      return
    }

    val call = service.register(
        instanceId,
        RegisterRequest(
            token,
            knownPreviousClientIds,
            DeviceMetadata(BuildConfig.VERSION_NAME, android.os.Build.VERSION.RELEASE)
        )
    )
    call.enqueue(object : RequestCallbackWithExpBackoff<RegisterResponse>() {
      override fun onResponse(call: Call<RegisterResponse>?, response: Response<RegisterResponse>?) {
        val responseBody = response?.body()
        if (responseBody != null) {
          deviceId = responseBody.id

          operationCallback.onSuccess(
              RegisterDeviceResult(
                  deviceId = responseBody.id,
                  initialInterests = responseBody.initialInterestSet))

          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error =
              try {
                gson.fromJson(responseErrorBody.string(), RegisterResponseError::class.java)
              } catch (jsonException: JsonSyntaxException) {
                log.w("Failed to parse json `${responseErrorBody.string()}`", jsonException)
                unknownNOKResponse
              }

          log.w("Failed to register device: $error")
          operationCallback.onFailure(error)
        }
      }
    })
  }

  fun subscribe(deviceId: String, interest: String, operationCallback: OperationCallbackNoArgs) {
    service.subscribe(instanceId, deviceId, interest)
        .enqueue(object : RequestCallbackWithExpBackoff<Void>() {
          override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
            if (response != null && response.code() >= 200 && response.code() < 300) {
              log.d("Successfully subscribed to interest '$interest'")
              operationCallback.onSuccess()
              return
            }

            val responseErrorBody = response?.errorBody()
            if (responseErrorBody != null) {
              val error = safeExtractJsonError(responseErrorBody.string())
              log.w("Failed to subscribe to interest: $error")
              operationCallback.onFailure(error)
            }
          }
        })
  }

  fun unsubscribe(deviceId: String, interest: String, operationCallback: OperationCallbackNoArgs) {
    service.unsubscribe(instanceId, deviceId, interest)
        .enqueue(object : RequestCallbackWithExpBackoff<Void>() {
          override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
            if (response != null && response.code() >= 200 && response.code() < 300) {
              log.d("Successfully unsubscribed to interest '$interest'")
              operationCallback.onSuccess()
              return
            }

            val responseErrorBody = response?.errorBody()
            if (responseErrorBody != null) {
              val error = safeExtractJsonError(responseErrorBody.string())
              log.w("Failed to unsubscribe to interest: $error")
              operationCallback.onFailure(error)
            }
          }
        })
  }

  fun setSubscriptions(deviceId: String, interests: Set<String>, operationCallback: OperationCallbackNoArgs) {
    service.setSubscriptions(
        instanceId, deviceId, SetSubscriptionsRequest(interests)
    ).enqueue(object : RequestCallbackWithExpBackoff<Void>() {
      override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
        if (response != null && response.code() >= 200 && response.code() < 300) {
          log.d("Successfully updated the interest set")
          operationCallback.onSuccess()
          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to update the interest set: $error")
          operationCallback.onFailure(error)
        }
      }
    })
  }

  fun setMetadata(deviceId: String, metadata: DeviceMetadata, operationCallback: OperationCallbackNoArgs) {
    service.setMetadata(
        instanceId, deviceId, metadata
    ).enqueue(object : RequestCallbackWithExpBackoff<Void>() {
      override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
        if (response != null && response.code() >= 200 && response.code() < 300) {
          log.d("Successfully set metadata")
          operationCallback.onSuccess()
          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to set metadata: $error")
          operationCallback.onFailure(error)
        }
      }
    })
  }
}
