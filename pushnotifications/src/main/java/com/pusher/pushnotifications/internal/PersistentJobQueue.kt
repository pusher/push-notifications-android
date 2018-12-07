package com.pusher.pushnotifications.internal

import com.squareup.tape2.QueueFile
import java.io.*

interface PersistentJobQueue<T: Serializable> {
  fun push(job: T)
  fun peek(): T?
  fun pop()
  fun clear()
}


class TapeJobQueue<T: Serializable>(file: File): PersistentJobQueue<T> {
  private val queueFile = QueueFile.Builder(file).build()

  override fun push(job: T) {
    val byteOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteOutputStream)
    objectOutputStream.writeObject(job)
    objectOutputStream.flush()
    val jobBytes = byteOutputStream.toByteArray()

    queueFile.add(jobBytes)
  }

  override fun peek(): T? {
    val jobBytes = queueFile.peek() ?: return null

    val byteInputStream = ByteArrayInputStream(jobBytes)
    val objectInputStream = ObjectInputStream(byteInputStream)

    return objectInputStream.readObject() as T
  }

  override fun pop() {
    queueFile.remove()
  }

  override fun clear() {
    queueFile.clear()
  }
}