package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.logging.Logger
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.tape2.ObjectQueue
import com.squareup.tape2.QueueFile
import java.io.*

interface PersistentJobQueue<T: Serializable> {
  fun push(job: T)
  fun peek(): T?
  fun pop()
  fun clear()
  fun asIterable(): Iterable<T>
}

class TapeJobQueue<T: Serializable>(file: File, converter: ObjectQueue.Converter<T>): PersistentJobQueue<T> {
  private val log = Logger.get(this::class)
  private val noExceptionConverter = object : ObjectQueue.Converter<T?> {
    override fun from(source: ByteArray): T? {
      return try {
        converter.from(source)
      } catch (ex: JsonEncodingException) {
        log.w("Failed to read object data from tape. Continuing without this data.")
        null
      } catch (ex: JsonDataException) {
        log.w("Failed to read object data from tape. Continuing without this data.")
        null
      }
    }

    override fun toStream(value: T?, sink: OutputStream) {
      converter.toStream(value!!, sink)
    }
  }
  private val queue = ObjectQueue.create(QueueFile.Builder(file).build(), noExceptionConverter)

  @Synchronized override fun push(job: T) {
    queue.add(job)
  }

  @Synchronized override fun peek(): T? {
    return queue.peek()
  }

  @Synchronized override fun pop() {
    queue.remove()
  }

  @Synchronized override fun clear() {
    queue.clear()
  }

  @Synchronized override fun asIterable(): Iterable<T> {
    return queue.asIterable().filterNotNull()
  }
}
