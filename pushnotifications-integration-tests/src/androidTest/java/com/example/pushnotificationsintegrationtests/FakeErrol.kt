package com.example.pushnotificationsintegrationtests

import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*

data class FakeErrolDevice(
  val id: String,
  val interests: MutableSet<String>
) {
  companion object {
    fun New(interests: MutableSet<String>): FakeErrolDevice {
      return FakeErrolDevice(UUID.randomUUID().toString(), interests)
    }
  }
}

data class FakeErrolStorage(
  val devices: MutableMap<String, FakeErrolDevice>
)


data class SetSubscriptionsRequest(
    val interests: Set<String>
)


class FakeErrol(port: Int): NanoHTTPDRouter(port) {
  val storage = FakeErrolStorage(mutableMapOf())
  val gson = Gson()

  init {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }

  override fun setupRoutes() {
    post("/instances/{instanceId}/devices/fcm") { session, params, body ->
      val device = FakeErrolDevice.New(mutableSetOf())
      storage.devices[device.id] = device

      NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, """
        {
          "id": "${device.id}",
          "initialInterestSet": []
        }
      """.trimIndent())
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}") { session, params, body ->
      val device = storage.devices[params["deviceId"]]
      if (device != null) {
        NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, """
          {
            "id": "${device.id}",
            "metadata": {
              "sdkVersion": "",
              "androidVersion": ""
            }
          }
        """.trimIndent())
      } else {
        notFoundResponse
      }
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}/interests") { session, params, body ->
      val device = storage.devices[params["deviceId"]]
      if (device != null) {
        val responseBody = gson.toJson(GetInterestsResponse(interests = device.interests))
        NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, responseBody)
      } else {
        notFoundResponse
      }
    }

    post("/instances/{instanceId}/devices/fcm/{deviceId}/interests/{interest}") { session, params, body ->
      val device = storage.devices[params["deviceId"]]
      if (device != null) {
        device.interests.add(params["interest"]!!)
        NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, "")
      } else {
        notFoundResponse
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/interests") { session, params, body ->
      try {
        val reqBody = gson.fromJson<SetSubscriptionsRequest>(body.toString(Charset.forName("UTF-8")), SetSubscriptionsRequest::class.java)

        val device = storage.devices[params["deviceId"]]
        if (device != null) {
          device.interests.clear()
          device.interests.addAll(reqBody.interests)
          NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, "")
        } else {
          notFoundResponse
        }
      } catch (e: Throwable) {
        notFoundResponse
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/metadata") { session, params, body ->
      NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, "")
    }
  }
}
