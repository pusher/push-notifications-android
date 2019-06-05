package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.api.DeviceMetadata
import com.squareup.moshi.Moshi
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.File

class PersistentJobQueueTest {
  lateinit var tempFile: File
  private val moshi = Moshi.Builder().add(ServerSyncJsonAdapters.polymorphicJsonAdapterFactory).build()
  private val converter = MoshiConverter(moshi.adapter(ServerSyncJob::class.java))

  @Before
  fun before() {
    tempFile = File.createTempFile("persistentJobQueue-", ".queue")
    tempFile.delete() // QueueFile expects a handle to a non-existent file on first run.
  }

  @Test
  fun `iterables returns correct Start Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(StartJob("fcm_token", arrayListOf("one", "two")))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(StartJob::class.java, returnedList[0].javaClass)
    val startJob:StartJob = returnedList[0] as StartJob
    assertEquals("fcm_token", startJob.fcmToken)
    assertEquals(2, startJob.knownPreviousClientIds.size)
    assertEquals("one", startJob.knownPreviousClientIds[0])
    assertEquals("two", startJob.knownPreviousClientIds[1])
  }

  @Test
  fun `iterables returns correct Refresh Token Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(RefreshTokenJob("new_token"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(RefreshTokenJob::class.java, returnedList[0].javaClass)
    val refreshTokenJob:RefreshTokenJob = returnedList[0] as RefreshTokenJob
    assertEquals("new_token", refreshTokenJob.newToken)
  }

  @Test
  fun `iterables returns correct Subscribe Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(SubscribeJob("interest_subscribe"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(SubscribeJob::class.java, returnedList[0].javaClass)
    val subscribeJob:SubscribeJob = returnedList[0] as SubscribeJob
    assertEquals("interest_subscribe", subscribeJob.interest)
  }

  @Test
  fun `iterables returns correct Unsubscribe Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(UnsubscribeJob("interest_unsubscribe"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(UnsubscribeJob::class.java, returnedList[0].javaClass)
    val unsubscribeJob:UnsubscribeJob = returnedList[0] as UnsubscribeJob
    assertEquals("interest_unsubscribe", unsubscribeJob.interest)
  }

  @Test
  fun `iterables returns correct Set Subscriptions Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(SetSubscriptionsJob(setOf("subscribe_one", "subscribe_two")))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(SetSubscriptionsJob::class.java, returnedList[0].javaClass)
    val unsubscribeJob:SetSubscriptionsJob = returnedList[0] as SetSubscriptionsJob
    assertEquals("subscribe_one", unsubscribeJob.interests.first())
    assertEquals("subscribe_two", unsubscribeJob.interests.last())
  }

  @Test
  fun `iterables returns correct Application Start Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(ApplicationStartJob(
            DeviceMetadata("sdk_version", "android_version")))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(ApplicationStartJob::class.java, returnedList[0].javaClass)
    val applicationStartJob:ApplicationStartJob = returnedList[0] as ApplicationStartJob
    assertEquals("sdk_version", applicationStartJob.deviceMetadata.sdkVersion)
    assertEquals("android_version", applicationStartJob.deviceMetadata.androidVersion)
  }

  @Test
  fun `iterables returns correct Application Set User Id Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(SetUserIdJob("user_id"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(SetUserIdJob::class.java, returnedList[0].javaClass)
    val setUserIdJob:SetUserIdJob = returnedList[0] as SetUserIdJob
    assertEquals("user_id", setUserIdJob.userId)
  }

  @Test
  fun `iterables returns correct Server Sync Job Types`() {

    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)

    queue.push(StartJob("fcm_token", arrayListOf("one", "two")))
    queue.push(RefreshTokenJob("new_token"))
    queue.push(SubscribeJob("interest_subscribe"))
    queue.push(UnsubscribeJob("interest_unsubscribe"))
    queue.push(SetSubscriptionsJob(setOf("subscribe_one", "subscribe_two")))
    queue.push(ApplicationStartJob(
            DeviceMetadata("sdk_version", "android_version")))
    queue.push(SetUserIdJob("user_id"))
    queue.push(StopJob())

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(8, returnedList.size)

    assertEquals(StartJob::class.java, returnedList[0].javaClass)
    assertEquals(RefreshTokenJob::class.java, returnedList[1].javaClass)
    assertEquals(SubscribeJob::class.java, returnedList[2].javaClass)
    assertEquals(UnsubscribeJob::class.java, returnedList[3].javaClass)
    assertEquals(SetSubscriptionsJob::class.java, returnedList[4].javaClass)
    assertEquals(ApplicationStartJob::class.java, returnedList[5].javaClass)
    assertEquals(SetUserIdJob::class.java, returnedList[6].javaClass)
    assertEquals(StopJob::class.java, returnedList[7].javaClass)

  }

}