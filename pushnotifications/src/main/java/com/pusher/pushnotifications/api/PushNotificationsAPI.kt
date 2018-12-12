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
import java.lang.RuntimeException

class PushNotificationsAPIException: RuntimeException {
  constructor(message: String): super(message)
  constructor(cause: Throwable): super(cause)
}

sealed class RetryStrategy<T> {
  abstract fun retry(f: () -> T): T

  class WithInfiniteExpBackOff<T>: RetryStrategy<T>() {
    private var retryCount = 0

    override fun retry(f: () -> T): T {
      while (true) {
        try {
          val result = f()
          if (result != null) {
            return result
          }
        } catch (e: RuntimeException) {
        }

        retryCount++
        val delayMs = computeExponentialBackoff(retryCount)
        Thread.sleep(delayMs)
      }
    }

    private fun computeExponentialBackoff(retryCount: Int): Long =
        Math.min(maxRetryDelayMs, baseRetryDelayMs * Math.pow(2.0, retryCount - 1.0)).toLong()

    companion object {
      private const val baseRetryDelayMs = 200.0
      private const val maxRetryDelayMs = 64000.0
    }
  }
}

class PushNotificationsAPI(private val instanceId: String, overrideHostURL: String?) {
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
      .create(PushNotificationService::class.java)

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

  @Throws(PushNotificationsAPIException::class)
  fun registerFCM(
      token: String,
      knownPreviousClientIds: List<String>,
      retryStrategy: RetryStrategy<RegisterDeviceResult>
  ): RegisterDeviceResult {
    return retryStrategy.retry(fun(): RegisterDeviceResult{
      val requestBody = RegisterRequest(
          token,
          knownPreviousClientIds,
          DeviceMetadata(BuildConfig.VERSION_NAME, android.os.Build.VERSION.RELEASE)
      )
      val response = service.register(instanceId, requestBody).execute()

      val responseBody = response?.body()
      if (responseBody != null && response.code() in 200..299) {
        return RegisterDeviceResult(
            deviceId = responseBody.id,
            initialInterests = responseBody.initialInterestSet)
      }

      val responseErrorBody = response?.errorBody()
      if (responseErrorBody != null) {
        val error = safeExtractJsonError(responseErrorBody.string())
        log.w("Failed to register device: $error")
        throw PushNotificationsAPIException(error)
      }

      throw PushNotificationsAPIException("Unknown API error")
    })
  }
/*
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

    if (fcmToken == token) {
      return // Registration already in progress
    }

    fcmToken = token

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
          fcmToken = null
          operationCallback.onFailure(error)
        }
      }
    })
  }
*/
  fun subscribe(
    deviceId: String,
    interest: String,
    retryStrategy: RetryStrategy<Unit>
  ) {
    return retryStrategy.retry(fun() {
      val response = service.subscribe(instanceId, deviceId, interest).execute()
      if (response.code() !in 200..299) {
        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to subscribe to interest: $error")
          throw PushNotificationsAPIException(error)
        }

        throw PushNotificationsAPIException("Unknown API error")
      }
    })
  }

  fun unsubscribe(
      deviceId: String,
      interest: String,
      retryStrategy: RetryStrategy<Unit>
  ) {
    return retryStrategy.retry(fun() {
      val response = service.unsubscribe(instanceId, deviceId, interest).execute()
      if (response.code() !in 200..299) {
        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to unsubscribe from interest: $error")
          throw PushNotificationsAPIException(error)
        }

        throw PushNotificationsAPIException("Unknown API error")
      }
    })
  }

  fun setSubscriptions(deviceId: String, interests: Set<String>, retryStrategy: RetryStrategy<Unit>) {
    return retryStrategy.retry(fun() {
      val response = service.setSubscriptions(
          instanceId,
          deviceId,
          SetSubscriptionsRequest(interests)
      ).execute()
      if (response.code() !in 200..299) {
        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to set subscriptions: $error")
          throw PushNotificationsAPIException(error)
        }

        throw PushNotificationsAPIException("Unknown API error")
      }
    })
  }

  fun refreshToken(deviceId: String, fcmToken: String, retryStrategy: RetryStrategy<Unit>) {
    return retryStrategy.retry(fun() {
      val response = service.refreshToken(
          instanceId,
          deviceId,
          RefreshToken(fcmToken)
      ).execute()
      if (response.code() !in 200..299) {
        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to refresh FCM token: $error")
          throw PushNotificationsAPIException(error)
        }

        throw PushNotificationsAPIException("Unknown API error")
      }
    })
  }

  fun setMetadata(
      deviceId: String,
      metadata: DeviceMetadata,
      retryStrategy: RetryStrategy<Unit>
  ) {
    return retryStrategy.retry(fun() {
      val response = service.setMetadata(
          instanceId,
          deviceId,
          metadata
      ).execute()
      if (response.code() !in 200..299) {
        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = safeExtractJsonError(responseErrorBody.string())
          log.w("Failed to set device metadata: $error")
          throw PushNotificationsAPIException(error)
        }

        throw PushNotificationsAPIException("Unknown API error")
      }
    })
  }
}
