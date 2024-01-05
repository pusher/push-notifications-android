package com.example.pushnotificationsintegrationtests

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.installations.FirebaseInstallations
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.SubscriptionsChangedListener
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.awaitility.kotlin.until
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.junit.*

import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.File
import java.util.concurrent.TimeUnit

const val DEVICE_REGISTRATION_WAIT_SECS: Long = 15 // We need to wait for FCM to register the device etc.

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DeviceRegistrationTest {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e83"
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
  fun registerDeviceUponFreshStart() {
    // Start the SDK
    PushNotificationsInstance(context, instanceId).start()

    assertStoredDeviceIdIsNotNull()

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

    assertStoredDeviceIdIsNotNull()

    val storedDeviceId = getStoredDeviceId()

    // The SDK should have no interests
    assertThat(pni.getDeviceInterests(), `is`(emptySet()))

    // The server should have no interests for this device
    val interestsOnServer = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer, `is`(equalTo(pni.getDeviceInterests())))

    // Subscribe to an interest
    pni.addDeviceInterest("peanuts")

    // The device should have that interest stored locally
    assertThat(pni.getDeviceInterests(), `is`(equalTo(setOf("peanuts"))))

    // The server should have the interest too
    Thread.sleep(1000)
    val interestsOnServer2 = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer2, `is`(equalTo(pni.getDeviceInterests())))
  }

  @Test
  fun subscribeToInterestsBeforeStartAndSyncAfterStart() {
    val pni = PushNotificationsInstance(context, instanceId)

    // The SDK should have no interests
    assertThat(pni.getDeviceInterests(), `is`(emptySet()))

    // Subscribe to an interest
    pni.addDeviceInterest("peanuts")

    // The device should have that interest stored locally
    assertThat(pni.getDeviceInterests(), `is`(equalTo(setOf("peanuts"))))

    // Start the SDK
    pni.start()

    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // The server should have the interest too
    Thread.sleep(1000)
    val interestsOnServer2 = errolClient.getDeviceInterests(storedDeviceId!!)
    assertThat(interestsOnServer2, `is`(equalTo(pni.getDeviceInterests())))
  }

  //@SkipIfProductionErrol
  @Test
  fun refreshTokenAfterStart() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId).start()

    assertStoredDeviceIdIsNotNull()

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)

    val oldToken = errol.getInstanceStorage(instanceId).devices[storedDeviceId]?.token
    assertThat(oldToken, `is`(not(equalTo(""))))

    FirebaseInstallations.getInstance().delete()

    await.atMost(DEVICE_REGISTRATION_WAIT_SECS, TimeUnit.SECONDS) until {
      // The server should have the new token now
      val newToken = errol.getInstanceStorage(instanceId).devices[storedDeviceId]?.token
      newToken == oldToken && newToken != ""
    }
  }

  @Test
  fun startDoSomeOperationsWhileHandlingUnexpectedDeviceDeletionCorrectly() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.addDeviceInterest("hello")
    pni.start()

    assertStoredDeviceIdIsNotNull()

    // A device ID should have been stored
    val storedDeviceId = getStoredDeviceId()
    assertNotNull(storedDeviceId)

    // and the stored id should match the server one
    val getDeviceResponse = errolClient.getDevice(storedDeviceId!!)
    assertNotNull(getDeviceResponse)

    errolClient.deleteDevice(storedDeviceId)

    pni.addDeviceInterest("potato")

    Thread.sleep(1000)

    assertStoredDeviceIdIsNotNull()
    val newStoredDeviceId = getStoredDeviceId()
    assertThat(newStoredDeviceId, `is`(not(equalTo(storedDeviceId))))

    assertThat(pni.getDeviceInterests(), `is`(equalTo(setOf("hello", "potato"))))
    val interestsOnServer = errolClient.getDeviceInterests(newStoredDeviceId!!)
    assertThat(interestsOnServer, `is`(equalTo(setOf("hello", "potato"))))
  }

  @Test
  fun onSubscriptionsChangedListenerShouldBeCalledIfInterestsChange() {
    val pni = PushNotificationsInstance(context, instanceId)
    var setOnSubscriptionsChangedListenerCalledCount = 0
    var lastSetOnSubscriptionsChangedListenerCalledWithInterests: Set<String>? = null
    pni.setOnDeviceInterestsChangedListener(object : SubscriptionsChangedListener {
      override fun onSubscriptionsChanged(interests: Set<String>) {
        setOnSubscriptionsChangedListenerCalledCount++
        lastSetOnSubscriptionsChangedListenerCalledWithInterests = interests
      }
    })

    pni.addDeviceInterest("hello")
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(1)))
    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests, `is`(equalTo(setOf("hello"))))

    pni.addDeviceInterest("hello")
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(1)))

    pni.removeDeviceInterest("hello")
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(2)))
    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests, `is`(equalTo(setOf())))

    pni.removeDeviceInterest("hello")
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(2)))

    pni.setDeviceInterests(setOf("hello", "panda"))
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(3)))
    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests, `is`(equalTo(setOf("hello", "panda"))))

    pni.setDeviceInterests(setOf("hello", "panda"))
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(3)))

    pni.clearDeviceInterests()
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(4)))
    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests, `is`(equalTo(setOf())))

    pni.clearDeviceInterests()
    assertThat(setOnSubscriptionsChangedListenerCalledCount, `is`(equalTo(4)))
  }

  @Test
  @Ignore // not really working
  fun onSubscriptionsChangedListenerShouldBeCalledIfInterestsChangeDuringDeviceRegistration() {
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    assertStoredDeviceIdIsNotNull()

    pni.addDeviceInterest("hello")

    // force a fresh state locally, but keep the device in the server
    wipeLocalState()

    var setOnSubscriptionsChangedListenerCalledCount = 0
    var lastSetOnSubscriptionsChangedListenerCalledWithInterests: Set<String>? = null
    var lastSetOnSubscriptionsChangedListenerCalledThread: Thread? = null
    pni.setOnDeviceInterestsChangedListener(object : SubscriptionsChangedListener {
      override fun onSubscriptionsChanged(interests: Set<String>) {
        setOnSubscriptionsChangedListenerCalledCount++
        lastSetOnSubscriptionsChangedListenerCalledWithInterests = interests
        lastSetOnSubscriptionsChangedListenerCalledThread = Thread.currentThread()
      }
    })

    // this will cause it to receive the initial interest set of {"hello"}
    pni.start()

    await.atMost(1, TimeUnit.SECONDS) until {
      setOnSubscriptionsChangedListenerCalledCount == 1
    }

    assertThat(lastSetOnSubscriptionsChangedListenerCalledWithInterests, `is`(equalTo(setOf("hello"))))

    // We want to run these callbacks from the UI thread as it's more likely to be useful
    // for the customers. Although, we are not going to make any promises at this point
    val mainThread = InstrumentationRegistry.getInstrumentation().targetContext.mainLooper.thread
    assertThat(lastSetOnSubscriptionsChangedListenerCalledThread, `is`(equalTo(mainThread)))
  }
}
