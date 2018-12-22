package com.pusher.pushnotifications.auth

interface TokenProvider {
  fun fetchToken(userId: String): String
}
