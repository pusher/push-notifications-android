package com.example.pushnotificationsintegrationtests

import android.app.ActivityManager
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.internal.DeviceStateStore
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before

const val DEVICE_REGISTRATION_WAIT_MS: Long = 7000 // We need to wait for FCM to register the device etc.

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DeviceRegistrationTest {
  fun wipeAndroidState() {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    assertTrue(deviceStateStore.clear())
    assertNull(deviceStateStore.deviceId)
  }

  fun getStoredDeviceId(): String? {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    return deviceStateStore.deviceId
  }

  val context = InstrumentationRegistry.getTargetContext()
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e89"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")
  companion object {
    val errol = FakeErrol(8080)
  }

  @Before
  @After
  fun cleanup() {
    wipeAndroidState()
  }

  @Test
  fun registerDeviceUponFreshStart() {
    // Start the SDK
    PushNotificationsInstance(context, instanceId).start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
  }

  @Test
  fun subscribeToInterestsAfterStart() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId).start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // The SDK should have no interests
    assertThat(pni.getSubscriptions(), `is`(emptySet()))

    // The server should have no interests for this device
    val interestsOnServer = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer, `is`(equalTo(pni.getSubscriptions())))

    // Subscribe to an interest
    pni.subscribe("peanuts")

    // The device should have that interest stored locally
    assertThat(pni.getSubscriptions(), `is`(equalTo(setOf("peanuts"))))

    // The server should have the interest too
    Thread.sleep(1000)
    val interestsOnServer2 = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer2, `is`(equalTo(pni.getSubscriptions())))
  }

  @Test
  fun subscribeToInterestsBeforeStartAndSyncAfterStart() {
    val pni = PushNotificationsInstance(context, instanceId)

    // The SDK should have no interests
    assertThat(pni.getSubscriptions(), `is`(emptySet()))

    // Subscribe to an interest
    pni.subscribe("peanuts")

    // The device should have that interest stored locally
    assertThat(pni.getSubscriptions(), `is`(equalTo(setOf("peanuts"))))

    // Start the SDK
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // The server should have the interest too
    Thread.sleep(1000)
    val interestsOnServer2 = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer2, `is`(equalTo(pni.getSubscriptions())))
  }
}
