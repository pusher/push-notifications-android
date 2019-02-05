package com.example.pushnotificationsintegrationtests;

import com.pusher.pushnotifications.auth.TokenProvider;

// Used to test Kotlin's API being called from a Java environment
public class JavaNull {

    protected static TokenProvider getNullTokenProvider() {
        return null;
    }
}
