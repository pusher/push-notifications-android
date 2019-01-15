package com.example.pushnotificationsintegrationtests


import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.pusher.pushnotifications.Callback
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.PusherCallbackError
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.internal.DeviceStateStore
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.AfterClass

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ClearAllStateTest {
  fun getStoredDeviceId(): String? {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    return deviceStateStore.deviceId
  }

  val context = InstrumentationRegistry.getTargetContext()
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e81"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

  companion object {
    val errol = FakeErrol(8080, "a-really-long-secret-key-that-ends-with-hunter2")

    @AfterClass
    @JvmStatic
    fun shutdownFakeErrol() {
      errol.stop()
    }
  }

  @Before
  @After
  fun wipeLocalState() {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    assertTrue(deviceStateStore.clear())
    assertThat(deviceStateStore.interests.size, `is`(equalTo(0)))
    assertNull(deviceStateStore.deviceId)

    File(context.filesDir, "$instanceId.jobqueue").delete()

    PushNotifications.setTokenProvider(null)
  }

  @Test
  fun clearAllStateShouldClearAllState() {
    val userId = "alice"
    val secretKey = "a-really-long-secret-key-that-ends-with-hunter2"

    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    PushNotifications.setTokenProvider(tokenProvider)
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    pni.setUserId(userId)
    Thread.sleep(1000)

    // Assert that the correct user id has been set for the device on the server
    var getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertThat(getDeviceResponse!!.userId, `is`(equalTo(userId)))

    // Subscribe to an interest
    pni.subscribe("peanuts")

    // The device should have that interest stored locally
    assertThat(pni.getSubscriptions(), `is`(equalTo(setOf("peanuts"))))

    pni.clearAllState()
    Thread.sleep(1000)

    // A new device ID should have been stored
    val newStoredDeviceId = getStoredDeviceId()
    assertNotNull(newStoredDeviceId)
    assertThat(newStoredDeviceId, `is`(not(equalTo(storedDeviceId))))

    // The device should not have a user ID
    getDeviceResponse = errolClient.getDevice(newStoredDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertNull(getDeviceResponse!!.userId)

    // The device should have no interests
    assertThat(pni.getSubscriptions(), `is`(emptySet()))
  }

  private class StubTokenProvider(var jwt: String): TokenProvider {
    override fun fetchToken(userId: String): String {
      return jwt
    }
  }

  private fun makeJWT(instanceId: String, secretKey: String, userId: String): String {
    val iss = "https://$instanceId.pushnotifications.pusher.com"
    val exp = LocalDateTime.now().plusDays(1)

    val b64SecretKey = Base64.getEncoder().encode(secretKey.toByteArray())

    return Jwts.builder()
        .setSubject(userId)
        .setIssuer(iss)
        .setExpiration(Date.from(exp.toInstant(ZoneOffset.UTC)))
        .signWith(SignatureAlgorithm.HS256, b64SecretKey)
        .compact()
  }
}