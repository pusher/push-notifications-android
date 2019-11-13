package com.pusher.pushnotifications.reporting

import android.content.Context
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver
import com.firebase.jobdispatcher.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pusher.pushnotifications.featureflags.FeatureFlag
import com.pusher.pushnotifications.featureflags.FeatureFlagManager
import com.pusher.pushnotifications.internal.AppActivityLifecycleCallbacks
import com.pusher.pushnotifications.internal.DeviceStateStore
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.reporting.api.DeliveryEvent

class FCMMessageReceiver : WakefulBroadcastReceiver() {
  private val gson = Gson()
  private val log = Logger.get(this::class)

  override fun onReceive(context: Context?, intent: Intent?) {
    if (!FeatureFlagManager.isEnabled(FeatureFlag.DELIVERY_TRACKING)) {
      log.i("Delivery tracking flag is disabled. Skipping.")
      return
    }

    intent?.getStringExtra("pusher")?.let { pusherDataJson ->
      try {
        val pusherData = gson.fromJson(pusherDataJson, PusherMetadata::class.java)
        log.i("Got a valid pusher message.")

        if (context == null) {
          log.w("Failed to get device ID (no context) - Skipping delivery tracking.")
          return
        }

        val deviceStateStore = InstanceDeviceStateStore(context, pusherData.instanceId)

        val deviceId = deviceStateStore.deviceId
        if (deviceId == null) {
          log.w("Failed to get device ID (device ID not stored) - Skipping delivery tracking.")
          return
        }

        val reportEvent = DeliveryEvent(
          instanceId = pusherData.instanceId,
          publishId = pusherData.publishId,
          deviceId = deviceId,
          userId = deviceStateStore.userId,
          timestampSecs = Math.round(System.currentTimeMillis() / 1000.0),
          appInBackground = AppActivityLifecycleCallbacks.appInBackground(),
          hasDisplayableContent = pusherData.hasDisplayableContent,
          hasData = pusherData.hasData
        )

        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        val job = dispatcher.newJobBuilder()
          .setService(ReportingJobService::class.java)
          .setTag("pusher.delivered.publishId=${pusherData.publishId}")
          .setConstraints(Constraint.ON_ANY_NETWORK)
          .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
          .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
          .setExtras(ReportingJobService.toBundle(reportEvent))
          .build()

        dispatcher.mustSchedule(job)
      } catch (_: JsonSyntaxException) {
        // TODO: Add client-side reporting
      }
    }
  }
}
