package com.pusher.pushnotifications

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pusher.pushnotifications.api.DeviceMetadata
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.api.PushNotificationsAPIUnprocessableEntity
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.internal.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshisealed.reflect.MoshiSealedJsonAdapterFactory
import junit.framework.Assert.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.IllegalStateException
import java.lang.RuntimeException

@RunWith(AndroidJUnit4::class)
class ServerSyncProcessHandlerTest {
  val instanceId = "000000-c1de-09b9-a8f6-2a22dbdd062a"

  @Before
  @After
  fun cleanup() {
    val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)
    Assert.assertTrue(deviceStateStore.clear())
    Assert.assertNull(deviceStateStore.deviceId)
    Assert.assertThat(deviceStateStore.interests.size, `is`(equalTo(0)))
  }

  private val mockServer = MockWebServer().apply { start() }
  private val api = PushNotificationsAPI(instanceId, mockServer.url("/").toString())
  private val deviceStateStore = InstanceDeviceStateStore(InstrumentationRegistry.getInstrumentation().targetContext, instanceId)
  val moshi = Moshi.Builder()
          .add(MoshiSealedJsonAdapterFactory())
          .add(KotlinJsonAdapterFactory()).build()
  val converter = MoshiConverter(moshi.adapter(ServerSyncJob::class.java))

  private val jobQueue = {
    val tempFile = File.createTempFile("persistentJobQueue-", ".queue")
    tempFile.delete() // QueueFile expects a handle to a non-existent file on first run.
    TapeJobQueue<ServerSyncJob>(tempFile, converter)
  }()
  private val looper = InstrumentationRegistry.getInstrumentation().targetContext.mainLooper
  private var lastHandleServerSyncEvent: ServerSyncEvent? = null
  private var tokenProvider: TokenProvider? = null
  private val handler = ServerSyncProcessHandler(
      api = api,
      deviceStateStore = deviceStateStore,
      jobQueue = jobQueue,
      handleServerSyncEvent = { lastHandleServerSyncEvent = it},
      getTokenProvider = { tokenProvider },
      looper = looper
  )

  /**
   * Non-start jobs should be skipped if the SDK has not been started yet.
   */
  @Test
  fun skipJobsBeforeStart() {
    listOf(
        ServerSyncHandler.subscribe("interest-0"),
        ServerSyncHandler.subscribe("interest-1"),
        ServerSyncHandler.unsubscribe("interest-0"),
        ServerSyncHandler.setSubscriptions(setOf("interest-0", "interest-2"))
    ).forEach { job ->
      jobQueue.push(job.obj as ServerSyncJob)
      handler.handleMessage(job)
    }

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
  }

  @Test
  fun doPendingJobsAfterStart() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    listOf(
        ServerSyncHandler.subscribe("interest-0"),
        ServerSyncHandler.subscribe("interest-1"),
        ServerSyncHandler.unsubscribe("interest-0"),
        ServerSyncHandler.setSubscriptions(setOf("interest-0", "interest-2"))
    ).forEach { job ->
      jobQueue.push(job.obj as ServerSyncJob)
      handler.handleMessage(job)
    }

    assertThat(mockServer.requestCount, `is`(equalTo(0)))

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))
    // expect set interests
    mockServer.enqueue(MockResponse().setBody(""))

    jobQueue.push(startJob.obj as ServerSyncJob)
    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertTrue(jobQueue.peek() == null)
  }

  @Test
  fun doJobsAfterStart() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    listOf(
        ServerSyncHandler.subscribe("interest-0"),
        ServerSyncHandler.subscribe("interest-1"),
        ServerSyncHandler.unsubscribe("interest-0"),
        ServerSyncHandler.setSubscriptions(setOf("interest-0", "interest-2"))
    ).withIndex().forEach { (index, job) ->
      assertThat(mockServer.requestCount, `is`(equalTo(1 + index)))
      jobQueue.push(job.obj as ServerSyncJob)
      mockServer.enqueue(MockResponse().setBody(""))
      handler.handleMessage(job)
      assertThat(mockServer.requestCount, `is`(equalTo(1 + index + 1)))
    }

    assertTrue(jobQueue.peek() == null)
  }

  @Test
  fun startAndThenRefreshToken() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    val refreshTokenJob = ServerSyncHandler.refreshToken("new-token")
    jobQueue.push(refreshTokenJob.obj as ServerSyncJob)
    mockServer.enqueue(MockResponse().setBody(""))
    handler.handleMessage(refreshTokenJob)
    assertThat(mockServer.requestCount, `is`(equalTo(2)))

    assertTrue(jobQueue.peek() == null)
  }

  @Test
  fun startDoSomeOperationsWhileHandlingUnexpectedDeviceDeletionCorrectly() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    // expect to fail with 404 not found the next subscribe
    mockServer.enqueue(MockResponse().setResponseCode(404).setBody(""))

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))
    // expect set subscriptions
    mockServer.enqueue(MockResponse().setBody(""))

    val subJob = ServerSyncHandler.subscribe("hello")
    jobQueue.push(subJob.obj as ServerSyncJob)
    handler.handleMessage(subJob)

    assertThat(mockServer.requestCount, `is`(equalTo(4)))

    assertTrue(jobQueue.peek() == null)
  }

  @Test
  fun startDoSomeOperationsWhileHandlingUnexpectedDeviceDeletionCorrectlyWithUserId() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    // setting the user id now
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNull(deviceStateStore.userId)

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(setUserIdJob)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))

    // expect to fail with 404 not found the next subscribe
    mockServer.enqueue(MockResponse().setResponseCode(404).setBody(""))

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    // expect set subscriptions
    mockServer.enqueue(MockResponse().setBody(""))

    val subJob = ServerSyncHandler.subscribe("hello")
    jobQueue.push(subJob.obj as ServerSyncJob)
    handler.handleMessage(subJob)

    assertThat(mockServer.requestCount, `is`(equalTo(6)))
    assertTrue(jobQueue.peek() == null)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))
  }

  @Test
  fun aDeviceWithAUserShouldBecomeACleanDeviceIfTokenProviderFails() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    // setting the user id now
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNull(deviceStateStore.userId)

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(setUserIdJob)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))

    // any other set user id
    tokenProvider = ExceptionalTokenProvider()

    // subscribe to an interest
    val subJob = ServerSyncHandler.subscribe("hello")
    jobQueue.push(subJob.obj as ServerSyncJob)
    deviceStateStore.interests = deviceStateStore.interests.apply { add("hello") }

    // expect subscribe
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(subJob)

    // do set metadata job that will fail with a 404
    val subJob2 = ServerSyncHandler.subscribe("hello-2")
    jobQueue.push(subJob2.obj as ServerSyncJob)
    deviceStateStore.interests = deviceStateStore.interests.apply { add("hello-2") }

    // expect to fail with 404 not found for the sub
    mockServer.enqueue(MockResponse().setResponseCode(404).setBody(""))

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect the set subscriptions after the device recreation
    mockServer.enqueue(MockResponse().setBody(""))

    // expect the subscribe 2
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(subJob2)

    assertThat(mockServer.requestCount, `is`(equalTo(7)))
    assertTrue(jobQueue.peek() == null)
    assertNull(deviceStateStore.userId)

    // the device recreation logic will not clear the previous interests
    assertThat(deviceStateStore.interests, `is`(equalTo(setOf("hello", "hello-2"))))
  }

  @Test
  fun skipJobsIf400BadRequestAreReceived() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    // expect to fail with 400 for the next subscribe
    mockServer.enqueue(MockResponse().setResponseCode(400).setBody(""))

    // expect an unsubscribe
    mockServer.enqueue(MockResponse().setBody(""))

    val subJob = ServerSyncHandler.subscribe("hello")
    jobQueue.push(subJob.obj as ServerSyncJob)
    handler.handleMessage(subJob)

    val unsubJob = ServerSyncHandler.unsubscribe("hello")
    jobQueue.push(unsubJob.obj as ServerSyncJob)
    handler.handleMessage(unsubJob)

    assertThat(mockServer.requestCount, `is`(equalTo(3)))

    assertTrue(jobQueue.peek() == null)
  }

  @Test
  fun applicationStartJobWillSyncMetadataIfNeeded() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    val deviceMetadata = DeviceMetadata(sdkVersion = "123", androidVersion = "X")
    val applicationStartJob = ServerSyncHandler.applicationStart(deviceMetadata)
    jobQueue.push(applicationStartJob.obj as ServerSyncJob)

    // expect set metadata to be called
    mockServer.enqueue(MockResponse().setBody(""))
    // expect set subscriptions to be called
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(applicationStartJob)

    assertThat(mockServer.requestCount, `is`(equalTo(3)))

    // if nothing changes, then there are no server requests
    jobQueue.push(applicationStartJob.obj as ServerSyncJob)
    handler.handleMessage(applicationStartJob)
    assertThat(mockServer.requestCount, `is`(equalTo(3)))

    val newDeviceMetadata = DeviceMetadata(sdkVersion = "9001", androidVersion = "X")
    val newApplicationStartJob = ServerSyncHandler.applicationStart(newDeviceMetadata)

    // expect set metadata to be called
    mockServer.enqueue(MockResponse().setBody(""))

    jobQueue.push(newApplicationStartJob.obj as ServerSyncJob)
    handler.handleMessage(newApplicationStartJob)
    assertThat(mockServer.requestCount, `is`(equalTo(4)))
  }

  @Test
  fun applicationStartJobWillNotRetrySyncMetadata() {
    val flakyMockServer = MockWebServer().apply { start() }

    val flakyAPI = PushNotificationsAPI(instanceId, flakyMockServer.url("/").toString())
    val handlerWithBrokenAPI =
        ServerSyncProcessHandler(flakyAPI, deviceStateStore, jobQueue, { lastHandleServerSyncEvent = it}, { tokenProvider }, looper)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    flakyMockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handlerWithBrokenAPI.handleMessage(startJob)

    assertThat(flakyMockServer.requestCount, `is`(equalTo(1)))

    val deviceMetadata = DeviceMetadata(sdkVersion = "123", androidVersion = "X")
    val applicationStartJob = ServerSyncHandler.applicationStart(deviceMetadata)
    jobQueue.push(applicationStartJob.obj as ServerSyncJob)

    // expect set metadata to be called, but the server is now dead
    flakyMockServer.close()

    handlerWithBrokenAPI.handleMessage(applicationStartJob)

    // the test finishes means that it didn't retry indefinitely
  }

  @Test
  fun applicationStartJobWillSyncInterestsIfNeeded() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    val deviceMetadata = DeviceMetadata(sdkVersion = "123", androidVersion = "X")
    val applicationStartJob = ServerSyncHandler.applicationStart(deviceMetadata)
    jobQueue.push(applicationStartJob.obj as ServerSyncJob)

    // expect set metadata to be called
    mockServer.enqueue(MockResponse().setBody(""))
    // expect set subscriptions to be called
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(applicationStartJob)

    assertThat(mockServer.requestCount, `is`(equalTo(3)))

    // if nothing changes, then there are no server requests
    jobQueue.push(applicationStartJob.obj as ServerSyncJob)
    handler.handleMessage(applicationStartJob)
    assertThat(mockServer.requestCount, `is`(equalTo(3)))

    val newDeviceMetadata = DeviceMetadata(sdkVersion = "9001", androidVersion = "X")
    val newApplicationStartJob = ServerSyncHandler.applicationStart(newDeviceMetadata)


    // send a set subscriptions and make the server return a 400 so
    // that it fails to sync the interests and doesn't retry
    mockServer.enqueue(MockResponse().setBody("").setResponseCode(400))
    val setSubscriptionsJob = ServerSyncHandler.setSubscriptions(setOf("orange"))
    jobQueue.push(setSubscriptionsJob.obj as ServerSyncJob)
    handler.handleMessage(setSubscriptionsJob)
    assertThat(mockServer.requestCount, `is`(equalTo(4)))

    // expect set subscriptions to be called when booting as the previous sync failed
    mockServer.enqueue(MockResponse().setBody(""))
    jobQueue.push(newApplicationStartJob.obj as ServerSyncJob)
    handler.handleMessage(newApplicationStartJob)
    assertThat(mockServer.requestCount, `is`(equalTo(5)))
  }


  @Test
  fun applicationStartJobShouldBeIgnoredBeforeStart() {
    // The application boots, ans an ApplicationStartJob is enqueued
    val deviceMetadata = DeviceMetadata(sdkVersion = "123", androidVersion = "X")
    val applicationStartJob = ServerSyncHandler.applicationStart(deviceMetadata)
    jobQueue.push(applicationStartJob.obj as ServerSyncJob)
    handler.handleMessage(applicationStartJob)

    // Nothing should happen
    assertThat(mockServer.requestCount, `is`(equalTo(0)))

    // A start job is enqueued
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device and nothing else
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
  }

  @Test
  fun stopShouldDeleteTheDeviceFromTheServerAndClearDeviceStateStore() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNotNull(deviceStateStore.deviceId)

    val stopJob = ServerSyncHandler.stop()
    jobQueue.push(stopJob.obj as ServerSyncJob)

    // expect delete device
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(stopJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNull(deviceStateStore.deviceId)
  }

  @Test
  fun stopIfNotStartedShouldBeFine() {
    val stopJob = ServerSyncHandler.stop()
    jobQueue.push(stopJob.obj as ServerSyncJob)
    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.deviceId)
    assertThat(deviceStateStore.interests.size, `is`(equalTo(0)))
  }

  @Test
  fun stopShouldClearAnyPendingChangesBeforeStart() {
    // + □ + ▷ should result in a single + subscription
    val subJob1 = ServerSyncHandler.subscribe("hello")
    jobQueue.push(subJob1.obj as ServerSyncJob)
    handler.handleMessage(subJob1)

    val stopJob = ServerSyncHandler.stop()
    jobQueue.push(stopJob.obj as ServerSyncJob)

    val subJob2 = ServerSyncHandler.subscribe("goodbye")
    jobQueue.push(subJob2.obj as ServerSyncJob)
    handler.handleMessage(subJob2)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": ["portugal"]}"""))

    // expect set subscriptions
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertThat(deviceStateStore.interests, `is`(equalTo(setOf("portugal", "goodbye"))))
  }

  @Test
  fun startStopStopShouldDeleteTheDeviceFromTheServerAndClearDeviceStateStore() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNotNull(deviceStateStore.deviceId)

    deviceStateStore.interests = mutableSetOf("hello")

    val stopJob = ServerSyncHandler.stop()
    jobQueue.push(stopJob.obj as ServerSyncJob)

    // expect delete device
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(stopJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNull(deviceStateStore.deviceId)

    // and stopping again should do nothing
    handler.handleMessage(stopJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNull(deviceStateStore.deviceId)
  }

  @Test
  fun setUserIdAfterStartShouldSetTheUserIdInTheServerAndDeviceStateStore() {
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNotNull(deviceStateStore.deviceId)

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))
  }

  @Test
  fun setUserIdBeforeStartShouldSetTheUserIdInTheServerAndDeviceStateStore() {
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.userId)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNotNull(deviceStateStore.deviceId)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))

    assertNotNull(lastHandleServerSyncEvent)
    assertThat(lastHandleServerSyncEvent!! as UserIdSet, `is`(equalTo(UserIdSet(userId, null))))
  }

  @Test
  fun setUserIdShouldReturnErrorEventIfTokenProviderThrowsAnException() {
    val userId = "alice"
    tokenProvider = ExceptionalTokenProvider()

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.userId)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNotNull(deviceStateStore.deviceId)
    assertNull(deviceStateStore.userId)

    assertNotNull(lastHandleServerSyncEvent)
    assertThat((lastHandleServerSyncEvent!! as UserIdSet).userId, `is`(equalTo(userId)))
    assertNotNull((lastHandleServerSyncEvent!! as UserIdSet).pusherCallbackError)
    assertThat((lastHandleServerSyncEvent!! as UserIdSet).pusherCallbackError!!.cause, `is`(equalTo(ExceptionalTokenProvider.exception)))
  }

  @Test
  fun setUserIdShouldReturnErrorEventIfTheUserHasTooManyDevicesAlready() {
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.userId)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNotNull(deviceStateStore.deviceId)
    assertNull(deviceStateStore.userId)

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    // expect set user id returning a 422 unprocessable entity
    mockServer.enqueue(MockResponse().setBody("{}").setResponseCode(422))

    handler.handleMessage(setUserIdJob)

    assertNotNull(lastHandleServerSyncEvent)
    assertThat((lastHandleServerSyncEvent!! as UserIdSet).userId, `is`(equalTo(userId)))
    assertNotNull((lastHandleServerSyncEvent!! as UserIdSet).pusherCallbackError)
    assertNotNull((lastHandleServerSyncEvent!! as UserIdSet).pusherCallbackError!!.cause is PushNotificationsAPIUnprocessableEntity)
  }

  @Test
  fun setUserIdShouldReturnErrorEventIfTokenProviderTimesOut() {
    val userId = "alice"
    tokenProvider = SlowTokenProvider(sleepSecs = 2)
    handler.tokenProviderTimeoutSecs = 1

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.userId)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(startJob)

     assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertNotNull(deviceStateStore.deviceId)
    assertNull(deviceStateStore.userId)

    assertNotNull(lastHandleServerSyncEvent)
    assertThat((lastHandleServerSyncEvent!! as UserIdSet).userId, `is`(equalTo(userId)))
    assertNotNull((lastHandleServerSyncEvent!! as UserIdSet).pusherCallbackError)
    assertThat((lastHandleServerSyncEvent!! as UserIdSet).pusherCallbackError!!.message, containsString("timed out"))
  }

  @Test
  fun setUserIdShouldNotCallTheTokenProviderIfAlreadySet() {
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.userId)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNotNull(deviceStateStore.deviceId)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))

    assertNotNull(lastHandleServerSyncEvent)
    assertThat(lastHandleServerSyncEvent!! as UserIdSet, `is`(equalTo(UserIdSet(userId, null))))

    lastHandleServerSyncEvent = null
    tokenProvider = ExceptionalTokenProvider() // making sure this won't be called as it would fail
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNotNull(deviceStateStore.deviceId)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))

    assertNotNull(lastHandleServerSyncEvent)
    assertThat(lastHandleServerSyncEvent!! as UserIdSet, `is`(equalTo(UserIdSet(userId, null))))
  }

  @Test(expected = IllegalStateException::class)
  fun setUserIdShouldThrowAnExceptionIfSomeoneAttemptsToChangeTheExistingOne() {
    val userId = "alice"
    tokenProvider = StubTokenProvider()

    val setUserIdJob = ServerSyncHandler.setUserId(userId)
    jobQueue.push(setUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(setUserIdJob)

    assertThat(mockServer.requestCount, `is`(equalTo(0)))
    assertNull(deviceStateStore.userId)

    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    // expect set user id
    mockServer.enqueue(MockResponse().setBody(""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(2)))
    assertNotNull(deviceStateStore.deviceId)
    assertThat(deviceStateStore.userId, `is`(equalTo(userId)))

    assertNotNull(lastHandleServerSyncEvent)
    assertThat(lastHandleServerSyncEvent!! as UserIdSet, `is`(equalTo(UserIdSet(userId, null))))

    lastHandleServerSyncEvent = null
    tokenProvider = ExceptionalTokenProvider() // making sure this won't be called as it would fail
    val anotherSetUserIdJob = ServerSyncHandler.setUserId("another-$userId")
    jobQueue.push(anotherSetUserIdJob.obj as ServerSyncJob)

    handler.handleMessage(anotherSetUserIdJob)
  }

  @Test(timeout = 5000)
  fun refreshTokenShouldNotContactTheServerIfTheTokenIsUnchanged() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    val refreshTokenJob = ServerSyncHandler.refreshToken("token-123")
    jobQueue.push(refreshTokenJob.obj as ServerSyncJob)

    // no server call is expected

    handler.handleMessage(refreshTokenJob)
    assertThat(mockServer.requestCount, `is`(equalTo(1)))

    assertTrue(jobQueue.peek() == null)
  }

  @Test
  fun refreshTokenShouldUpdateTheLocalStorage() {
    val startJob = ServerSyncHandler.start("token-123", emptyList())
    jobQueue.push(startJob.obj as ServerSyncJob)

    // expect register device
    mockServer.enqueue(MockResponse().setBody("""{"id": "d-123", "initialInterestSet": []}"""))

    handler.handleMessage(startJob)

    assertThat(mockServer.requestCount, `is`(equalTo(1)))
    assertThat(deviceStateStore.FCMToken, `is`(equalTo("token-123")))

    val refreshTokenJob = ServerSyncHandler.refreshToken("new-token")
    jobQueue.push(refreshTokenJob.obj as ServerSyncJob)

    mockServer.enqueue(MockResponse())

    handler.handleMessage(refreshTokenJob)
    assertThat(mockServer.requestCount, `is`(equalTo(2)))

    assertTrue(jobQueue.peek() == null)
    assertThat(deviceStateStore.FCMToken, `is`(equalTo("new-token")))
  }
}

private class StubTokenProvider(): TokenProvider {
  override fun fetchToken(userId: String): String {
    return "this-is-a-token"
  }
}

private class ExceptionalTokenProvider: TokenProvider {
  companion object {
    val exception: Throwable = RuntimeException("127.0.0.1")
  }

  override fun fetchToken(userId: String): String {
    throw exception
  }
}

private class SlowTokenProvider(val sleepSecs: Long): TokenProvider {
  override fun fetchToken(userId: String): String {
    Thread.sleep(sleepSecs * 1000)
    return "adorable"
  }
}
