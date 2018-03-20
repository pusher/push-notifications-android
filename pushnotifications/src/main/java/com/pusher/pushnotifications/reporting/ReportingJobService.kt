package com.pusher.pushnotifications.reporting

import android.os.Bundle
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.reporting.api.ReportEvent
import com.pusher.pushnotifications.reporting.api.ReportEventType
import com.pusher.pushnotifications.reporting.api.ReportingAPI
import com.pusher.pushnotifications.reporting.api.UnrecoverableRuntimeException

data class PusherMetadata(
        val publishId: String,
        val clickAction: String?
)

class ReportingJobService: JobService() {
  companion object {
    private const val BUNDLE_EVENT_TYPE_KEY = "ReportEventType"
    private const val BUNDLE_DEVICE_ID_KEY = "DeviceId"
    private const val BUNDLE_PUBLISH_ID_KEY = "PublishId"
    private const val BUNDLE_TIMESTAMP_KEY = "Timestamp"

    fun toBundle(reportEvent: ReportEvent): Bundle {
      val b = Bundle()
      b.putString(BUNDLE_EVENT_TYPE_KEY, reportEvent.eventType.toString())
      b.putString(BUNDLE_DEVICE_ID_KEY, reportEvent.deviceId)
      b.putString(BUNDLE_PUBLISH_ID_KEY, reportEvent.publishId)
      b.putLong(BUNDLE_TIMESTAMP_KEY, reportEvent.timestampSecs)

      return b
    }

    fun fromBundle(bundle: Bundle): ReportEvent {
      return ReportEvent(
        eventType =  ReportEventType.valueOf(bundle.getString(BUNDLE_EVENT_TYPE_KEY)),
        deviceId = bundle.getString(BUNDLE_DEVICE_ID_KEY),
        publishId = bundle.getString(BUNDLE_PUBLISH_ID_KEY),
        timestampSecs = bundle.getLong(BUNDLE_TIMESTAMP_KEY)
      )
    }
  }

  private val log = Logger.get(this::class)

  override fun onStartJob(params: JobParameters?): Boolean {
    params?.let {
      val extras = it.extras
      if (extras != null) {
        val reportEvent = fromBundle(extras)

        val instanceId = PushNotificationsInstance.getInstanceId(this.applicationContext)
        if (instanceId != null) {
          ReportingAPI(instanceId).submit(
            reportEvent = reportEvent,
            operationCallback = object: OperationCallback {
              override fun onSuccess() {
                log.v("Successfully submitted report.")
                jobFinished(params, false)
              }

              override fun onFailure(t: Throwable) {
                log.w("Failed submitted report.", t)
                val shouldRetry = t !is UnrecoverableRuntimeException

                jobFinished(params, shouldRetry)
              }
            }
          )
        } else {
          log.w("Incorrect start of service: instance id is missing.")
          return false
        }
      } else {
        log.w("Incorrect start of service: extras bundle is missing.")
        return false
      }
    }

    return true // A background job was started
  }

  override fun onStopJob(params: JobParameters?): Boolean {
    return true // Answers the question: "Should this job be retried?"
  }
}
