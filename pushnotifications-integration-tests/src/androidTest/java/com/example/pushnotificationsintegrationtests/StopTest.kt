package com.example.pushnotificationsintegrationtests


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
import java.io.File

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
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e89"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")

  companion object {
    val errol = FakeErrol(8080)
  }

  @Before
  @After
  fun cleanup() {
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())
    assertTrue(deviceStateStore.clear())
    assertNull(deviceStateStore.deviceId)
    assertThat(deviceStateStore.interests.size, `is`(equalTo(0)))

    File(context.filesDir, "$instanceId.jobqueue").delete()
  }

  @Test
  fun stopShouldDeleteADeviceInTheServer() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    pni.stop()

    Thread.sleep(1000)

    // and now the server should not have this device anymore
    assertNull(errolClient.getDevice(storedDeviceId!!))
  }

  @Test
  fun stopShouldDeleteLocalInterests() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.subscribe("potato")
    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

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
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    pni.stop()

    Thread.sleep(1000)

    // and now the server should not have this device anymore
    assertNull(errolClient.getDevice(storedDeviceId!!))

    pni.start()
    Thread.sleep(DEVICE_REGISTRATION_WAIT_MS)

    // A device ID should have been stored
    val newStoredDeviceId = getStoredDeviceId()
    assertNotNull(newStoredDeviceId)
    assertNotNull(errolClient.getDevice(newStoredDeviceId!!))
  }
}