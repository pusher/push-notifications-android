package com.pusher.pushnotifications.auth

/**
 * Describes how to acquire a Beams Token. This is used for User authentication.
 *
 * For example, synchronously do an HTTP request to your server via your usual
 * authentication method.
 */
interface TokenProvider {

  /**
   * Retrieves the Beams token to authenticate the given User with this device.
   *
   * If you failed to get a token from your server, throw an `Exception`.
   * You'll be able to handle it via a callback in the `setUserId` function.
   */
  fun fetchToken(userId: String): String
}
