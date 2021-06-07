package com.example.pushnotificationsintegrationtests

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
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
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class StopTest {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e80"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

  fun getStoredDeviceId(): String? {
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)
    return deviceStateStore.deviceId
  }

  companion object {
    val errol = FakeErrol(8080)

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
  fun stopShouldDeleteADeviceInTheServer() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    pni.stop()

    await.atMost(3, TimeUnit.SECONDS) untilNull {
      // and now the server should not have this device anymore
      errolClient.getDevice(storedDeviceId!!)
    }
  }

  @Test
  fun stopShouldDeleteLocalInterests() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.addDeviceInterest("potato")
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the stored id should match the server one
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)

    pni.stop()
    assertThat(pni.getDeviceInterests(), `is`(emptySet()))
  }

  @Test
  fun afterStoppingStartShouldBePossible() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    pni.stop()

    await.atMost(3, TimeUnit.SECONDS) untilNull {
      // and now the server should not have this device anymore
      errolClient.getDevice(storedDeviceId!!)
    }

    pni.start()

    assertStoredDeviceIdIsNotNull()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val newStoredDeviceId = getStoredDeviceId()
    assertNotNull(errolClient.getDevice(newStoredDeviceId!!))
    assertThat(newStoredDeviceId, `is`(not(equalTo(storedDeviceId))))
  }
}