package com.example.pushnotificationsintegrationtests.internal

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assert.*
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
class InstanceDeviceStateStoreTest {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val instanceId1 = "00000000-1241-08e9-b379-377c32cd1e80"
  val instanceId2 = "00000000-1241-08e9-b379-377c32cd1e81"

  val idss1 = InstanceDeviceStateStore(context, instanceId1)
  val idss2 = InstanceDeviceStateStore(context, instanceId2)

  @Before
  @After
  fun wipeLocalState() {
      assertTrue(idss1.clear())
      assertTrue(idss2.clear())
    await.atMost(1, TimeUnit.SECONDS) until {

      idss1.deviceId == null &&
          idss1.userId == null &&
          idss1.FCMToken == null &&
          idss1.osVersion == null &&
          idss1.sdkVersion == null &&
          idss1.interests.size == 0 &&
          idss1.serverConfirmedInterestsHash == null &&
          !idss1.startJobHasBeenEnqueued &&
          idss1.setUserIdHasBeenCalledWith == null &&
      idss2.deviceId == null &&
          idss2.userId == null &&
          idss2.FCMToken == null &&
          idss2.osVersion == null &&
          idss2.sdkVersion == null &&
          idss2.interests.size == 0 &&
          idss2.serverConfirmedInterestsHash == null &&
          !idss2.startJobHasBeenEnqueued &&
          idss2.setUserIdHasBeenCalledWith == null
    }
  }

  @Test
  fun deviceIdShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.deviceId)
    idss1.deviceId = "d-123"
    assertThat(idss1.deviceId, `is`(equalTo("d-123")))
  }

  @Test
  fun userIdShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.userId)
    idss1.userId = "alice"
    assertThat(idss1.userId, `is`(equalTo("alice")))
  }

  @Test
  fun FCMTokenShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.FCMToken)
    idss1.FCMToken = "t-123"
    assertThat(idss1.FCMToken, `is`(equalTo("t-123")))
  }

  @Test
  fun osVersionShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.osVersion)
    idss1.osVersion = "android-9"
    assertThat(idss1.osVersion, `is`(equalTo("android-9")))
  }

  @Test
  fun sdkVersionShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.sdkVersion)
    idss1.sdkVersion = "1.2.3"
    assertThat(idss1.sdkVersion, `is`(equalTo("1.2.3")))
  }

  @Test
  fun interestsShouldRetrieveAndStoreCorrectly() {
    assertThat(idss1.interests, `is`(emptySet<String>()))
    idss1.interests = mutableSetOf("donuts")
    assertThat(idss1.interests, `is`(mutableSetOf("donuts")))
  }

  @Test
  fun serverConfirmedInterestsHashShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.serverConfirmedInterestsHash)
    idss1.serverConfirmedInterestsHash = "a1b2c3d4e5"
    assertThat(idss1.serverConfirmedInterestsHash, `is`(equalTo("a1b2c3d4e5")))
  }

  @Test
  fun startJobHasBeenEnqueuedShouldRetrieveAndStoreCorrectly() {
    assertFalse(idss1.startJobHasBeenEnqueued)
    idss1.startJobHasBeenEnqueued = true
    assertTrue(idss1.startJobHasBeenEnqueued)
  }

  @Test
  fun setUserIdHasBeenCalledWithShouldRetrieveAndStoreCorrectly() {
    assertNull(idss1.setUserIdHasBeenCalledWith)
    idss1.setUserIdHasBeenCalledWith = "bob"
    assertThat(idss1.setUserIdHasBeenCalledWith, `is`(equalTo("bob")))
  }

  @Test
  fun twoInstancesDoNotInterfere() {
    idss1.deviceId = "0"
    idss1.userId = "0"
    idss1.FCMToken = "0"
    idss1.osVersion = "0"
    idss1.sdkVersion = "0"
    idss1.interests = mutableSetOf("0")
    idss1.serverConfirmedInterestsHash = "0"
    idss1.startJobHasBeenEnqueued = true
    idss1.setUserIdHasBeenCalledWith = "0"

    idss2.deviceId = "1"
    idss2.userId = "1"
    idss2.FCMToken = "1"
    idss2.osVersion = "1"
    idss2.sdkVersion = "1"
    idss2.interests = mutableSetOf("1")
    idss2.serverConfirmedInterestsHash = "1"
    idss2.startJobHasBeenEnqueued = false
    idss2.setUserIdHasBeenCalledWith = "1"

    assertTrue(idss1.deviceId == "0")
    assertTrue(idss1.userId == "0")
    assertTrue(idss1.FCMToken == "0")
    assertTrue(idss1.osVersion == "0")
    assertTrue(idss1.sdkVersion == "0")
    assertTrue(idss1.interests == mutableSetOf("0"))
    assertTrue(idss1.serverConfirmedInterestsHash == "0")
    assertTrue(idss1.startJobHasBeenEnqueued)
    assertTrue(idss1.setUserIdHasBeenCalledWith == "0")

    assertTrue(idss2.deviceId == "1")
    assertTrue(idss2.userId == "1")
    assertTrue(idss2.FCMToken == "1")
    assertTrue(idss2.osVersion == "1")
    assertTrue(idss2.sdkVersion == "1")
    assertTrue(idss2.interests == mutableSetOf("1"))
    assertTrue(idss2.serverConfirmedInterestsHash == "1")
    assertTrue(idss2.startJobHasBeenEnqueued == false)
    assertTrue(idss2.setUserIdHasBeenCalledWith == "1")
  }
}