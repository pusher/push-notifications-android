package com.example.pushnotificationsintegrationtests


import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.internal.DeviceStateStore
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.awaitility.kotlin.untilNull
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
  fun getStoredDeviceId(): String? {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    return deviceStateStore.deviceId
  }

  val context = InstrumentationRegistry.getTargetContext()
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e80"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

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
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    assertTrue(deviceStateStore.clear())
    assertThat(deviceStateStore.interests.size, `is`(equalTo(0)))
    assertNull(deviceStateStore.deviceId)

    File(context.filesDir, "$instanceId.jobqueue").delete()

    PushNotifications.setTokenProvider(null)
  }

  @Test
  fun stopShouldDeleteADeviceInTheServer() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
      getStoredDeviceId()
    }

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

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
    pni.subscribe("potato")
    pni.start()

    await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
      getStoredDeviceId()
    }

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)

    pni.stop()
    assertThat(pni.getSubscriptions(), `is`(emptySet()))
  }

  @Test
  fun afterStoppingStartShouldBePossible() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
      getStoredDeviceId()
    }

    // A device ID should have been stored
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

    await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) untilNotNull {
      getStoredDeviceId()
    }

    // A device ID should have been stored
    val newStoredDeviceId = getStoredDeviceId()
    assertNotNull(newStoredDeviceId)
    assertNotNull(errolClient.getDevice(newStoredDeviceId!!))
    assertThat(newStoredDeviceId, `is`(not(equalTo(storedDeviceId))))
  }
}