package com.example.pushnotificationsintegrationtests

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.internal.DeviceStateStore

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DeviceRegistrationTest {
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e89"

  fun wipeAndroidState() {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())

    assertTrue(deviceStateStore.clear())
    assertNull(deviceStateStore.deviceId)
  }

  fun getStoredDeviceId(): String? {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())

    return deviceStateStore.deviceId
  }

  @Test
  fun registerDeviceUponFreshStart() {
    val appContext = InstrumentationRegistry.getTargetContext()

    FakeErrol(8080)

    wipeAndroidState()

    PushNotifications.start(appContext, instanceId)

    Thread.sleep(5000)

    val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)
  }
}
