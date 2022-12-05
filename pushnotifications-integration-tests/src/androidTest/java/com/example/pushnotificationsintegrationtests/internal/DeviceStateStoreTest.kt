package com.example.pushnotificationsintegrationtests.internal

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.internal.DeviceStateStore
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DeviceStateStoreTest {
  val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
  val instanceId1 = "00000000-1241-08e9-b379-377c32cd1e80"
  val instanceId2 = "00000000-1241-08e9-b379-377c32cd1e81"

  val deviceStateStore = DeviceStateStore(context)

  @Before
  @After
  fun wipeLocalState() {
    val deviceStateStore = DeviceStateStore(context)
    await.atMost(1, TimeUnit.SECONDS) until {
      assertTrue(deviceStateStore.clear())

      deviceStateStore.instanceIds.size == 0
    }
  }

  @Test
  fun instanceIdsShouldRetrieveAndStoreInstancesCorrectly() {
    assertThat(deviceStateStore.instanceIds, `is`(emptySet<String>()))

    deviceStateStore.instanceIds = mutableSetOf(instanceId1)

    assertThat(deviceStateStore.instanceIds, `is`(equalTo(mutableSetOf(instanceId1))))

    // and we can add more instances to this set
    deviceStateStore.instanceIds = deviceStateStore.instanceIds.apply { add(instanceId2) }

    assertThat(deviceStateStore.instanceIds, `is`(equalTo(mutableSetOf(instanceId1, instanceId2))))
  }

  @Test
  fun instanceIdsShouldNotHaveDuplicates() {
    assertThat(deviceStateStore.instanceIds, `is`(emptySet<String>()))

    deviceStateStore.instanceIds = deviceStateStore.instanceIds.apply { add(instanceId1) }
    assertThat(deviceStateStore.instanceIds, `is`(equalTo(mutableSetOf(instanceId1))))

    deviceStateStore.instanceIds = deviceStateStore.instanceIds.apply { add(instanceId1) }
    assertThat(deviceStateStore.instanceIds, `is`(equalTo(mutableSetOf(instanceId1))))
  }

  @Test
  fun previousInstanceGetMigratedToNewFormat() {
    assertThat(deviceStateStore.instanceIds, `is`(emptySet<String>()))

    val instanceDeviceStateStore = InstanceDeviceStateStore(context, "i-123")
    assertTrue(instanceDeviceStateStore.clear())
    Thread.sleep(1000)

    val prefs = context.getSharedPreferences("com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE)
    prefs.edit().putString("instanceId", "i-123").commit()

    val oldInstance = InstanceDeviceStateStore(context, null)
    oldInstance.deviceId = "old"
    oldInstance.userId = "old"
    oldInstance.FCMToken = "old"
    oldInstance.osVersion = "old"
    oldInstance.sdkVersion = "old"
    oldInstance.interests = mutableSetOf("old")
    oldInstance.serverConfirmedInterestsHash = "old"
    oldInstance.startJobHasBeenEnqueued = true
    oldInstance.setUserIdHasBeenCalledWith = "old"

    val newDSS = DeviceStateStore(context)
    assertThat(newDSS.instanceIds, `is`(mutableSetOf("i-123")))

    val newIDSS = InstanceDeviceStateStore(context, "i-123")
    assertTrue(newIDSS.deviceId == "old")
    assertTrue(newIDSS.userId == "old")
    assertTrue(newIDSS.FCMToken == "old")
    assertTrue(newIDSS.osVersion == "old")
    assertTrue(newIDSS.sdkVersion == "old")
    assertTrue(newIDSS.interests == mutableSetOf("old"))
    assertTrue(newIDSS.serverConfirmedInterestsHash == "old")
    assertTrue(newIDSS.startJobHasBeenEnqueued)
    assertTrue(newIDSS.setUserIdHasBeenCalledWith == "old")
  }
}