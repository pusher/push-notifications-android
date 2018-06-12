package com.pusher.pushnotifications

/**
 * The listener interface for when a interests change upon migrating from a 3rd-party service
 */
interface SubscriptionsChangedListener {
  /**
   * Called when interests change after migration
   *
   * @param interests a set of interests reconciled with existing interests from a 3rd-party service
   */
  fun onSubscriptionsChanged(interests: Set<String>)
}
