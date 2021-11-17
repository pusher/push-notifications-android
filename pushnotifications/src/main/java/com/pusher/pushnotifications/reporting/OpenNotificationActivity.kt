package com.pusher.pushnotifications.reporting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pusher.pushnotifications.internal.InstanceDeviceStateStore
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.reporting.api.OpenEvent

/*
 * This activity will be opened when a user taps a notification sent by the Pusher push notifications
 * service. It reports back to Pusher that the notification has been opened and then
 * opens the customer application activity that would have been opened had it not been
 * intercepted.
 */
class OpenNotificationActivity: Activity() {
    private val log = Logger.get(this::class)

    private fun startIntent(bundle: Bundle?, clickAction: String? = null) {
        val i: Intent
        if (clickAction != null) {
            i = Intent()
            i.action = clickAction
        } else {
            i = packageManager.getLaunchIntentForPackage(packageName)!!
        }

        i.replaceExtras(bundle)

        // We need to clear the activity stack so that this activity doesn't show up when customers
        // are debugging.
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        try {
            applicationContext.startActivity(i)
        } catch (_: RuntimeException) {
            log.e("Failed to start activity using clickAction $clickAction")
            // The default Firebase behaviour in this situation is to kill the app, so we should
            // do the same.
            finishAffinity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.getStringExtra("pusher")?.let { pusherDataJson ->
            try {
                val gson = Gson()
                val pusherData = gson.fromJson(pusherDataJson, PusherMetadata::class.java)
                log.i("Got a valid pusher message.")

                val deviceStateStore = InstanceDeviceStateStore(applicationContext, pusherData.instanceId)
                val deviceId = deviceStateStore.deviceId
                if (deviceId == null) {
                    log.e("Failed to get device ID (device ID not stored) - Skipping open tracking.")
                    startIntent(intent.extras)
                    return
                }

                val reportEvent = OpenEvent(
                   instanceId = pusherData.instanceId,
                   publishId = pusherData.publishId,
                   deviceId = deviceId,
                   userId = deviceStateStore.userId,
                   timestampSecs = System.currentTimeMillis() / 1000
                )

                val reportWorker = OneTimeWorkRequest.Builder(ReportingWorker::class.java)
                        .setConstraints(Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build())
                        .setInputData(ReportingWorker.toInputData(reportEvent))
                        .build()

                val workManagerInstance = WorkManager.getInstance(applicationContext)
                workManagerInstance.enqueueUniqueWork("pusher.open.publishId=${pusherData.publishId}",
                        ExistingWorkPolicy.KEEP,
                        reportWorker)

                startIntent(intent.extras, pusherData.clickAction)
            } catch (_: JsonSyntaxException) {
                // TODO: Add client-side reporting

                // This means that something went horribly wrong. Just starting the main
                // activity seems like a decent best-effort response.
                startIntent(intent.extras)
            }
            return
        }

        // Somehow this activity was started without a pusher payload. We should start the main
        // activity as a best-effort response.
        startIntent(intent.extras)
    }
}
