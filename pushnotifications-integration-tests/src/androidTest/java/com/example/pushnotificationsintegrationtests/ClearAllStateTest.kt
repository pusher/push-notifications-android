package com.example.pushnotificationsintegrationtests

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.awaitility.core.ConditionTimeoutException
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.junit.AfterClass

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ClearAllStateTest {
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e81"
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

  fun getStoredDeviceId(): String? {
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)
    return deviceStateStore.deviceId
  }

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
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)

    await.atMost(1, TimeUnit.SECONDS) until {
      assertTrue(deviceStateStore.clear())

      deviceStateStore.interests.size == 0 && deviceStateStore.deviceId == null
    }

    File(context.filesDir, "$instanceId.jobqueue").delete()
  }

  private fun assertStoredDeviceIdIsNotNull() {
    try {
      await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
        getStoredDeviceId()
      }
    } catch (e: ConditionTimeoutException) {
      // Maybe FCM is complaining in CI, so let's pretend to have a token now
      MessagingService.onRefreshToken!!("fake-fcm-token")

      await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
        getStoredDeviceId()
      }
    }
  }

  @Test
  fun clearAllStateShouldClearAllState() {
    val userId = "alice"
    val secretKey = "a-really-long-secret-key-that-ends-with-hunter2"

    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    assertStoredDeviceIdIsNotNull()

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    pni.setUserId(userId, tokenProvider)
    Thread.sleep(1000)

    // Assert that the correct user id has been set for the device on the server
    var getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertThat(getDeviceResponse!!.userId, `is`(equalTo(userId)))

    // Subscribe to an interest
    pni.addDeviceInterest("peanuts")

    // The device should have that interest stored locally
    assertThat(pni.getDeviceInterests(), `is`(equalTo(setOf("peanuts"))))

    pni.clearAllState()
    Thread.sleep(1000)

    // A new device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val newStoredDeviceId = getStoredDeviceId()

    // The device should not have a user ID
    getDeviceResponse = errolClient.getDevice(newStoredDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertNull(getDeviceResponse!!.userId)

    // The device should have no interests
    assertThat(pni.getDeviceInterests(), `is`(emptySet()))
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
