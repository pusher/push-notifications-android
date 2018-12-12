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

data class NewDeviceResponse(
    val id: String,
    val initialInterestSet: Set<String>
)

data class SetSubscriptionsRequest(
    val interests: Set<String>
)

class FakeErrol(port: Int): NanoHTTPDRouter(port) {
  val storage = FakeErrolStorage(mutableMapOf())

  init {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }

  override fun setupRoutes() {
    post("/instances/{instanceId}/devices/fcm") {
      val device = FakeErrolDevice.New(mutableSetOf())
      storage.devices[device.id] = device

      complete(Response.Status.OK,
          NewDeviceResponse(id = device.id, initialInterestSet = emptySet()))
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}") {
      val device = storage.devices[params["deviceId"]]
      if (device != null) {
        complete(Response.Status.OK, GetDeviceResponse(id = device.id, deviceMetadata = DeviceMetadata("", "")))
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}/interests") {
      val device = storage.devices[params["deviceId"]]
      if (device != null) {
        complete(Response.Status.OK, GetInterestsResponse(interests = device.interests))
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    post("/instances/{instanceId}/devices/fcm/{deviceId}/interests/{interest}") {
      val device = storage.devices[params["deviceId"]]
      if (device != null) {
        device.interests.add(params["interest"]!!)
        complete(Response.Status.OK)
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/interests") {
      entity(SetSubscriptionsRequest::class) { setSubscriptions ->
        val device = storage.devices[params["deviceId"]]
        if (device != null) {
          device.interests.clear()
          device.interests.addAll(setSubscriptions.interests)

          complete(Response.Status.OK)
        } else {
          complete(Response.Status.NOT_FOUND)
        }
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/metadata") {
      complete(Response.Status.OK)
    }
  }
}
