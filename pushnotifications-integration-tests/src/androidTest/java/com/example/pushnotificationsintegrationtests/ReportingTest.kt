package com.example.pushnotificationsintegrationtests

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.firebase.jobdispatcher.*
import com.pusher.pushnotifications.*
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.AppActivityLifecycleCallbacks
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import com.pusher.pushnotifications.reporting.ReportingJobService
import com.pusher.pushnotifications.reporting.api.DeliveryEvent
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ReportingTest {
  val context = InstrumentationRegistry.getTargetContext()
  val instanceId = "00000000-1241-08e9-b379-377c32cd1e84"
  val errolClient = ErrolAPI(instanceId, "http://localhost:8080")
  val targetCtx: Context = InstrumentationRegistry.getTargetContext()

  fun getStoredDeviceId(): String? {
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getTargetContext(), instanceId)
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
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getTargetContext(), instanceId)

    await.atMost(1, TimeUnit.SECONDS) until {
      assertTrue(deviceStateStore.clear())

      deviceStateStore.interests.size == 0 && deviceStateStore.deviceId == null
    }

    File(context.filesDir, "$instanceId.jobqueue").delete()
  }

  @Before
  @After
  fun wipeServerReportedNotifications() {
    errol.getInstanceEventsStorage(instanceId).clear()
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
  @Ignore("The job dispatcher will schedule the job but it isn't triggered within a reasonable timeframe")
  fun aNotificationSentFromPusherShouldBeReportedTestBroken() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the device id should exist in the server
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    // send a notification
    val i = Intent()
    i.action = "com.google.android.c2dm.intent.RECEIVE"
    i.`package` = targetCtx.packageName

    val bundle = Bundle()
    // grabbing all the fields from an actual push notification which will include
    // undocumented and Google FCM specific keys. These are left here to ensure that
    // our tests are more realistic and our reporting isn't affected by their presence
    bundle.putString("google.delivered_priority", "high")
    bundle.putLong("google.sent_time", 1574938426317)
    bundle.putLong("google.ttl", 2419200)
    bundle.putString("google.original_priority", "high")
    bundle.putString("gcm.notification.e", "1")
    bundle.putString("pusher", """{"instanceId":"$instanceId","hasDisplayableContent":true,"publishId":"pubid-e3c82c34-667b-4969-9509-ff59dfbe328a"}""")
    i.replaceExtras(bundle)

    targetCtx.sendBroadcast(i)

    await.atMost(90, TimeUnit.MINUTES) until {
      errol.getInstanceEventsStorage(instanceId).any { event ->
        event.publishId == "pubid-e3c82c34-667b-4969-9509-ff59dfbe328a"
      }
    }
  }

  @Test
  fun aNotificationSentFromPusherShouldBeReported() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the device id should exist in the server
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    val reportEvent = DeliveryEvent(
        instanceId = instanceId,
        publishId = "pubid-e3c82c34-667b-4969-9509-ff59dfbe328a",
        deviceId = storedDeviceId,
        userId = "alice",
        timestampSecs = Math.round(System.currentTimeMillis() / 1000.0),
        appInBackground = AppActivityLifecycleCallbacks.appInBackground(),
        hasDisplayableContent = false,
        hasData = true
    )

    val bundledEvent = ReportingJobService.toBundle(reportEvent)

    val job =
        FirebaseJobDispatcher(GooglePlayDriver(context)).newJobBuilder()
            .setService(ReportingJobService::class.java)
            .setConstraints(Constraint.ON_ANY_NETWORK)
            .setTag("pusher.delivered.publishId=pubid-e3c82c34-667b-4969-9509-ff59dfbe328a")
            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
            .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
            .setExtras(bundledEvent)
            .build()

    class ReportingJobServiceWithContext : ReportingJobService() {
      init {
        // Android Studio might render the following function as red, but it works.
        attachBaseContext(context)
      }
    }

    val reportingJobService = ReportingJobServiceWithContext()

    assertTrue(reportingJobService.onStartJob(job))

    await.atMost(5, TimeUnit.SECONDS) until {
      errol.getInstanceEventsStorage(instanceId).any { event ->
        event.publishId == "pubid-e3c82c34-667b-4969-9509-ff59dfbe328a"
      }
    }
  }

  @Test
  fun migrationWithJobScheduledWithMissingInstanceIdDoesNotCrashButLosesTheReport() {
    // Start the SDK
    val pni = PushNotificationsInstance(context, instanceId)
    pni.start()

    // A device ID should have been stored
    assertStoredDeviceIdIsNotNull()
    val storedDeviceId = getStoredDeviceId()

    // and the device id should exist in the server
    assertNotNull(errolClient.getDevice(storedDeviceId!!))

    val reportEvent = DeliveryEvent(
        instanceId = instanceId,
        publishId = "pubid-e3c82c34-667b-4969-9509-ff59dfbe328a",
        deviceId = storedDeviceId,
        userId = "alice",
        timestampSecs = Math.round(System.currentTimeMillis() / 1000.0),
        appInBackground = AppActivityLifecycleCallbacks.appInBackground(),
        hasDisplayableContent = false,
        hasData = true
    )

    val bundledEvent = ReportingJobService.toBundle(reportEvent)
    // old sdk didn't bundle in the instance id, so it will be missing
    bundledEvent.remove("InstanceId")

    val job =
        FirebaseJobDispatcher(GooglePlayDriver(context)).newJobBuilder()
            .setService(ReportingJobService::class.java)
            .setConstraints(Constraint.ON_ANY_NETWORK)
            .setTag("pusher.delivered.publishId=pubid-e3c82c34-667b-4969-9509-ff59dfbe328a")
            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
            .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
            .setExtras(bundledEvent)
            .build()

    class ReportingJobServiceWithContext : ReportingJobService() {
      init {
        attachBaseContext(context)
      }
    }

    val reportingJobService = ReportingJobServiceWithContext()

    assertFalse(reportingJobService.onStartJob(job))

    Thread.sleep(1000)
    assertEquals(errol.getInstanceEventsStorage(instanceId).size, 0)
  }
}

