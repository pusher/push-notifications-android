package com.pusher.pushnotifications.auth

interface TokenProvider {
    fun fetchToken(): String?
}
