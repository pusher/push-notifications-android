package com.pusher.pushnotifications.reporting.api

import com.google.gson.Gson
import com.pusher.pushnotifications.api.DeviceHeadersInterceptor
import com.pusher.pushnotifications.api.OperationCallback
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReportingAPI(private val instanceId: String) {
  private val baseUrl = "https://$instanceId.pushnotifications.pusher.com/reporting_api/v1/"

  private val gson = Gson()

  private val client =
    OkHttpClient.Builder()
      .addInterceptor(DeviceHeadersInterceptor())
      .build()

  private val service =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .client(client)
      .build()
      .create(ReportingService::class.java)

  fun submit(reportEvent: ReportEvent, operationCallback: OperationCallback) {
    val callback = object : Callback<Void> {
      override fun onResponse(call: Call<Void>?, response: Response<Void>) {
        when {
          response.code() in 200..299 -> {
            operationCallback.onSuccess()
          }
          response.code() >= 500 ->
            onFailure(call, RuntimeException("Failed to submit reporting event"))
          else ->
            onFailure(call, UnrecoverableRuntimeException("Failed to submit reporting event"))
        }
      }

      override fun onFailure(call: Call<Void>?, t: Throwable) {
        operationCallback.onFailure(t)
      }
    }

    service.submit(
      instanceId = instanceId,
      eventType = reportEvent.eventType.toString(),
      registerRequest = ReportingMetadata(
        publishId = reportEvent.publishId,
        timestamp = reportEvent.timestampMs
      )
    ).enqueue(callback)
  }
}
