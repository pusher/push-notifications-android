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
import org.junit.*

import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MultipleClassInstancesTests {
  fun getStoredDeviceId(): String? {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    return deviceStateStore.deviceId
  }

  val context = InstrumentationRegistry.getTargetContext()
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e82"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

  companion object {
    val secretKey = "a-really-long-secret-key-that-ends-with-hunter2"
    val errol = FakeErrol(8080, secretKey)

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
  fun setUserIdShoudlThrowExceptionIfCalledOnAnyInstanceBeforeStart() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, "alice")
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    PushNotifications.setTokenProvider(tokenProvider)
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // Set a user id
    pni.setUserId("alice")

    // no exception here

    val pni2 = PushNotificationsInstance(context, instanceId)

    // immediately calling `setUserId`
    try {
      pni2.setUserId("alice")

      Assert.fail("No exception was triggered")
    } catch (e: IllegalStateException) {
      // Expected.
    }
  }

  @Test
  fun testPossibleRaceConditionsWhenMultipleInstancesAreSubscribing() {
    val pni1 = PushNotificationsInstance(context, instanceId)
    val pni2 = PushNotificationsInstance(context, instanceId)

      Thread {
      for (i in 1..50) {
        pni1.subscribe("a-$i")
      }
    }.start()

    Thread {
      for (i in 1..50) {
        pni2.subscribe("b-$i")
      }
    }.start()

    Thread.sleep(1000)

    assertThat(pni1.getSubscriptions().size, `is`(equalTo(100)))
    assertThat(pni2.getSubscriptions().size, `is`(equalTo(100)))
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
