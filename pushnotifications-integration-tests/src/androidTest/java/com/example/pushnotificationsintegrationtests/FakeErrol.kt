package com.example.pushnotificationsintegrationtests

import fi.iki.elonen.NanoHTTPD

class FakeErrol(port: Int): NanoHTTPDRouter(port) {
  init {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }

  override fun setupRoutes() {
    post("/instances/{instanceId}/devices/fcm") { session, params ->
      NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, """
        {
          "id": "d-123",
          "initialInterestSet": []
        }
      """.trimIndent())
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}") { session, params ->
      if (params["deviceId"] == "d-123") {
        NanoHTTPD.newFixedLengthResponse(Response.Status.OK, mimeTypeJSON, """
          {
            "id": "d-123",
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
