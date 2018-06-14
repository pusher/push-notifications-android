package com.pusher.pushnotifications.internal

import android.content.Context
import android.preference.PreferenceManager

/**
 * Temporary helper to extract out information stored by the old SDK
 */
class OldSDKDeviceStateStore(context: Context) {
  private val clientKeyPrefix = "__pusher__client__key__"
  private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

  /**
   * Extracts out all the client ids stored by the old SDK
   *
   * There can be multiple ones as the same app could have used several
   * configurations during its use.
   */
  fun clientIds(): List<String> =
      preferences
          .all
          .filterKeys { it.startsWith(clientKeyPrefix) }
          .values
          .map { it.toString() }
          .toList()
}
