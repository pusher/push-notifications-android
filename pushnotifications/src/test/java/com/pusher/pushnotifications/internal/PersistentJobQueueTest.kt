package com.pusher.pushnotifications.internal

import org.hamcrest.CoreMatchers.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class PersistentJobQueueTest {
  lateinit var tempFile: File

  @Before
  fun before() {
    tempFile = File.createTempFile("persistentJobQueue-", ".queue")
    tempFile.delete() // QueueFile expects a handle to a non-existent file on first run.
  }

  @Test
  fun `that calling peek returns the head element`() {
    val queueElement = "Cabbage"
    val otherQueueElement = "Avocado"

    val queue: PersistentJobQueue<String> = TapeJobQueue(tempFile)
    Assert.assertThat(queue.peek(), `is`(nullValue()))

    queue.push(queueElement)
    Assert.assertThat(queue.peek(), `is`(equalTo(queueElement)))

    queue.push(otherQueueElement)
    Assert.assertThat(queue.peek(), `is`(equalTo(queueElement)))
  }

  @Test
  fun `that calling pop removes items from the queue`() {
    val queueElement = "Cabbage"
    val queue: PersistentJobQueue<String> = TapeJobQueue(tempFile)

    queue.push(queueElement)
    queue.pop()

    Assert.assertThat(queue.peek(), `is`(nullValue()))
  }

  @Test
  fun `queue is persistent (not in memory)`() {
    val queueElement = "Radish"

    val queue1: PersistentJobQueue<String> = TapeJobQueue(tempFile)
    queue1.push(queueElement)

    val queue2: PersistentJobQueue<String> = TapeJobQueue(tempFile)

    Assert.assertThat(queue2.peek(), `is`(equalTo(queueElement)))
  }

  @Test
  fun `clear removes all elements`() {
    val queueElement = "Cabbage"
    val otherQueueElement = "Avocado"

    val queue: PersistentJobQueue<String> = TapeJobQueue(tempFile)
    queue.push(queueElement)
    queue.push(otherQueueElement)
    queue.clear()

    Assert.assertThat(queue.peek(), `is`(nullValue()))
  }

  @Test
  fun `iterables returns an an Iterable of items stored`() {
    val queueElement = "Cabbage"
    val otherQueueElement = "Avocado"

    val queue: PersistentJobQueue<String> = TapeJobQueue(tempFile)
    queue.push(queueElement)
    queue.push(otherQueueElement)

    val returnedIterable = queue.asIterable()

    Assert.assertThat(returnedIterable, `is`(not(nullValue())))
    Assert.assertThat(returnedIterable.toList().size, `is`(2))
  }

}