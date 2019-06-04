package com.pusher.pushnotifications.internal

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
  var queue = ObjectQueue.create( QueueFile.Builder(file).build(), converter )

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
    return queue.asIterable()
  }
}
