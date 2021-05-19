package com.example.pushnotificationsintegrationtests

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.*
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
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.lang.IllegalStateException
import java.lang.RuntimeException
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
class SetUserIdTest {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e84"
  val userId = "alice"
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
  fun setUserIdShouldAssociateThisDeviceWithUserOnServer() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    pni.setUserId(userId, tokenProvider)
    Thread.sleep(1000)

    // Assert that the correct user id has been set for the device on the server
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertThat(getDeviceResponse!!.userId, `is`(equalTo(userId)))
  }

  @Test
  fun setUserIdShouldCallSuccessCallbackIfNothingWentWrong() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, tokenProvider, object: BeamsCallback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        successWasCalled = true
      }

      override fun onFailure(error: PusherCallbackError) {
        failureWasCalled = true
      }
    })

    Thread.sleep(1000)

    assertTrue(successWasCalled)
    assertFalse(failureWasCalled)
  }

  @Test
  fun startSetUserIdStopAndSetUserIdWithADifferentUserShouldSucceed() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    pni.setUserId(userId, tokenProvider)
    Thread.sleep(2000)

    // Assert that the correct user id has been set for the device on the server
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertThat(getDeviceResponse!!.userId, `is`(equalTo(userId)))

    // Restart the SDK
    pni.stop()
    val newJwt = makeJWT(instanceId, secretKey, "another-$userId")
    tokenProvider.jwt = newJwt
    pni.start()

    // Set a different user id
    pni.setUserId("another-$userId", tokenProvider)
    Thread.sleep(1000)

    // A new device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val newStoredDeviceId = getStoredDeviceId()
    assertThat(newStoredDeviceId, `is`(not(equalTo(storedDeviceId))))

    // Assert that the correct user id has been set for the device on the server
    val newGetDeviceResponse = errolClient.getDevice(newStoredDeviceId!!)
    assertNotNull(newGetDeviceResponse)
    assertThat(newGetDeviceResponse!!.userId, `is`(equalTo("another-$userId")))
  }

  @Test
  fun setUserIdShouldCallFailureCallbackIfBeamsServerRejectsJWT() {
    val badSecretKey = "i-am-an-evil-hacker-hunter2"
    // Create token provider
    val jwt = makeJWT(instanceId, badSecretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, tokenProvider, object: BeamsCallback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        successWasCalled = true
      }

      override fun onFailure(error: PusherCallbackError) {
        failureWasCalled = true
      }
    })

    Thread.sleep(1000)

    assertFalse(successWasCalled)
    assertTrue(failureWasCalled)
  }

  @Test
  fun setUserIdShouldCallFailureCallbackIfTokenProviderFails() {
    // Create token provider
    val tokenProvider = BrokenTokenProvider()

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, tokenProvider, object: BeamsCallback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        successWasCalled = true
      }

      override fun onFailure(error: PusherCallbackError) {
        assertThat(error.cause.toString(), containsString("Cheese is love, cheese is life."))
        failureWasCalled = true
      }
    })

    Thread.sleep(1000)

    assertFalse(successWasCalled)
    assertTrue(failureWasCalled)
  }

  @Test(expected = IllegalStateException::class)
  fun setUserIdShouldThrowExceptionIfStartHasNotBeenCalled() {
    // Create token provider
    val tokenProvider = StubTokenProvider("")

    // Create sdk instance
    val pni = PushNotificationsInstance(context, instanceId)

    // Call setUserId straight away
    pni.setUserId(userId, tokenProvider)
  }

  @Test(expected = IllegalStateException::class)
  fun setUserIdShouldThrowExceptionIfANullTokenProviderWasGiven() {
    // Create sdk instance
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // Call setUserId straight away

    pni.setUserId(userId, JavaNull.getNullTokenProvider())
  }

  @Test(expected = PusherAlreadyRegisteredAnotherUserIdException::class)
  fun setUserIdShouldThrowExceptionIfUserIdReassigned() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, tokenProvider, object: BeamsCallback<Void, PusherCallbackError> {
      override fun onSuccess(vararg values: Void) {
        successWasCalled = true
      }

      override fun onFailure(error: PusherCallbackError) {
        assertThat(error.cause.toString(), containsString("Cheese is love, cheese is life."))
        failureWasCalled = true
      }
    })

    Thread.sleep(1000)

    assertTrue(successWasCalled)
    assertFalse(failureWasCalled)

    // Try setting another user id
    pni.setUserId("another-user-id", tokenProvider)
  }

  @Test(expected = PusherAlreadyRegisteredAnotherUserIdException::class)
  fun setUserIdShouldThrowExceptionIfUserIdReassignedBeforeDeviceCreated() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // Set a user id
    pni.setUserId(userId, tokenProvider)

    // Try setting another user id
    pni.setUserId("another-user-id", tokenProvider)
  }

  private class BrokenTokenProvider: TokenProvider {
    override fun fetchToken(userId: String): String {
      throw RuntimeException("Cheese is love, cheese is life.")
    }
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

