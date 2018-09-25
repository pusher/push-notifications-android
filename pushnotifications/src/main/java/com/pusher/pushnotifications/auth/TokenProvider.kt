package com.pusher.pushnotifications.auth

import android.os.AsyncTask


interface TokenProvider {
    fun fetchToken(): String
    // TODO: See if we want this
    // fun clearToken(token: String? = null)
}