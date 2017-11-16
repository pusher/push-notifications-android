package com.pusher.pushnotifications;

import android.content.Context;

import java.util.Set;

public class PushNotifications {
    private static PushNotificationsInstance instance;

    public static PushNotificationsInstance start(Context context, String instanceId) {
        instance = new PushNotificationsInstance(context, instanceId);
        instance.start();
        return instance;
    }

    public static void subscribe(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.subscribe(interest);
    }

    public static void unsubscribe(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribe(interest);
    }

    public static void unsubscribeAll(String interest) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.unsubscribe(interest);
    }

    public static void setSubscriptions(Set<String> interests) {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        instance.setSubscriptions(interests);
    }

    public static Set<String> getSubscriptions() {
        if (instance == null) {
            throw new IllegalStateException("PushNotifications.start must have been called before");
        }

        return instance.getSubscriptions();
    }
}
