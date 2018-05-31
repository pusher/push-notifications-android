package com.pusher.pushnotifications.featureflags

import org.junit.Test

class FeatureFlagManagerTest {

  @Test
  fun `isEnabled never throws exceptions`() {
    FeatureFlag.values().forEach {
      FeatureFlagManager.isEnabled(it)
    }
    // no exceptions -> test pass
  }
}
