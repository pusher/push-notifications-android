package com.pusher.pushnotifications.featureflags

enum class FeatureFlag {
  DELIVERY_TRACKING
}

/*
 * Tells the SDK which features should be enabled. May be extended in the future to get this
 * information from the server.
 */
object FeatureFlagManager {
  private val flags: Map<FeatureFlag, Boolean> = mapOf(
    FeatureFlag.DELIVERY_TRACKING to true
  )

  fun isEnabled(flag: FeatureFlag): Boolean {
    return flags[flag]!!
  }
}
