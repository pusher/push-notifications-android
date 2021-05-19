package com.example.pushnotificationsintegrationtests

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.BeamsCallback
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.PusherCallbackError
import com.pusher.pushnotifications.SubscriptionsChangedListener
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.awaitility.kotlin.untilNull
import org.awaitility.kotlin.until
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
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MultiInstanceSupportTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val instanceId1 = "00000000-1241-08e9-b379-377c32cd1e80"
  private val instanceId2 = "00000000-1241-08e9-b379-377c32cd1e82"
  private val errolClient1 = ErrolAPI(instanceId1, "http://localhost:8080")
  private val errolClient2 = ErrolAPI(instanceId2, "http://localhost:8080")


  fun getStoredDeviceId(instanceId: String): String? {
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)
    return deviceStateStore.deviceId
  }

  companion object {
    const val clusterSecretKey = "really-long-cluster-secret-key"
    private val errol = FakeErrol(8080, clusterSecretKey)

    @AfterClass
    @JvmStatic
    fun shutdownFakeErrol() {
      errol.stop()
    }
  }

  @Before
  @After
  fun wipeLocalState() {
    fun wipe(instanceId: String) {
      val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)

      await.atMost(1, TimeUnit.SECONDS) until {
        assertTrue(deviceStateStore.clear())

        deviceStateStore.interests.size == 0 && deviceStateStore.deviceId == null
      }

      File(context.filesDir, "$instanceId.jobqueue").delete()
    }
    wipe(instanceId1)
    wipe(instanceId2)
  }

  private fun assertStoredDeviceIdIsNotNull(instanceId: String) {
    try {
      await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
        getStoredDeviceId(instanceId)
      }
    } catch (e: ConditionTimeoutException) {
      // Maybe FCM is complaining in CI, so let's pretend to have a token now
      MessagingService.onRefreshToken!!("fake-fcm-token")

      await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
        getStoredDeviceId(instanceId)
      }
    }
  }

  @Test
  fun startingShouldGiveDifferentDeviceIds() {
    // Start both instances
    val pni1 = PushNotificationsInstance(context, instanceId1)
    pni1.start()

    val pni2 = PushNotificationsInstance(context, instanceId2)
    pni2.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull(instanceId1)
    assertStoredDeviceIdIsNotNull(instanceId2)

    val storedDeviceId1 = getStoredDeviceId(instanceId1)
    val storedDeviceId2 = getStoredDeviceId(instanceId2)

    // and they are different
    assertNotEquals(storedDeviceId1, storedDeviceId2)

    // and the stored ids should match the server one
    assertNotNull(errolClient1.getDevice(storedDeviceId1!!))
    assertNotNull(errolClient2.getDevice(storedDeviceId2!!))
  }

  @Test
  fun stoppingShouldNotAffectTheOther() {
    // Start both instances
    val pni1 = PushNotificationsInstance(context, instanceId1)
    pni1.start()

    val pni2 = PushNotificationsInstance(context, instanceId2)
    pni2.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull(instanceId1)
    assertStoredDeviceIdIsNotNull(instanceId2)

    val storedDeviceId1 = getStoredDeviceId(instanceId1)
    val storedDeviceId2 = getStoredDeviceId(instanceId2)

    // and the stored ids should match the server one
    assertNotNull(errolClient1.getDevice(storedDeviceId1!!))
    assertNotNull(errolClient2.getDevice(storedDeviceId2!!))

    pni1.stop()

    // check the server no longer has device 1
    await.atMost(3, TimeUnit.SECONDS) untilNull {
      errolClient1.getDevice(storedDeviceId1)
    }
    // but device 2 is still there
    assertNotNull(errolClient2.getDevice(storedDeviceId2))
  }

  @Test
  fun interestSubscriptionsShouldNotAffectTheOther() {
    // Start both instances
    val pni1 = PushNotificationsInstance(context, instanceId1)
    pni1.addDeviceInterest("zebra")
    pni1.addDeviceInterest("panda")
    pni1.start()

    val pni2 = PushNotificationsInstance(context, instanceId2)
    pni2.addDeviceInterest("donut")
    pni2.addDeviceInterest("pizza")
    pni2.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull(instanceId1)
    assertStoredDeviceIdIsNotNull(instanceId2)

    val storedDeviceId1 = getStoredDeviceId(instanceId1)
    val storedDeviceId2 = getStoredDeviceId(instanceId2)

    // and the stored ids should match the server one
    assertNotNull(errolClient1.getDevice(storedDeviceId1!!))
    assertNotNull(errolClient2.getDevice(storedDeviceId2!!))

    // check the server has the same interests
    await.atMost(3, TimeUnit.SECONDS) until {
      errolClient1.getDeviceInterests(storedDeviceId1) == setOf("zebra", "panda")
    }
    await.atMost(3, TimeUnit.SECONDS) until {
      errolClient2.getDeviceInterests(storedDeviceId2) == setOf("donut", "pizza")
    }

    assertThat(pni1.getDeviceInterests(), `is`(setOf("zebra", "panda")))
    assertThat(pni2.getDeviceInterests(), `is`(setOf("donut", "pizza")))

    pni1.addDeviceInterest("koala")
    pni1.removeDeviceInterest("zebra")
    pni2.addDeviceInterest("bagel")
    pni2.removeDeviceInterest("donut")

    await.atMost(3, TimeUnit.SECONDS) until {
      errolClient1.getDeviceInterests(storedDeviceId1) == setOf("panda", "koala")
    }
    await.atMost(3, TimeUnit.SECONDS) until {
      errolClient2.getDeviceInterests(storedDeviceId2) == setOf("pizza", "bagel")
    }

    assertThat(pni1.getDeviceInterests(), `is`(setOf("panda", "koala")))
    assertThat(pni2.getDeviceInterests(), `is`(setOf("pizza", "bagel")))
  }

  @Test
  fun setUserIdShouldNotAffectTheOther() {
    // Create token provider
    val aliceJWT = makeJWT(instanceId1, clusterSecretKey, "alice")
    val bobJWT = makeJWT(instanceId2, clusterSecretKey, "bob")
    val tokenProvider1 = StubTokenProvider(aliceJWT)
    val tokenProvider2 = StubTokenProvider(bobJWT)

    // Start both instances
    val pni1 = PushNotificationsInstance(context, instanceId1)
    pni1.start()

    var successWasCalled1 = false
    var failureWasCalled1 = false
    pni1.setUserId("alice", tokenProvider1, object: BeamsCallback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        successWasCalled1 = true
      }

      override fun onFailure(error: PusherCallbackError) {
        failureWasCalled1 = true
      }
    })

    val pni2 = PushNotificationsInstance(context, instanceId2)
    pni2.start()

    var successWasCalled2 = false
    var failureWasCalled2 = false
    pni2.setUserId("bob", tokenProvider2, object: BeamsCallback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        successWasCalled2 = true
      }

      override fun onFailure(error: PusherCallbackError) {
        failureWasCalled2 = true
      }
    })

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull(instanceId1)
    assertStoredDeviceIdIsNotNull(instanceId2)

    val storedDeviceId1 = getStoredDeviceId(instanceId1)
    val storedDeviceId2 = getStoredDeviceId(instanceId2)

    // and the stored ids should match the server one
    assertNotNull(errolClient1.getDevice(storedDeviceId1!!))
    assertNotNull(errolClient2.getDevice(storedDeviceId2!!))

    // check the server has the same interests
    await.atMost(3, TimeUnit.SECONDS) until {
      errolClient1.getDevice(storedDeviceId1)?.userId == "alice"
    }
    await.atMost(3, TimeUnit.SECONDS) until {
      errolClient2.getDevice(storedDeviceId2)?.userId == "bob"
    }

    // make sure the callbacks were correctly triggered
    assertTrue(successWasCalled1)
    assertFalse(failureWasCalled1)
    assertTrue(successWasCalled2)
    assertFalse(failureWasCalled2)
  }

  @Test
  fun setOnDeviceInterestsChangedListenerDoesNotAffectTheOther() {
    // Start both instances
    val pni1 = PushNotificationsInstance(context, instanceId1)
    pni1.addDeviceInterest("hello")
    pni1.start()

    val pni2 = PushNotificationsInstance(context, instanceId2)
    pni2.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull(instanceId1)
    assertStoredDeviceIdIsNotNull(instanceId2)

    val storedDeviceId1 = getStoredDeviceId(instanceId1)
    val storedDeviceId2 = getStoredDeviceId(instanceId2)

    // and the stored ids should match the server one
    assertNotNull(errolClient1.getDevice(storedDeviceId1!!))
    assertNotNull(errolClient2.getDevice(storedDeviceId2!!))

    var setOnSubscriptionsChangedListenerCalledCount1 = 0
    var lastSetOnSubscriptionsChangedListenerCalledWithInterests1: Set<String>? = null
    pni1.setOnDeviceInterestsChangedListener(object : SubscriptionsChangedListener {
      override fun onSubscriptionsChanged(interests: Set<String>) {
        setOnSubscriptionsChangedListenerCalledCount1++
        lastSetOnSubscriptionsChangedListenerCalledWithInterests1 = interests
      }
    })
    var setOnSubscriptionsChangedListenerCalledCount2 = 0
    var lastSetOnSubscriptionsChangedListenerCalledWithInterests2: Set<String>? = null
    pni2.setOnDeviceInterestsChangedListener(object : SubscriptionsChangedListener {
      override fun onSubscriptionsChanged(interests: Set<String>) {
        setOnSubscriptionsChangedListenerCalledCount2++
        lastSetOnSubscriptionsChangedListenerCalledWithInterests2 = interests
      }
    })

    pni1.setDeviceInterests(setOf("potato"))
    assertThat(setOnSubscriptionsChangedListenerCalledCount1, `is`(equalTo(1)))
    assertThat(setOnSubscriptionsChangedListenerCalledCount2, `is`(equalTo(0)))

    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests1, `is`(equalTo(setOf("potato"))))
    assertNull(lastSetOnSubscriptionsChangedListenerCalledWithInterests2)

    pni2.setDeviceInterests(setOf("pasta"))
    assertThat(setOnSubscriptionsChangedListenerCalledCount1, `is`(equalTo(1)))
    assertThat(setOnSubscriptionsChangedListenerCalledCount2, `is`(equalTo(1)))

    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests1, `is`(equalTo(setOf("potato"))))
    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests2, `is`(equalTo(setOf("pasta"))))
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
