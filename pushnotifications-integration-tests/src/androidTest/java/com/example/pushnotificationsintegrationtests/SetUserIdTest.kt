package com.example.pushnotificationsintegrationtests


import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.pusher.pushnotifications.Callback
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.PusherAlreadyRegisteredAnotherUserIdException
import com.pusher.pushnotifications.PusherCallbackError
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.internal.DeviceStateStore
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SetUserIdTest {
  fun getStoredDeviceId(): String? {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    return deviceStateStore.deviceId
  }

  val context = InstrumentationRegistry.getTargetContext()
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e89"
  val userId = "alice"
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
  }

  @Test
  fun setUserIdShouldAssociateThisDeviceWithUserOnServer() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
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
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, object: Callback<Void, PusherCallbackError> {
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
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
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
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
    assertThat(getDeviceResponse!!.userId, `is`(equalTo(userId)))

    // Restart the SDK
    pni.stop()
    val newJwt = makeJWT(instanceId, secretKey, "another-$userId")
    tokenProvider.jwt = newJwt
    pni.start()

    // Set a different user id
    pni.setUserId("another-$userId")
    Thread.sleep(1000)

    // A new device ID should have been stored
    val newStoredDeviceId = getStoredDeviceId()
    assertNotNull(newStoredDeviceId)
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
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, object: Callback<Void, PusherCallbackError> {
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
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, object: Callback<Void, PusherCallbackError> {
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
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)

    // Call setUserId straight away
    pni.setUserId(userId)
  }

  @Test(expected = IllegalStateException::class)
  fun setUserIdShouldThrowExceptionIfNoTokenProviderHasBeenGiven() {
    // Create sdk instance
    val pni = PushNotificationsInstance(context, instanceId)

    // Call setUserId straight away
    pni.setUserId(userId)
  }

  @Test(expected = PusherAlreadyRegisteredAnotherUserIdException::class)
  fun setUserIdShouldThrowExceptionIfUserIdReassigned() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // set the user id
    var successWasCalled = false
    var failureWasCalled = false
    pni.setUserId(userId, object: Callback<Void, PusherCallbackError> {
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
    pni.setUserId("another-user-id")
  }

  @Test(expected = PusherAlreadyRegisteredAnotherUserIdException::class)
  fun setUserIdShouldThrowExceptionIfUserIdReassignedBeforeDeviceCreated() {
    // Create token provider
    val jwt = makeJWT(instanceId, secretKey, userId)
    val tokenProvider = StubTokenProvider(jwt)

    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId, tokenProvider)
    pni.start()

    // Set a user id
    pni.setUserId(userId)

    // Try setting another user id
    pni.setUserId("another-user-id")
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

