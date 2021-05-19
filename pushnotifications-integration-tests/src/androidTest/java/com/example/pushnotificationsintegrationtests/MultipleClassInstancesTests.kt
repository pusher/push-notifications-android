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
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.hamcrest.CoreMatchers.*
import org.junit.*

import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File
import java.lang.IllegalStateException
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
class MultipleClassInstancesTests {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e82"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

  fun getStoredDeviceId(): String? {
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)
    return deviceStateStore.deviceId
  }

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
  fun setUserIdShouldThrowExceptionIfCalledOnAnyInstanceBeforeStart() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, "alice")
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // Set a user id
    pni.setUserId("alice", tokenProvider)

    // no exception here

    // test hack: we are deliberately waiting for a device id here before finishing the test
    // so it doesn't interfere with others
    assertStoredDeviceIdIsNotNull()

    val pni2 = PushNotificationsInstance(context, instanceId)

    // immediately calling `setUserId`
    try {
      pni2.setUserId("alice", tokenProvider)

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
        pni1.addDeviceInterest("a-$i")
      }
    }.start()

    Thread {
      for (i in 1..50) {
        pni2.addDeviceInterest("b-$i")
      }
    }.start()

    Thread.sleep(1000)

    assertThat(pni1.getDeviceInterests().size, `is`(equalTo(100)))
    assertThat(pni2.getDeviceInterests().size, `is`(equalTo(100)))
  }

  @Test
  fun multipleInstantiationsOfPushNotificationsInstanceAreSupported() {
    val pni1 = PushNotificationsInstance(context, instanceId)
    val pni2 = PushNotificationsInstance(context, instanceId)
    pni1.start()

    assertStoredDeviceIdIsNotNull()

    assertThat(pni1.getDeviceInterests(), `is`(emptySet()))
    assertThat(pni2.getDeviceInterests(), `is`(emptySet()))

    (0..5).forEach { n ->
      pni1.addDeviceInterest("hell-$n")
      pni2.removeDeviceInterest("hell-$n")
    }

    assertThat(pni1.getDeviceInterests(), `is`(emptySet()))
    assertThat(pni2.getDeviceInterests(), `is`(emptySet()))

    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    Thread.sleep(1000)
    val interestsOnServer = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer, `is`(emptySet()))
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
