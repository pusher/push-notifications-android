package com.pusher.pushnotifications.internal

import com.squareup.tape2.QueueFile
import java.io.*

interface PersistentJobQueue<T: Serializable> {
  fun push(job: T)
  fun peek(): T?
  fun pop()
  fun clear()
  fun asIterable(): Iterable<T>
}

class TapeJobQueue<T: Serializable>(file: File): PersistentJobQueue<T> {
  private val queueFile = QueueFile.Builder(file).build()

  override fun push(job: T) {
    val byteOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteOutputStream)
    objectOutputStream.writeObject(job)
    objectOutputStream.flush()
    val jobBytes = byteOutputStream.toByteArray()

    synchronized(queueFile) {
      queueFile.add(jobBytes)
    }
  }

  @Suppress("unchecked_cast")
  override fun peek(): T? {
    val jobBytes = synchronized(queueFile) {
      queueFile.peek() ?: return null
    }

    val byteInputStream = ByteArrayInputStream(jobBytes)
    val objectInputStream = ObjectInputStream(byteInputStream)

    return objectInputStream.readObject() as T
  }

  override fun pop() {
    synchronized(queueFile) {
      queueFile.remove()
    }
  }

  override fun clear() {
    synchronized(queueFile) {
      queueFile.clear()
    }
  }

  @Suppress("unchecked_cast")
  override fun asIterable(): Iterable<T> {
    return synchronized(queueFile) {
      queueFile.asIterable().map { jobBytes ->
        val byteInputStream = ByteArrayInputStream(jobBytes)
        val objectInputStream = ObjectInputStream(byteInputStream)

        objectInputStream.readObject() as T
      }.toList() // forcing the list to be computed here in its entirety
    }
  }
}