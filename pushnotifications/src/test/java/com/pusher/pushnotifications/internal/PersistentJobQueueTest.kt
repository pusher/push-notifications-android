package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.api.DeviceMetadata
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshisealed.reflect.MoshiSealedJsonAdapterFactory
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class PersistentJobQueueTest {
  lateinit var tempFile: File
  private val moshi = Moshi.Builder()
          .add(MoshiSealedJsonAdapterFactory())
          .add(KotlinJsonAdapterFactory()).build()
  private val converter = MoshiConverter(moshi.adapter(ServerSyncJob::class.java))

  @Before
  fun before() {
    tempFile = File.createTempFile("persistentJobQueue-", ".queue")
    tempFile.delete() // QueueFile expects a handle to a non-existent file on first run.
  }

  @Test
  fun `that calling peek returns the head element`() {
    val queueElement = SubscribeJob("Cabbage")
    val otherQueueElement = SubscribeJob("Avocado")

    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    assertNull(queue.peek())

    queue.push(queueElement)
    assertEquals(queueElement, queue.peek())

    queue.push(otherQueueElement)
    assertEquals(queueElement, queue.peek())
  }

  @Test
  fun `that calling pop removes items from the queue`() {
    val queueElement = SubscribeJob("Cabbage")
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)

    queue.push(queueElement)
    assertNotNull(queue.peek())
    queue.pop()
    assertNull(queue.peek())
  }

  @Test
  fun `queue is persistent (not in memory)`() {
    val queueElement = SubscribeJob("Radish")

    val queue1: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue1.push(queueElement)

    val queue2: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)

    assertEquals(queueElement, queue2.peek())
  }

  @Test
  fun `clear removes all elements`() {
    val queueElement = SubscribeJob("Cabbage")
    val otherQueueElement = SubscribeJob("Avocado")

    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(queueElement)
    queue.push(otherQueueElement)
    queue.clear()

    assertNull(queue.peek())
  }

  @Test
  fun `iterables returns correct Start Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(StartJob("fcm_token", arrayListOf("phone1", "phone2")))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(StartJob::class.java, returnedList[0].javaClass)
    val startJob = returnedList[0] as StartJob
    assertEquals("fcm_token", startJob.fcmToken)
    assertEquals(2, startJob.knownPreviousClientIds.size)
    assertEquals("phone1", startJob.knownPreviousClientIds[0])
    assertEquals("phone2", startJob.knownPreviousClientIds[1])
  }

  @Test
  fun `iterables returns correct Refresh Token Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(RefreshTokenJob("new_token"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(RefreshTokenJob::class.java, returnedList[0].javaClass)
    val refreshTokenJob = returnedList[0] as RefreshTokenJob
    assertEquals("new_token", refreshTokenJob.newToken)
  }

  @Test
  fun `iterables returns correct Subscribe Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(SubscribeJob("carrots"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(SubscribeJob::class.java, returnedList[0].javaClass)
    val subscribeJob = returnedList[0] as SubscribeJob
    assertEquals("carrots", subscribeJob.interest)
  }

  @Test
  fun `iterables returns correct Unsubscribe Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(UnsubscribeJob("asparagus"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(UnsubscribeJob::class.java, returnedList[0].javaClass)
    val unsubscribeJob = returnedList[0] as UnsubscribeJob
    assertEquals("asparagus", unsubscribeJob.interest)
  }

  @Test
  fun `iterables returns correct Set Subscriptions Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(SetSubscriptionsJob(setOf("apples", "pears")))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(SetSubscriptionsJob::class.java, returnedList[0].javaClass)
    val unsubscribeJob = returnedList[0] as SetSubscriptionsJob
    assertEquals("apples", unsubscribeJob.interests.first())
    assertEquals("pears", unsubscribeJob.interests.last())
  }

  @Test
  fun `iterables returns correct Application Start Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(ApplicationStartJob(
            DeviceMetadata("18", "5")))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(ApplicationStartJob::class.java, returnedList[0].javaClass)
    val applicationStartJob = returnedList[0] as ApplicationStartJob
    assertEquals("18", applicationStartJob.deviceMetadata.sdkVersion)
    assertEquals("5", applicationStartJob.deviceMetadata.androidVersion)
  }

  @Test
  fun `iterables returns correct Application Set User Id Job`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(SetUserIdJob("cucas"))

    val returnedList = queue.asIterable().toList()

    assertNotNull(returnedList)
    assertEquals(1, returnedList.size)

    assertEquals(SetUserIdJob::class.java, returnedList[0].javaClass)
    val setUserIdJob = returnedList[0] as SetUserIdJob
    assertEquals("cucas", setUserIdJob.userId)
  }

  @Test
  fun `iterables returns correct Server Sync Job Types`() {
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)
    queue.push(StartJob("fcm_token", arrayListOf("phone1", "phone2")))
    queue.push(RefreshTokenJob("new_token"))
    queue.push(SubscribeJob("carrots"))
    queue.push(UnsubscribeJob("okra"))
    queue.push(SetSubscriptionsJob(setOf("apples", "pears")))
    queue.push(ApplicationStartJob(
            DeviceMetadata("16", "10")))
    queue.push(SetUserIdJob("danielle"))
    queue.push(StopJob())

    val retrievedElements = queue.asIterable().toList()

    assertNotNull(retrievedElements)
    assertEquals(8, retrievedElements.size)

    assertTrue(retrievedElements[0] is StartJob)
    assertTrue(retrievedElements[1] is RefreshTokenJob)
    assertTrue(retrievedElements[2] is SubscribeJob)
    assertTrue(retrievedElements[3] is UnsubscribeJob)
    assertTrue(retrievedElements[4] is SetSubscriptionsJob)
    assertTrue(retrievedElements[5] is ApplicationStartJob)
    assertTrue(retrievedElements[6] is SetUserIdJob)
    assertTrue(retrievedElements[7] is StopJob)
  }


  @Test
  fun `corrupted saved data - existing type object has field added`() {
    val tempFile = File("src/test/resources/com/pusher/pushnotifications/internal/persistentJobQueue-corrupted_existing_object_field_added.queue")
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)

      // uncomment the following to write this to the file
//    queue.push(SubscribeJob("potato", 5)) //added an interest level of type int field
//    queue.push(UnsubscribeJob("carrot"))
//    queue.push(UnsubscribeJob("pear"))

      val retrievedElements = queue.asIterable().toList()
      assertEquals(3, retrievedElements.size)

      assertEquals((retrievedElements.first() as SubscribeJob).interest, "potato")
      assertEquals((retrievedElements[1] as UnsubscribeJob).interest, "carrot")
      assertEquals((retrievedElements[2] as UnsubscribeJob).interest, "pear")
  }

  @Test
  fun `corrupted saved data - existing type object has field removed`() {
    val tempFile = File("src/test/resources/com/pusher/pushnotifications/internal/persistentJobQueue-corrupted_existing_object_field_removed.queue")
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)

    // uncomment the following to write this to the file
//    queue.push(StartJob("fcm_token")) //removed the knownPreviousClientIds field
//    queue.push(UnsubscribeJob("carrot"))
//    queue.push(UnsubscribeJob("pear"))

    val retrievedElements = queue.asIterable().toList()
    assertEquals(2, retrievedElements.size)

    // queue element 0 could not be parsed (missing the knownPreviousClientids so the whole record gets ignored

    assertEquals((retrievedElements[0] as UnsubscribeJob).interest, "carrot")
    assertEquals((retrievedElements[1] as UnsubscribeJob).interest, "pear")

  }

  @Test
  fun `corrupted saved data - existing type no longer exists`() {
    val tempFile = File("src/test/resources/com/pusher/pushnotifications/internal/persistentJobQueue-corrupted_existing_type_no_longer_exists.queue")
    val queue: PersistentJobQueue<ServerSyncJob> = TapeJobQueue(tempFile, converter)

    // uncomment the following to write this to the file
//    queue.push(DummyJob("dummy_data")) // this data class no longer exists!
//    queue.push(UnsubscribeJob("carrot"))
//    queue.push(UnsubscribeJob("pear"))

    val retrievedElements = queue.asIterable().toList()
    assertEquals(2, retrievedElements.size)

    // queue element 0 could not be parsed (missing the knownPreviousClientids so the whole record gets ignored

    assertEquals((retrievedElements[0] as UnsubscribeJob).interest, "carrot")
    assertEquals((retrievedElements[1] as UnsubscribeJob).interest, "pear")
  }

}
