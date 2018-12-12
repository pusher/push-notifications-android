package com.pusher.pushnotifications.internal

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.api.RetryStrategy
import java.io.Serializable

sealed class ServerSyncJob: Serializable
data class StartJob(val fcmToken: String, val knownPreviousClientIds: List<String>): ServerSyncJob()
data class SubscribeJob(val interest: String): ServerSyncJob()
data class UnsubscribeJob(val interest: String): ServerSyncJob()
data class SetSubscriptionsJob(val interests: Set<String>): ServerSyncJob()

class ServerSyncHandler(
    private val api: PushNotificationsAPI,
    private val deviceStateStore: DeviceStateStore,
    private val jobQueue: PersistentJobQueue<ServerSyncJob>,
    looper: Looper
): Handler(looper) {
  private val serverSyncProcessHandlerHandler = {
    val handlerThread = HandlerThread(looper.thread.name + "-inner-worker")
    handlerThread.start()

    ServerSyncProcessHandler(api, deviceStateStore, jobQueue, handlerThread.looper)
  }()

  init {
    // when the app first launches, we should queue up all of the outstanding
    // jobs in the queue so we can pick up where we have left off
    jobQueue.asIterable().forEach { job ->
      serverSyncProcessHandlerHandler.sendMessage(Message().also { it.obj = job })
    }
  }

  override fun handleMessage(msg: Message) {
    super.handleMessage(msg)
    val job = msg.obj as ServerSyncJob
    jobQueue.push(job)

    val clonedMsg = Message()
    clonedMsg.obj = msg.obj
    serverSyncProcessHandlerHandler.sendMessage(clonedMsg)
  }

  companion object {
  fun start(fcmToken: String, knownPreviousClientIds: List<String>): Message =
      Message().also {
        it.obj = StartJob(fcmToken, knownPreviousClientIds)
      }

  fun subscribe(interest: String): Message =
      Message().also {
        it.obj = SubscribeJob(interest)
      }

  fun unsubscribe(interest: String): Message =
      Message().also {
        it.obj = UnsubscribeJob(interest)
      }

  fun setSubscriptions(interests: Set<String>): Message =
      Message().also {
        it.obj = SetSubscriptionsJob(interests)
      }
  }
}

class ServerSyncProcessHandler(
    private val api: PushNotificationsAPI,
    private val deviceStateStore: DeviceStateStore,
    private val jobQueue: PersistentJobQueue<ServerSyncJob>,
    looper: Looper
): Handler(looper) {
  private val started: Boolean
  get() = deviceStateStore.deviceId != null

  private fun processStartJob(startJob: StartJob) {
    // Register device with Errol
    val registrationResponse =
        api.registerFCM(
            token = startJob.fcmToken,
            knownPreviousClientIds = startJob.knownPreviousClientIds,
            retryStrategy = RetryStrategy.WithInfiniteExpBackOff())

    synchronized(deviceStateStore) {
      deviceStateStore.deviceId = registrationResponse.deviceId

      // Replay sub/unsub/setsub operations in job queue over initial interest set
      val interests = registrationResponse.initialInterests.toMutableSet()
      for(j in jobQueue.asIterable()) {
        if (j is StartJob) {
          break
        }
        when(j) {
          is SubscribeJob -> {
            interests += j.interest
          }
          is UnsubscribeJob -> {
            interests -= j.interest
          }
          is SetSubscriptionsJob -> {
            interests.clear()
            interests.addAll(j.interests)
          }
        }
      }

      val localInterestWillChange = deviceStateStore.interests != interests

      // Replace interests with the result
      if (localInterestWillChange) {
        deviceStateStore.interests = interests
        // TODO: call the callback in the UI thread somehow
      }
    }

    val remoteInterestsWillChange = deviceStateStore.interests != registrationResponse.initialInterests
    if (remoteInterestsWillChange) {
      api.setSubscriptions(
          deviceId = registrationResponse.deviceId,
          interests = deviceStateStore.interests,
          retryStrategy = RetryStrategy.WithInfiniteExpBackOff())
    }

    // Clear queue up to the start job
    while (jobQueue.peek() !is StartJob) {
      jobQueue.pop()
    }
    jobQueue.pop() // Also remove start job
  }

  private fun processJob(job: ServerSyncJob) {
    when(job) {
      is SubscribeJob -> {
        api.subscribe(
            deviceStateStore.deviceId!!,
            job.interest,
            RetryStrategy.WithInfiniteExpBackOff())
      }
      is UnsubscribeJob -> {
        api.unsubscribe(
            deviceStateStore.deviceId!!,
            job.interest,
            RetryStrategy.WithInfiniteExpBackOff())
      }
      is SetSubscriptionsJob -> {
        api.setSubscriptions(
            deviceStateStore.deviceId!!,
            job.interests,
            RetryStrategy.WithInfiniteExpBackOff())
      }
    }
    jobQueue.pop()
  }

  override fun handleMessage(msg: Message) {
    val job = msg.obj as ServerSyncJob

    // If the SDK hasn't started yet we can't do anything, so skip
    val shouldSkip = !started && job !is StartJob
    if (shouldSkip) {
      return
    }

    if(job is StartJob) {
      processStartJob(job)
    } else {
      processJob(job)
    }
  }
}
