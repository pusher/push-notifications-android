package com.example.pushnotificationsintegrationtests

import fi.iki.elonen.NanoHTTPD
import io.jsonwebtoken.Jwts
import java.util.*

data class FakeErrolDevice(
  val id: String,
  val token: String,
  val interests: MutableSet<String>,
  val userId: String? = null
) {
  companion object {
    fun New(token: String, interests: MutableSet<String>): FakeErrolDevice {
      return FakeErrolDevice(
          id = UUID.randomUUID().toString(),
          token = token,
          interests = interests
      )
    }
  }
}

data class FakeErrolStorage(
  val devices: MutableMap<String, FakeErrolDevice> = mutableMapOf()
)

data class RegisterDeviceRequest(
    val token: String
)

data class NewDeviceResponse(
    val id: String,
    val initialInterestSet: Set<String>
)

data class SetSubscriptionsRequest(
    val interests: Set<String>
)

class FakeErrol(port: Int, private val clusterKey: String = ""): NanoHTTPDRouter(port) {
  private val storage = mutableMapOf<String, FakeErrolStorage>()

  fun getInstanceStorage(instanceId: String): FakeErrolStorage {
    return storage.getOrPut(instanceId, { FakeErrolStorage() })
  }

  init {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }

  override fun setupRoutes() {
    post("/instances/{instanceId}/devices/fcm") {
      entity(RegisterDeviceRequest::class) { registerDeviceRequest ->
        val device = FakeErrolDevice.New(registerDeviceRequest.token, mutableSetOf())
        getInstanceStorage(params["instanceId"]!!).devices[device.id] = device

        complete(Response.Status.OK,
            NewDeviceResponse(id = device.id, initialInterestSet = emptySet()))
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/token") {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device != null) {
        entity(RegisterDeviceRequest::class) { registerDeviceRequest ->
          val device = FakeErrolDevice.New(registerDeviceRequest.token, mutableSetOf())
          getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]!!] = device.copy(token = registerDeviceRequest.token)

          complete(Response.Status.OK)
        }
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    get("/instances/{instanceId}/devices/fcm/{deviceId}") {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device != null) {
        complete(Response.Status.OK, GetDeviceResponse(id = device.id, userId = device.userId, deviceMetadata = DeviceMetadata("", "")))
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    delete("/instances/{instanceId}/devices/fcm/{deviceId}") {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device != null) {
        getInstanceStorage(params["instanceId"]!!).devices -= params["deviceId"]!!
        complete(Response.Status.OK)
      } else {
        complete(Response.Status.OK)
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/user", fun NanoHTTPDRouter.Request.(): NanoHTTPD.Response {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device == null) {
        return complete(Response.Status.NOT_FOUND)
      }

      val authHeader = session.headers["authorization"]
      if (authHeader == null) {
        return complete(Response.Status.BAD_REQUEST)
      }

      val jwt = authHeader.removePrefix("Bearer ")
      return try {
        val claims = Jwts.parser()
            .setSigningKey(Base64.getEncoder().encode(clusterKey.toByteArray()))
            .parseClaimsJws(jwt)
        getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]!!] = device.copy(userId = claims.body.subject)
        complete(Response.Status.OK)
      } catch (e: Exception) {
        complete(Response.Status.BAD_REQUEST)
      }
    })

    get("/instances/{instanceId}/devices/fcm/{deviceId}/interests") {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device != null) {
        complete(Response.Status.OK, GetInterestsResponse(interests = device.interests))
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    post("/instances/{instanceId}/devices/fcm/{deviceId}/interests/{interest}") {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device != null) {
        device.interests.add(params["interest"]!!)
        complete(Response.Status.OK)
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    delete("/instances/{instanceId}/devices/fcm/{deviceId}/interests/{interest}") {
      val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
      if (device != null) {
        device.interests.remove(params["interest"]!!)
        complete(Response.Status.OK)
      } else {
        complete(Response.Status.NOT_FOUND)
      }
    }

    put("/instances/{instanceId}/devices/fcm/{deviceId}/interests") {
      entity(SetSubscriptionsRequest::class) { setSubscriptions ->
        val device = getInstanceStorage(params["instanceId"]!!).devices[params["deviceId"]]
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
