package com.example.pushnotificationsintegrationtests

import fi.iki.elonen.NanoHTTPD
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

class FakeErrol(port: Int): NanoHTTPDRouter(port) {
  val storage = FakeErrolStorage(mutableMapOf())

  init {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }

  override fun setupRoutes() {
    post("/instances/{instanceId}/devices/fcm") { session, params ->
      val device = FakeErrolDevice.New(mutableSetOf())
      storage.devices[device.id] = device

      NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, """
        {
          "id": "${device.id}",
          "initialInterestSet": []
        }
      """.trimIndent())
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}") { session, params ->
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


    put("/instances/{instanceId}/devices/fcm/{deviceId}/interests") { session, params ->
      NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, "")
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/metadata") { session, params ->
      NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, "")
    }
  }
}
