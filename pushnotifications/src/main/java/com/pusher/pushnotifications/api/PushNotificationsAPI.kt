package com.pusher.pushnotifications.api

import com.google.gson.Gson
import com.pusher.pushnotifications.api.retrofit2.RequestCallbackWithExpBackoff
import com.pusher.pushnotifications.logging.Logger
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class PushNotificationsAPI(private val instanceId: String) {
  private val baseUrl = "https://errol-staging.herokuapp.com/device_api/v1/"
//    private val baseUrl = "https://$instanceId.pushnotifications.staging.pusher.com/device_api/v1/"
//    private val baseUrl = "http://10.0.2.2:8111/device_api/v1/"

  private val log = Logger.get(this::class)

  private val gson = Gson()
  private val service =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build()
      .create(PushNotificationService::class.java)

  private val jobQueue: ArrayList<(String) -> Unit> = ArrayList()

  var deviceId: String? = null
  var fcmToken: String? = null

  fun registerOrRefreshFCM(token: String, operationCallback: OperationCallback) {
    deviceId?.let { dId ->
      if (fcmToken != null && fcmToken != token) {
        service.refreshToken(instanceId, dId, RefreshToken(token))
          .enqueue(object : RequestCallbackWithExpBackoff<Void>() {
            override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
              if (response?.code() == 404) {
                deviceId = null
                registerOrRefreshFCM(token, operationCallback)
                return
              }
              response?.errorBody()?.let { responseErrorBody ->
                val error = gson.fromJson(responseErrorBody.string(), NOKResponse::class.java)
                log.w("Failed to register device: $error")
                operationCallback.onFailure(error)
                return
              }
              fcmToken = token
            }
          })
      }
      return
    }

    val call = service.register(instanceId, RegisterRequest(token))
    call.enqueue(object : RequestCallbackWithExpBackoff<RegisterResponse>() {
      override fun onResponse(call: Call<RegisterResponse>?, response: Response<RegisterResponse>?) {
        val responseBody = response?.body()
        if (responseBody != null) {
          deviceId = responseBody.id
          log.i("Successfully registered device (id: $deviceId)")

          operationCallback.onSuccess()

          synchronized(jobQueue) {
            jobQueue.forEach {
              it(responseBody.id)
            }
            jobQueue.clear()
          }

          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = gson.fromJson(responseErrorBody.string(), RegisterResponseError::class.java)
          log.w("Failed to register device: $error")
          operationCallback.onFailure(error)
        }
      }
    })
  }

  fun subscribe(interest: String, operationCallback: OperationCallback) {
    val callback = object : RequestCallbackWithExpBackoff<Void>() {
      override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
        if (response != null && response.code() >= 200 && response.code() < 300) {
          log.d("Successfully subscribed to interest '$interest'")
          operationCallback.onSuccess()
          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = gson.fromJson(responseErrorBody.string(), NOKResponse::class.java)
          log.w("Failed to subscribe to interest: $error")
          operationCallback.onFailure(error)
        }
      }
    }

    synchronized(jobQueue) {
      deviceId?.let {
        service.subscribe(instanceId = instanceId, deviceId = it, interest = interest)
          .enqueue(callback)
        return
      }
      jobQueue += {
        service.subscribe(instanceId = instanceId, deviceId = it, interest = interest)
          .enqueue(callback)
      }
    }
  }

  fun unsubscribe(interest: String, operationCallback: OperationCallback) {
    val callback = object : RequestCallbackWithExpBackoff<Void>() {
      override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
        if (response != null && response.code() >= 200 && response.code() < 300) {
          log.d("Successfully unsubscribed to interest '$interest'")
          operationCallback.onSuccess()
          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = gson.fromJson(responseErrorBody.string(), NOKResponse::class.java)
          log.w("Failed to unsubscribe to interest: $error")
          operationCallback.onFailure(error)
        }
      }
    }

    synchronized(jobQueue) {
      deviceId?.let {
        service.unsubscribe(instanceId = instanceId, deviceId = it, interest = interest)
          .enqueue(callback)
        return
      }
      jobQueue += {
        service.unsubscribe(instanceId = instanceId, deviceId = it, interest = interest)
          .enqueue(callback)
      }
    }
  }

  fun setSubscriptions(interests: Set<String>, operationCallback: OperationCallback) {
    val callback = object : RequestCallbackWithExpBackoff<Void>() {
      override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
        if (response != null && response.code() >= 200 && response.code() < 300) {
          log.d("Successfully updated the interest set")
          operationCallback.onSuccess()
          return
        }

        val responseErrorBody = response?.errorBody()
        if (responseErrorBody != null) {
          val error = gson.fromJson(responseErrorBody.string(), NOKResponse::class.java)
          log.w("Failed to update the interest set: $error")
          operationCallback.onFailure(error)
        }
      }
    }

    synchronized(jobQueue) {
      deviceId?.let {
        service.setSubscriptions(
          instanceId = instanceId, deviceId = it, interests = SetSubscriptionsRequest(interests)
        ).enqueue(callback)
        return
      }
      jobQueue += {
        service.setSubscriptions(
          instanceId = instanceId, deviceId = it, interests = SetSubscriptionsRequest(interests)
        ).enqueue(callback)
      }
    }
  }
}
