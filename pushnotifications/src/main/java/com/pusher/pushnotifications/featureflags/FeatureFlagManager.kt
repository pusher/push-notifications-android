package com.pusher.pushnotifications.featureflags

/*
 * Tells the SDK which features should be enabled. May be extended in the future to get this
 * information from the server.
 */
enum class FeatureFlag {
    DELIVERY_TRACKING
}

object FeatureFlagManager {
    private val flags: Map<FeatureFlag, Boolean> = mapOf(
        FeatureFlag.DELIVERY_TRACKING to false
    )

    fun isEnabled(flag: FeatureFlag): Boolean {
        return flags[flag]!!
    }
}
