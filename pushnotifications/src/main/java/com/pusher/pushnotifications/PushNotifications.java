package com.pusher.pushnotifications;

import android.content.Context;

import com.pusher.pushnotifications.fcm.FCMMessagingService;

import java.util.Set;

public class PushNotifications {
    private static PushNotificationsInstance instance;

    /**
     * Starts the PushNotification client and synchronizes the FCM device token with
     * the Pusher services.
     * @param context the application context
     * @param instanceId the id of the instance
     * @return the Push Notifications instance which should be used if a non-singleton approach
     * is deemed better for your project.
     */
    public static PushNotificationsInstance start(Context context, String instanceId) {
        instance = new PushNotificationsInstance(context, instanceId);
        instance.start();
        return instance;
    }

    /**
     * Subscribes the device to an interest. For example:
     * <pre>{@code PushNotifications.subscribe("hello");}</pre>
     * @param interest the name of the interest
     */
    public static void subscribe(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.subscribe(interest);
    }

    /**
     * Unsubscribes the device from the interest. For example:
     * <pre>{@code PushNotifications.unsubscribe("hello");}</pre>
     * @param interest the name of the interest
     */
    public static void unsubscribe(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribe(interest);
    }

    /**
     * Unsubscribes the device from all the interests. For example:
     * <pre>{@code PushNotifications.unsubscribeAll();}</pre>
     */
    public static void unsubscribeAll() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribeAll();
    }

    /**
     * Sets the subscriptions state for the device. Any interests not in the set will be
     * unsubscribed from, so this will replace the interest set by the one provided.
     * <br>
     * For example:
     * <pre>{@code PushNotifications.setSubscriptions(Arrays.asList("hello", "donuts").toSet());}</pre>
     * @param interests the new set of interests
     */
    public static void setSubscriptions(Set<String> interests) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.setSubscriptions(interests);
    }

    /**
     * @return the set of subscriptions that the device is currently subscribed to
     */
    public static Set<String> getSubscriptions() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        return instance.getSubscriptions();
    }

    /**
     * Configures the listener that handles a remote message when the app is in the foreground.
     *
     * @param messageReceivedListener the listener that handles a remote message
     */
    public static void setOnMessageReceivedListener(PushNotificationReceivedListener messageReceivedListener) {
        FCMMessagingService.setOnMessageReceivedListener(messageReceivedListener);
    }
}
