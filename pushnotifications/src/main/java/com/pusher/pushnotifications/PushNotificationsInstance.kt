package com.pusher.pushnotifications

import java.util.regex.Pattern
import android.content.Context
import android.os.*
import com.google.firebase.iid.FirebaseInstanceId
import com.pusher.pushnotifications.api.DeviceMetadata
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.fcm.MessagingService
import com.pusher.pushnotifications.internal.*
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.validation.Validations

/**
 * Thrown when the device is re-registered to a different instance id. If you wish to register a
 * device to a different instance you will need to reinstall the application.
 *
 * @param message Error message to be shown
 */
class PusherAlreadyRegisteredException(message: String) : RuntimeException(message)

/**
 * Thrown when the device is re-registered to a different user id.
 */
class PusherAlreadyRegisteredAnotherUserIdException(message: String) : IllegalStateException(message)

/**
 * Returned by com.pusher.pushnotifications.Callback when an async operation fails.
 *
 * @param message Error message to be shown
 * @param cause Throwable that cause the async operation to fail (optional)
 */
data class PusherCallbackError(val message: String, val cause: Throwable?)

internal sealed class ServerSyncEvent
internal data class InterestsChangedEvent(val interests: Set<String>): ServerSyncEvent()
internal data class UserIdSet(val userId: String, val pusherCallbackError: PusherCallbackError?): ServerSyncEvent()

internal class ServerSyncEventHandler private constructor(looper: Looper): Handler(looper) {
  var onSubscriptionsChangedListener: SubscriptionsChangedListener? = null
  var userIdToCallbacks: MutableMap<String, MutableList<com.pusher.pushnotifications.Callback<Void, PusherCallbackError>>> = mutableMapOf()

  fun addUserIdCallback(userId: String, callback: com.pusher.pushnotifications.Callback<Void, PusherCallbackError>) {
    synchronized(userIdToCallbacks) {
      val callbacks = userIdToCallbacks.getOrPut(userId) { mutableListOf() }

      callbacks.add(callback)
    }
  }

  override fun handleMessage(msg: Message) {
    val event = msg.obj

    when (event) {
      is InterestsChangedEvent -> {
        onSubscriptionsChangedListener?.onSubscriptionsChanged(event.interests)
      }
      is UserIdSet -> {
        synchronized(userIdToCallbacks) {
          userIdToCallbacks[event.userId]?.let { callbacks ->
            if (event.pusherCallbackError == null) {
              callbacks.forEach { it.onSuccess() }
            } else {
              callbacks.forEach { it.onFailure(event.pusherCallbackError) }
            }
          }

          userIdToCallbacks.remove(event.userId)
        }
      }
    }
  }

  internal companion object {
    private val serverSyncEventHandlers = mutableMapOf<String, ServerSyncEventHandler>()
    internal fun obtain(instanceId: String, looper: Looper): ServerSyncEventHandler {
      return synchronized(serverSyncEventHandlers) {
        serverSyncEventHandlers.getOrPut(instanceId) {
          ServerSyncEventHandler(looper)
        }
      }
    }
  }
}

/**
 * Interacts with the Pusher service to subscribe and unsubscribe from interests.
 *
 * @param context the application context
 * @param instanceId the id of the instance
 */
class PushNotificationsInstance @JvmOverloads constructor(
// TODO: throw exception if tokenProvider is null but user id is set
    context: Context,
    instanceId: String
) {
  private val log = Logger.get(this::class)

  private val sdkConfig = SDKConfiguration(context)
  private val deviceStateStore = DeviceStateStore.obtain(instanceId, context)
  private val oldSDKDeviceStateStore = OldSDKDeviceStateStore(context)

  private val serverSyncEventHandler = ServerSyncEventHandler.obtain(instanceId, context.mainLooper)

  private val serverSyncHandler = {
    ServerSyncHandler.obtain(
        instanceId = instanceId,
        api = PushNotificationsAPI(instanceId, sdkConfig.overrideHostURL),
        deviceStateStore = deviceStateStore,
        secureFileDir = context.filesDir,
        handleServerSyncEvent = { msg ->
          serverSyncEventHandler.sendMessage(Message.obtain().apply { obj = msg })
        },
        getTokenProvider = {
          PushNotifications.tokenProvider
        }
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

  private var startHasBeenCalledThisSession = false
  /**
   * Starts the PushNotification client and synchronizes the FCM device token with
   * the Pusher services.
   */
  fun start(): PushNotificationsInstance {
    startHasBeenCalledThisSession = true
    val handleFcmToken = { fcmToken: String ->
      synchronized(deviceStateStore) {
        if (deviceStateStore.startJobHasBeenEnqueued) {
          serverSyncHandler.sendMessage(ServerSyncHandler.refreshToken(fcmToken))
        } else {
          serverSyncHandler.sendMessage(ServerSyncHandler.start(fcmToken, oldSDKDeviceStateStore.clientIds()))
          deviceStateStore.startJobHasBeenEnqueued = true
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
        serverSyncEventHandler.onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
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
        serverSyncEventHandler.onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
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
        serverSyncEventHandler.onSubscriptionsChangedListener?.onSubscriptionsChanged(deviceStateStore.interests)
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
    serverSyncEventHandler.onSubscriptionsChangedListener = listener
  }

  internal fun onApplicationStarted() {
    val deviceMetadata = DeviceMetadata(BuildConfig.VERSION_NAME, Build.VERSION.RELEASE)

    serverSyncHandler.sendMessage(ServerSyncHandler.applicationStart(deviceMetadata))
  }

  /**
   * Sets the user id that is associated with this device.
   * <i>Note: This method can only be called after start. Once a user id has been set for the device
   * it cannot be changed until stop is called.</i>
   * <br>
   * For example:
   * <pre>{@code pushNotifications.setUserId("bob");}</pre>
   * @param userId the id of the user you would like to associate with the device
   * @param callback callback used to indicate whether the user association process has succeeded
   */
  @JvmOverloads
  fun setUserId(userId: String, callback: Callback<Void, PusherCallbackError> = NoopCallback()) {
    if (PushNotifications.tokenProvider == null) {
      throw IllegalStateException("Token provider missing, please call `PushNotifications.setTokenProvider`")
    }
    if (!startHasBeenCalledThisSession) {
      throw IllegalStateException("Start method must be called before setUserId")
    }

    synchronized(deviceStateStore) {
      if (
          deviceStateStore.setUserIdHasBeenCalledWith != null &&
          deviceStateStore.setUserIdHasBeenCalledWith != userId
      ) {
        throw PusherAlreadyRegisteredAnotherUserIdException(
            "This device has already been registered to another user id.")
      }

      deviceStateStore.setUserIdHasBeenCalledWith = userId
    }

    serverSyncEventHandler.addUserIdCallback(userId, callback)
    serverSyncHandler.sendMessage(ServerSyncHandler.setUserId(userId))
  }

  fun stop() {
    synchronized(deviceStateStore) {
      val hadAnyInterests = deviceStateStore.interests.isNotEmpty()

      deviceStateStore.interests = mutableSetOf()
      deviceStateStore.startJobHasBeenEnqueued = false
      deviceStateStore.setUserIdHasBeenCalledWith = null
      startHasBeenCalledThisSession = false
      serverSyncHandler.sendMessage(ServerSyncHandler.stop())

      if (hadAnyInterests) {
        serverSyncEventHandler.onSubscriptionsChangedListener?.onSubscriptionsChanged(emptySet())
      }
    }
  }

  fun clearAllState() {
    stop()
    start()
  }
}
