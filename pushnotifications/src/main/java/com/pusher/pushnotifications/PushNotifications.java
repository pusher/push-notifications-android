package com.pusher.pushnotifications;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;

import com.pusher.pushnotifications.auth.TokenProvider;
import com.pusher.pushnotifications.fcm.MessagingService;

/**
 * The Pusher Beams static client.
 */
public class PushNotifications {
    private static PushNotificationsInstance instance;
    protected static Map<String, TokenProvider> tokenProvider = new HashMap<>();

    /**
     * Starts the PushNotification client and synchronizes the FCM device token with
     * the Pusher services.
     * @param context the application context
     * @param instanceId the id of the instance
     * @return the Push Notifications instance which should be used if a non-singleton approach
     * is deemed better for your project.
     */
    public static PushNotificationsInstance start(Context context, String instanceId) {
        if (instance == null) {
            instance = new PushNotificationsInstance(context, instanceId);
        } else if (!instance.getInstanceId().equals(instanceId)) {
            String errorMessage =
                    "PushNotifications.start has been called before with a different instanceId! (before: "
                            + instance.getInstanceId() + ", now: " + instanceId + ").\n"
                            + "If you want to use multiple instanceIds use the `PushNotificationsInstance` class directly "
                            + "e.g. `val pushNotifications1 = PushNotificationsInstance(context, instanceId)`\n"
                            + "`pushNotifications1.start()`";
            throw new IllegalStateException(errorMessage);
        }

        instance.start();
        return instance;
    }

    /**
     * Subscribes the device to an Interest. For example:
     * <pre>{@code PushNotifications.subscribe("hello");}</pre>
     * @param interest the name of the Interest
     * @deprecated use addDeviceInterest instead
     */
    @Deprecated
    public static void subscribe(String interest) {
        addDeviceInterest(interest);
    }

    /**
     * Subscribes the device to an Interest. For example:
     * <pre>{@code PushNotifications.addDeviceInterest("hello");}</pre>
     * @param interest the name of the Interest
     */
    public static void addDeviceInterest(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.subscribe(interest);
    }

    /**
     * Unsubscribes the device from the Interest. For example:
     * <pre>{@code PushNotifications.unsubscribe("hello");}</pre>
     * @param interest the name of the Interest
     * @deprecated use removeDeviceInterest instead
     */
    @Deprecated
    public static void unsubscribe(String interest) {
        removeDeviceInterest(interest);
    }

    /**
     * Unsubscribes the device from the Interest. For example:
     * <pre>{@code PushNotifications.removeDeviceInterest("hello");}</pre>
     * @param interest the name of the Interest
     */
    public static void removeDeviceInterest(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribe(interest);
    }

    /**
     * Unsubscribes the device from all the Interests. For example:
     * <pre>{@code PushNotifications.unsubscribeAll();}</pre>
     * @deprecated use clearDeviceInterests instead
     */
    @Deprecated
    public static void unsubscribeAll() {
        clearDeviceInterests();
    }

    /**
     * Unsubscribes the device from all the Interests. For example:
     * <pre>{@code PushNotifications.unsubscribeAll();}</pre>
     */
    public static void clearDeviceInterests() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribeAll();
    }

    /**
     * Sets the subscriptions state for the device. Any interests not in the set will be
     * unsubscribed from, so this will replace the Interest set by the one provided.
     * <br>
     * For example:
     * <pre>{@code PushNotifications.setSubscriptions(Arrays.asList("hello", "donuts").toSet());}</pre>
     * @param interests the new set of interests
     * @deprecated use setDeviceInterests instead
     */
    @Deprecated
    public static void setSubscriptions(Set<String> interests) {
        setDeviceInterests(interests);
    }

    /**
     * Sets the subscriptions state for the device. Any interests not in the set will be
     * unsubscribed from, so this will replace the Interest set by the one provided.
     * <br>
     * For example:
     * <pre>{@code PushNotifications.setSubscriptions(Arrays.asList("hello", "donuts").toSet());}</pre>
     * @param interests the new set of interests
     */
    public static void setDeviceInterests(Set<String> interests) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.setSubscriptions(interests);
    }

    /**
     * @return the Interest subscriptions that the device is currently subscribed to
     * @deprecated use getDeviceInterests instead
     */
    @Deprecated
    public static Set<String> getSubscriptions() {
        return getDeviceInterests();
    }

    /**
     * @return the Interest subscriptions that the device is currently subscribed to
     */
    public static Set<String> getDeviceInterests() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        return instance.getSubscriptions();
    }

    /**
     * Configures the listener that handles a remote message only when this activity is in the
     * foreground.
     *
     * You can use this method to update your UI. This should be called from the `Activity.onResume` method.
     *
     * If you intend to handle a remote message in all circumstances, read the service docs:
     * https://docs.pusher.com/push-notifications/reference/android#handle-incoming-notifications
     *
     * @param messageReceivedListener the listener that handles a remote message
     */
    public static void setOnMessageReceivedListenerForVisibleActivity(Activity activity, PushNotificationReceivedListener messageReceivedListener) {
        MessagingService.setOnMessageReceivedListenerForVisibleActivity(activity, messageReceivedListener);
    }

    /**
     * Configures the listener that handles a change the device's Interest subscriptions
     *
     * You can use this method to update your UI.
     *
     * @param listener the listener to handle Interest subscription change
     * @deprecated use setOnDeviceInterestsChangedListener instead
     */
    @Deprecated
    public static void setOnSubscriptionsChangedListener(SubscriptionsChangedListener listener) {
        setOnDeviceInterestsChangedListener(listener);
    }

    /**
     * Configures the listener that handles a change the device's Interest subscriptions
     *
     * You can use this method to update your UI.
     *
     * @param listener the listener to handle Interest subscription change
     */
    public static void setOnDeviceInterestsChangedListener(SubscriptionsChangedListener listener) {
        instance.setOnSubscriptionsChangedListener(listener);
    }

    /**
     * Sets the user id that is associated with this device.
     * <i>Note: This method can only be called after start. Once a user id has been set for the device
     * it cannot be changed until stop is called.</i>
     * <br>
     * For example:
     * <pre>{@code pushNotifications.setUserId("bob");}</pre>
     * @param userId the id of the user you would like to associate with the device
     */
    public static void setUserId(String userId, TokenProvider tokenProvider) {
        setUserId(userId, tokenProvider, new NoopBeamsCallback<Void, PusherCallbackError>());
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
    public static void setUserId(String userId, TokenProvider tokenProvider, BeamsCallback<Void, PusherCallbackError> callback) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.setUserId(userId, tokenProvider, callback);
    }

    /**
     * Stops the SDK by deleting all state (both locally and remotely).
     * Calling this will mean the device will cease to receive push notifications.
     *
     * `Start` must be called if you want to use the SDK again.
     */
    public static void stop() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.stop();
    }

    /**
     * Clears all the state in the SDK, leaving it in a empty started state.
     *
     * You should call this method when your user logs out of the application.
     */
    public static void clearAllState() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.clearAllState();
    }
}
