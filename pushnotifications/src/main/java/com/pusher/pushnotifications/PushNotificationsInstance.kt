package com.pusher.pushnotifications

import java.util.regex.Pattern
import android.content.Context
import com.google.firebase.iid.FirebaseInstanceId
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.api.OperationCallbackNoArgs
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.fcm.FCMInstanceIDService
import com.pusher.pushnotifications.internal.DeviceStateStore
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.validation.Validations

/**
 * Thrown when the device is re-registered to a different instance id. If you wish to register a
 * device to a different instance you will need to reinstall the application.
 *
 * @param message Error message to be shown
 */
class PusherAlreadyRegisteredException(message: String) : RuntimeException(message) {}

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

  private val api = PushNotificationsAPI(instanceId)
  private val deviceStateStore = DeviceStateStore(context)
  private val jobQueue: ArrayList<() -> Boolean> = ArrayList()
  private var onSubscriptionsChangedListener: SubscriptionsChangedListener? = null

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
  

  fun addInterestToStore(interest: String): Boolean {
    val interestsSet = deviceStateStore.interestsSet
    if (interestsSet.add(interest)) {
      deviceStateStore.interestsSet = interestsSet
      return true
    }
    return false // nothing changed
  }

  fun removeInterestFromStore(interest: String): Boolean {
    val interestsSet = deviceStateStore.interestsSet
    if (interestsSet.remove(interest)) {
      deviceStateStore.interestsSet = interestsSet
      return true
    }
    return false // nothing changed
  }

  fun replaceAllInterestsInStore(interests: Set<String>): Boolean {
    val localInterestsSet = deviceStateStore.interestsSet
    val areInterestSetsDifferent = localInterestsSet.containsAll(interests) && interests.containsAll(localInterestsSet)
    if (areInterestSetsDifferent) {
      deviceStateStore.interestsSet = localInterestsSet
      return true
    }
    return false // nothing changed
  }

  /**
   * Starts the PushNotification client and synchronizes the FCM device token with
   * the Pusher services.
   */
  fun start(): PushNotificationsInstance {
    deviceStateStore.deviceId?.let {
      api.deviceId = it
      log.i("PushNotifications device id: $it")
    }

    deviceStateStore.FCMToken?.let {
      api.fcmToken = it
    }

    val handleFcmToken = { fcmToken: String ->
      api.registerOrRefreshFCM(fcmToken, {
        object : OperationCallback<PushNotificationsAPI.RegisterDeviceResult> {
          override fun onSuccess(result: PushNotificationsAPI.RegisterDeviceResult) {
            if (deviceStateStore.deviceId == null) {
              synchronized(deviceStateStore) {
                val previousLocalInterestSet = deviceStateStore.interestsSet
                deviceStateStore.interestsSet = result.initialInterestSet.toMutableSet()

                jobQueue.forEach({ job -> job() })

                if (!previousLocalInterestSet.equals(deviceStateStore.interestsSet)) {
                  api.setSubscriptions(result.deviceId, deviceStateStore.interestsSet, OperationCallbackNoArgs.noop)

                  onSubscriptionsChangedListener?.let{
                    it.onSubscriptionsChanged(deviceStateStore.interestsSet)
                  }
                }
              }

              jobQueue.clear()
              log.i("Successfully started PushNotifications")
            }

            deviceStateStore.deviceId = result.deviceId
            deviceStateStore.FCMToken = fcmToken
          }

          override fun onFailure(t: Throwable) {
            log.w("Failed to start PushNotifications", t)
          }
        }
      }())
    }

    FCMInstanceIDService.onRefreshToken = handleFcmToken
    FirebaseInstanceId.getInstance().token?.let(handleFcmToken)
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
      val deviceId = deviceStateStore.deviceId

      if (deviceId != null) {
        if (addInterestToStore(interest)) {
          api.subscribe(deviceId, interest, OperationCallbackNoArgs.noop)
        }
      } else {
        addInterestToStore(interest)
        jobQueue += fun(): Boolean = addInterestToStore(interest)
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
      val deviceId = deviceStateStore.deviceId

      if (deviceId != null) {
        if(removeInterestFromStore(interest)) {
          api.unsubscribe(deviceId, interest, OperationCallbackNoArgs.noop)
        }
      } else {
        removeInterestFromStore(interest)
        jobQueue += fun (): Boolean = removeInterestFromStore(interest)
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
      val deviceId = deviceStateStore.deviceId
      if (deviceId != null) {
        if (replaceAllInterestsInStore(interests)) {
          api.setSubscriptions(deviceId, interests, OperationCallbackNoArgs.noop)
        }
      } else {
        replaceAllInterestsInStore(interests)
        jobQueue += fun (): Boolean = replaceAllInterestsInStore(interests)
      }
    }
  }

  /**
   * @return the set of subscriptions that the device is currently subscribed to
   */
  fun getSubscriptions(): Set<String> {
    synchronized(deviceStateStore) {
      return deviceStateStore.interestsSet
    }
  }

  /**
   * Registers a listener for when subscriptions have changed
   * @param listener - the listener object
   */
  fun setOnSubscriptionsChangedListener(listener: SubscriptionsChangedListener) {
    onSubscriptionsChangedListener = listener
  }
}
