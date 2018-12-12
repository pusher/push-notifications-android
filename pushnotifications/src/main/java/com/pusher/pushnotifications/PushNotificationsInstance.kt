package com.pusher.pushnotifications

import java.util.regex.Pattern
import android.content.Context
import android.os.HandlerThread
import com.google.firebase.iid.FirebaseInstanceId
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.*
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.validation.Validations
import java.io.File

/**
 * Thrown when the device is re-registered to a different instance id. If you wish to register a
 * device to a different instance you will need to reinstall the application.
 *
 * @param message Error message to be shown
 */
class PusherAlreadyRegisteredException(message: String) : RuntimeException(message)

/**
 * Interacts with the Pusher service to subscribe and unsubscribe from interests.
 *
 * @param context the application context
 * @param instanceId the id of the instance
 */
class PushNotificationsInstance(
    context: Context,
    instanceId: String) {
  private val log = Logger.get(this::class)

  private val sdkConfig = SDKConfiguration(context)
  private val deviceStateStore = DeviceStateStore(context)
  private val oldSDKDeviceStateStore = OldSDKDeviceStateStore(context)
  private var onSubscriptionsChangedListener: SubscriptionsChangedListener? = null

  private val serverSyncHandler = {
    val handlerThread = HandlerThread("ServerSyncHandler-$instanceId")
    handlerThread.start()

    ServerSyncHandler(
        api = PushNotificationsAPI(instanceId, sdkConfig.overrideHostURL),
        deviceStateStore = deviceStateStore,
        jobQueue = TapeJobQueue(File(context.filesDir, "$instanceId.jobqueue")),
        looper = handlerThread.looper
    )
  }()

  init {
    Validations.validateApplicationIcon(context)
    PushNotificationsInstance.getInstanceId(context)?.let {
      val isNewInstanceId = it != instanceId
      if (isNewInstanceId) {
        throw PusherAlreadyRegisteredException("This device has already been registered to a Pusher " +
            "Push Notifications application with instance ID: $it. " +
            "If you would like to register this device to $instanceId please reinstall the application.")
      }
    }
    deviceStateStore.instanceId = instanceId
  }

  companion object {
    private val validInterestRegex = Pattern.compile("^[a-zA-Z0-9_\\-=@,.;]{1,164}$").toRegex()

    fun getInstanceId(context: Context): String? {
      return DeviceStateStore(context).instanceId
    }
  }
  

  private fun addInterestToStore(interest: String): Boolean {
    val interests = deviceStateStore.interests
    if (interests.add(interest)) {
      deviceStateStore.interests = interests
      return true
    }
    return false // nothing changed
  }

  private fun removeInterestFromStore(interest: String): Boolean {
    val interests = deviceStateStore.interests
    if (interests.remove(interest)) {
      deviceStateStore.interests = interests
      return true
    }
    return false // nothing changed
  }

  private fun replaceAllInterestsInStore(interests: Set<String>): Boolean {
    val localInterests = deviceStateStore.interests
    val areInterestsDifferent = !(localInterests.containsAll(interests) && interests.containsAll(localInterests))
    if (areInterestsDifferent) {
      deviceStateStore.interests = interests.toMutableSet()
      return true
    }
    return false // nothing changed
  }

  /**
   * Starts the PushNotification client and synchronizes the FCM device token with
   * the Pusher services.
   */
  fun start(): PushNotificationsInstance {
    val handleFcmToken = { fcmToken: String ->
      synchronized(deviceStateStore) {
        if (!deviceStateStore.startHasBeenCalled) {
          serverSyncHandler.sendMessage(ServerSyncHandler.start(fcmToken, oldSDKDeviceStateStore.clientIds()))
          deviceStateStore.startHasBeenCalled = true
        }
      }

      Unit
    }

    MessagingService.onRefreshToken = handleFcmToken
    FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
      if (!task.isSuccessful) {
        log.w("Failed to get the token from FCM", task.exception)
      } else {
        handleFcmToken(task.result.token)
      }
    }

    return this
  }

  /**
   * Subscribes the device to an interest. For example:
   * <pre>{@code pushNotifications.subscribe("hello");}</pre>
   * @param interest the name of the interest
   */
  fun subscribe(interest: String) {
    if (!interest.matches(validInterestRegex)) {
      throw IllegalArgumentException(
          "Interest `$interest` is not valid. It can only contain up to 164 characters " +
              "and can only be ASCII upper/lower-case letters, numbers and one of _-=@,.:")
    }

    synchronized(deviceStateStore) {
      val haveInterestsChanged = addInterestToStore(interest)
      if (haveInterestsChanged) {
        serverSyncHandler.sendMessage(ServerSyncHandler.subscribe(interest))
        onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
      }
    }
  }

  /**
   * Unsubscribes the device from an interest. For example:
   * <pre>{@code pushNotifications.unsubscribe("hello");}</pre>
   * @param interest the name of the interest
   */
  fun unsubscribe(interest: String) {
    synchronized(deviceStateStore) {
      val haveInterestsChanged = removeInterestFromStore(interest)

      if (haveInterestsChanged) {
        serverSyncHandler.sendMessage(ServerSyncHandler.unsubscribe(interest))
        onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
      }
    }
  }

  /**
   * Unsubscribes the device from all the interests. For example:
   * <pre>{@code pushNotifications.unsubscribeAll();}</pre>
   */
  fun unsubscribeAll() {
    setSubscriptions(emptySet())
  }

  /**
   * Sets the subscriptions state for the device. Any interests not in the set will be
   * unsubscribed from, so this will replace the interest set by the one provided.
   * <br>
   * For example:
   * <pre>{@code pushNotifications.setSubscriptions(Arrays.asList("hello", "donuts").toSet());}</pre>
   * @param interests the new set of interests
   */
  fun setSubscriptions(interests: Set<String>) {
    interests.find {
      !it.matches(validInterestRegex)
    }?.let {
      throw IllegalArgumentException(
          "Interest `$it` is not valid. It can only contain up to 164 characters " +
              "and can only be ASCII upper/lower-case letters, numbers and one of _-=@,.:")
    }

    synchronized(deviceStateStore) {
      val haveInterestsChanged = replaceAllInterestsInStore(interests)

      if (haveInterestsChanged) {
        serverSyncHandler.sendMessage(ServerSyncHandler.setSubscriptions(deviceStateStore.interests))
        onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
      }
    }
  }

  /**
   * @return the set of subscriptions that the device is currently subscribed to
   */
  fun getSubscriptions(): Set<String> {
    synchronized(deviceStateStore) {
      return deviceStateStore.interests
    }
  }

  /**
   * Registers a listener for when subscriptions have changed
   *
   * @param listener - the listener object
   */
  fun setOnSubscriptionsChangedListener(listener: SubscriptionsChangedListener) {
    onSubscriptionsChangedListener = listener
  }
}
