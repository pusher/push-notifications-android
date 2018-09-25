package com.pusher.pushnotifications;

import java.util.Set;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;

import com.pusher.pushnotifications.auth.TokenProvider;
import com.pusher.pushnotifications.fcm.MessagingService;

public class PushNotifications {
    static class PrototypeGetTokenTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String beamsToken = instance.getTokenProvider().fetchToken();
            PushNotifications.subscribeDeviceTo(beamsToken);
            return null;
        }
    }

    private static PushNotificationsInstance instance;

    /**
     * Starts the PushNotification client and synchronizes the FCM device token with
     * the Pusher services.
     * @param context the application context
     * @param instanceId the id of the instance
     * @return the Push Notifications instance which should be used if a non-singleton approach
     * is deemed better for your project.
     */
    public static PushNotificationsInstance start(
            Context context,
            String instanceId,
            TokenProvider tokenProvider
    ) {
        instance = new PushNotificationsInstance(context, instanceId, tokenProvider);
        instance.start();
        return instance;
    }

    public PushNotifications() {
        super();
    }

    /**
     * Starts the PushNotification client and synchronizes the FCM device token with
     * the Pusher services.
     * @param context the application context
     * @param instanceId the id of the instance
     * @return the Push Notifications instance which should be used if a non-singleton approach
     * is deemed better for your project.
     */
    public static PushNotificationsInstance start(
            Context context,
            String instanceId
    ) {
        instance = new PushNotificationsInstance(context, instanceId, null);
        instance.start();
        return instance;
    }

    /**
     * Associates the device with a user ID from your authentication system.
     * Can be used to publish to all devices owned by that user.
     * <i>Note: You must initialize the SDK with a TokenProvider before calling this method.</i>
     * Example:
     * <pre>{@code PushNotifications.setUserId("userid-1234");}</pre>
     * @param userId unique identifier of the user you want to associate with this device
     */
    public static void setUserId(String userId) {
        new PrototypeGetTokenTask().execute();
    }

    /**
     * Subscribes the device to an Interest. For example:
     * <pre>{@code PushNotifications.subscribe("hello");}</pre>
     * @deprecated in favor of subscribeDeviceTo
     * @param interest the name of the Interest
     */
    @Deprecated
    public static void subscribe(String interest) {
        PushNotifications.subscribeDeviceTo(interest);
    }

    /**
     * Subscribes the device to an Interest. For example:
     * <pre>{@code PushNotifications.subscribeDeviceTo("hello");}</pre>
     * @param interest the name of the Interest
     */
    public static void subscribeDeviceTo(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.subscribe(interest);
    }

    /**
     * Unsubscribes the device from the Interest. For example:
     * <pre>{@code PushNotifications.unsubscribe("hello");}</pre>
     * @deprecated in favor of unsubscribeDeviceFrom
     * @param interest the name of the Interest
     */
    @Deprecated
    public static void unsubscribe(String interest) {
        PushNotifications.unsubscribeDeviceFrom(interest);
    }

    /**
     * Unsubscribes the device from the Interest. For example:
     * <pre>{@code PushNotifications.unsubscribeDeviceFrom("hello");}</pre>
     * @param interest the name of the Interest
     */
    public static void unsubscribeDeviceFrom(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribe(interest);
    }

    /**
     * Unsubscribes the device from all the Interests. For example:
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
     * unsubscribed from, so this will replace the Interest set by the one provided.
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
     * @return the Interest subscriptions that the device is currently subscribed to
     */
    public static Set<String> getSubscriptions() {
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
     */
    public static void setOnSubscriptionsChangedListener(SubscriptionsChangedListener listener) {
        instance.setOnSubscriptionsChangedListener(listener);
    }
}
