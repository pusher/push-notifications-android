package com.pusher.pushnotifications.featureflags

import org.junit.Test

class FeatureFlagManagerTest {

  @Test
  fun isEnabled_NeverThrowsExceptions() {
    FeatureFlag.values().forEach {
      FeatureFlagManager.isEnabled(it)
    }
    // no exceptions -> test pass
  }
}
