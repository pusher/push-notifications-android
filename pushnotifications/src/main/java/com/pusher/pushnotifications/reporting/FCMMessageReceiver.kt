package com.pusher.pushnotifications.reporting

import android.content.Context
import android.content.Intent
import android.support.v4.content.WakefulBroadcastReceiver
import android.util.Log
import com.firebase.jobdispatcher.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.reporting.api.ReportEvent
import com.pusher.pushnotifications.reporting.api.ReportEventType


data class PusherMetadata(
  val publishId: String
)

class FCMMessageReceiver : WakefulBroadcastReceiver() {
  private val gson = Gson()
  private val log = Logger.get(this::class)

  override fun onReceive(context: Context?, intent: Intent?) {
    Log.i("FCMMessageReceiver", intent.toString())

    intent?.getStringExtra("pusher")?.let { pusherDataJson ->
      try {
        val pusherData = gson.fromJson(pusherDataJson, PusherMetadata::class.java)
        log.i("Got a valid pusher message.")

        val reportEvent = ReportEvent(
          eventType = ReportEventType.Delivery,
          publishId = pusherData.publishId,
          timestamp = System.currentTimeMillis()
        )

        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
        val job = dispatcher.newJobBuilder()
          .setService(ReportingJobService::class.java)
          .setTag("pusherData.publishId="+pusherData.publishId)
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
