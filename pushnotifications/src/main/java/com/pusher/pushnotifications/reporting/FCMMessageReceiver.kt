package com.pusher.pushnotifications.reporting

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pusher.pushnotifications.featureflags.FeatureFlag
import com.pusher.pushnotifications.featureflags.FeatureFlagManager
import com.pusher.pushnotifications.internal.AppActivityLifecycleCallbacks
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.reporting.api.DeliveryEvent

class FCMMessageReceiver : androidx.legacy.content.WakefulBroadcastReceiver() {
  private val gson = Gson()
  private val log = Logger.get(this::class)

  override fun onReceive(context: Context?, intent: Intent?) {
    if (!FeatureFlagManager.isEnabled(FeatureFlag.DELIVERY_TRACKING)) {
      log.i("Delivery tracking flag is disabled. Skipping.")
      return
    }

    intent?.getStringExtra("pusher")?.let { pusherDataJson ->
        val pusherData = try {
          gson.fromJson(pusherDataJson, PusherMetadata::class.java)
      } catch (_: JsonSyntaxException) {
          // TODO: Add client-side reporting
            log.i("Got an invalid pusher message.")
            return
      }
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

        val reportWorker = OneTimeWorkRequest.Builder(ReportingWorker::class.java)
                .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setInputData(ReportingWorker.toInputData(reportEvent))
                .build()

        val workManagerInstance = WorkManager.getInstance(context)
        workManagerInstance.enqueueUniqueWork("pusher.delivered.publishId=${pusherData.publishId}",
                ExistingWorkPolicy.KEEP,
                reportWorker)
    }
  }
}
