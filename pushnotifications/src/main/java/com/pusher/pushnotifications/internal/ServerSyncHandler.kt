package com.pusher.pushnotifications.internal

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.pusher.pushnotifications.api.PushNotificationsAPI
import java.io.Serializable

sealed class ServerSyncJob: Serializable
data class StartJob(val fcmToken: String): ServerSyncJob()
data class SubscribeJob(val interest: String): ServerSyncJob()
data class UnsubscribeJob(val interest: String): ServerSyncJob()
data class SetSubscriptionsJob(val interests: Set<String>): ServerSyncJob()

class ServerSyncHandler(
    private val api: PushNotificationsAPI,
    private val deviceStateStore: DeviceStateStore,
    private val jobQueue: PersistentJobQueue<ServerSyncJob>,
    looper: Looper
): Handler(looper) {
  private val serverSyncProcessHandlerThread = HandlerThread(looper.thread.name + "-inner-worker")
  private val serverSyncProcessHandlerHandler = ServerSyncProcessHandler(deviceStateStore, jobQueue, looper)

  override fun handleMessage(msg: Message) {
    super.handleMessage(msg)
    val job = msg.obj as ServerSyncJob
    jobQueue.push(job)

    serverSyncProcessHandlerHandler.sendEmptyMessage(0)

    if (job is StartJob) {
//      api.registerOrRefreshFCM(job.fcmToken, oldSDKDeviceStateStore.clientIds(), {
//        object : OperationCallback<PushNotificationsAPI.RegisterDeviceResult> {
//          override fun onSuccess(result: PushNotificationsAPI.RegisterDeviceResult) {
//            if (deviceStateStore.deviceId == null) {
//              synchronized(deviceStateStore) {
//                val previousLocalInterests = deviceStateStore.interests
//                deviceStateStore.interests = result.initialInterests.toMutableSet()
//
//                jobQueue.forEach({ job -> job() })
//
//                api.setSubscriptions(result.deviceId, deviceStateStore.interests, OperationCallbackNoArgs.noop)
//
//                if (!previousLocalInterests.equals(deviceStateStore.interests)) {
//                  onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
//                }
//              }
//
//              jobQueue.clear()
//              log.i("Successfully started PushNotifications")
//            }
//
//            deviceStateStore.deviceId = result.deviceId
//            deviceStateStore.FCMToken = fcmToken
//          }
//
//          override fun onFailure(t: Throwable) {
//            log.w("Failed to start PushNotifications", t)
//          }
//        }
//      }())
//    }

      serverSyncProcessHandlerThread.start()
    }
  }

  companion object {
    fun start(fcmToken: String): Message =
      Message().also {
        it.obj = StartJob(fcmToken)
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
    private val deviceStateStore: DeviceStateStore,
    private val jobQueue: PersistentJobQueue<ServerSyncJob>,
    looper: Looper
): Handler(looper) {
  var started = false

  fun processJob(job: ServerSyncJob) {
  }

  override fun handleMessage(msg: Message) {
    jobQueue.peek()?.let { job ->

      processJob(job)
    }
  }
}
