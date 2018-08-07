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
    var onRefreshToken: ((String) -> Unit)? = null

    /**
     * Configures the listener that handles a remote message only when this activity is in the
     * foreground.
     *
     * You can use this method to update your UI.
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

    override fun onNewToken(token: String) {
      MessagingService.log.d("Got new or refreshed FCM token: $token")
      MessagingService.onRefreshToken?.let { it(token) }
    }

    override fun getApplicationContext(): Context {
      return context()
    }
  }

  private val underlyingService: FirebaseMessagingService =
      WrappedFirebaseMessagingService({ applicationContext }, { remoteMessage: RemoteMessage ->
        if (remoteMessage.data["pusherTokenValidation"] == "true") {
          log.d("Received blank message from Pusher to perform token validation")
        } else {
          log.d("Received from FCM: $remoteMessage")
          log.d("Received from FCM TITLE: " + remoteMessage.notification?.title)
          log.d("Received from FCM BODY: " + remoteMessage.notification?.body)

          val listenerActivity = listenerActivity?.get()
          val currentActivity = AppActivityLifecycleCallbacks.currentActivity?.get()
          when {
            listenerActivity == null -> log.v("Missing listener activity")
            currentActivity == null -> log.v("No active activity")
            listenerActivity != currentActivity -> log.v("Listener activity and current activity don't match")
            else ->listener?.onMessageReceived(remoteMessage)
          }

          this.onMessageReceived(remoteMessage)
        }
      })

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
