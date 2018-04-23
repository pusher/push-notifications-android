package com.pusher.pushnotifications.fcm

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pusher.pushnotifications.PushNotificationReceivedListener
import com.pusher.pushnotifications.internal.AppActivityLifecycleCallbacks
import com.pusher.pushnotifications.logging.Logger
import java.lang.ref.WeakReference

abstract class MessagingService: Service() {
  companion object {
    private val log = Logger.get(this::class)

    private var listenerActivity: WeakReference<Activity>? = null
    private var listener: PushNotificationReceivedListener? = null

    /**
     * Configures the listener that handles a remote message only when this activity is in the
     * foreground.
     *
     * Use this method to update your UI.
     *
     * If you intend to handle a remote message in all circumstances, read the service docs:
     * https://docs.pusher.com/push-notifications/reference/android#handle-incoming-notifications
     *
     * @param messageReceivedListener the listener that handles a remote message
     */
    @JvmStatic
    fun setOnMessageReceivedListenerForVisibleActivity(activity: Activity, messageReceivedListener: PushNotificationReceivedListener) {
      listenerActivity = WeakReference(activity)
      listener = messageReceivedListener
    }
  }

  private class WrappedFirebaseMessagingService(
      val context: () -> Context,
      val onMessageReceivedHandler: (RemoteMessage) -> Unit): FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
      onMessageReceivedHandler(remoteMessage)
    }

    override fun getApplicationContext(): Context {
      return context()
    }
  }

  private val underlyingService: FirebaseMessagingService

  init {
    val onMessageReceivedHandler = { remoteMessage: RemoteMessage ->
      if (remoteMessage.data["pusherTokenValidation"] == "true") {
        log.d("Received blank message from Pusher to perform token validation")
      } else {
        log.d("Received from FCM: $remoteMessage")
        log.d("Received from FCM TITLE: " + remoteMessage.notification?.title)
        log.d("Received from FCM BODY: " + remoteMessage.notification?.body)

        listenerActivity?.get()?.let { lActivity ->
          AppActivityLifecycleCallbacks.currentActivity?.get()?.let { currActivity ->
            if (lActivity == currActivity) {
              listener?.onMessageReceived(remoteMessage)
            }
          }
        }

        this.onMessageReceived(remoteMessage)
      }
    }

    underlyingService = WrappedFirebaseMessagingService({ applicationContext }, onMessageReceivedHandler)
  }

  override fun onBind(i: Intent?): IBinder = underlyingService.onBind(i)

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
      underlyingService.onStartCommand(intent, flags, startId)

  abstract fun onMessageReceived(remoteMessage: RemoteMessage)
}

class EmptyMessagingService: MessagingService() {
  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    // Doesn't do anything; manifest will default to this one
  }
}
