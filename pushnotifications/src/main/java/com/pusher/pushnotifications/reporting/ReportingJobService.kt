package com.pusher.pushnotifications.reporting

import android.os.Bundle
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.gson.annotations.SerializedName
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.api.OperationCallback
import com.pusher.pushnotifications.reporting.api.ReportEvent
import com.pusher.pushnotifications.reporting.api.ReportEventType
import com.pusher.pushnotifications.reporting.api.ReportingAPI
import com.pusher.pushnotifications.reporting.api.UnrecoverableRuntimeException

data class PusherMetadata(
  val publishId: String,
  val clickAction: String?,
  @SerializedName("hasDisplayableContent") private val _hasDisplayableContent: Boolean?,
  @SerializedName("hasData") private val _hasData: Boolean?
) {
  val hasDisplayableContent: Boolean
    get() {
      if (_hasDisplayableContent == null) {
        return false
      } else {
        return _hasDisplayableContent
      }
    }
  val hasData: Boolean
    get() {
      if (_hasData == null) {
        return false
      } else {
        return _hasData
      }
    }
}

class ReportingJobService: JobService() {
  companion object {
    private const val BUNDLE_EVENT_TYPE_KEY = "ReportEventType"
    private const val BUNDLE_DEVICE_ID_KEY = "DeviceId"
    private const val BUNDLE_PUBLISH_ID_KEY = "PublishId"
    private const val BUNDLE_TIMESTAMP_KEY = "Timestamp"
    private const val BUNDLE_APP_IN_BACKGROUND_KEY = "AppInBackground"
    private const val BUNDLE_HAS_DISPLAYABLE_CONTENT_KEY = "HasDisplayableContent"
    private const val BUNDLE_HAS_DATA_KEY = "HasData"

    fun toBundle(reportEvent: ReportEvent): Bundle {
      val b = Bundle()
      when (reportEvent) {
        is ReportEvent.DeliveryEvent -> {
          b.putString(BUNDLE_EVENT_TYPE_KEY, reportEvent.event.toString())
          b.putString(BUNDLE_DEVICE_ID_KEY, reportEvent.deviceId)
          b.putString(BUNDLE_PUBLISH_ID_KEY, reportEvent.publishId)
          b.putLong(BUNDLE_TIMESTAMP_KEY, reportEvent.timestampSecs)
          b.putBoolean(BUNDLE_APP_IN_BACKGROUND_KEY, reportEvent.appInBackground)
          b.putBoolean(BUNDLE_HAS_DISPLAYABLE_CONTENT_KEY, reportEvent.hasDisplayableContent)
          b.putBoolean(BUNDLE_HAS_DATA_KEY, reportEvent.hasData)
        }

        is ReportEvent.OpenEvent -> {
          b.putString(BUNDLE_EVENT_TYPE_KEY, reportEvent.event.toString())
          b.putString(BUNDLE_DEVICE_ID_KEY, reportEvent.deviceId)
          b.putString(BUNDLE_PUBLISH_ID_KEY, reportEvent.publishId)
          b.putLong(BUNDLE_TIMESTAMP_KEY, reportEvent.timestampSecs)
        }
      }

      return b
    }

    fun fromBundle(bundle: Bundle): ReportEvent {
      val event = ReportEventType.valueOf(bundle.getString(BUNDLE_EVENT_TYPE_KEY))
      when (event) {
        ReportEventType.Delivery -> {
          return ReportEvent.DeliveryEvent(
            event = event,
            deviceId = bundle.getString(BUNDLE_DEVICE_ID_KEY),
            publishId = bundle.getString(BUNDLE_PUBLISH_ID_KEY),
            timestampSecs = bundle.getLong(BUNDLE_TIMESTAMP_KEY),
            appInBackground = bundle.getBoolean(BUNDLE_APP_IN_BACKGROUND_KEY),
            hasDisplayableContent = bundle.getBoolean(BUNDLE_HAS_DISPLAYABLE_CONTENT_KEY),
            hasData = bundle.getBoolean(BUNDLE_HAS_DATA_KEY)
          )
        }
        ReportEventType.Open -> {
          return ReportEvent.OpenEvent(
            event = event,
            deviceId = bundle.getString(BUNDLE_DEVICE_ID_KEY),
            publishId = bundle.getString(BUNDLE_PUBLISH_ID_KEY),
            timestampSecs = bundle.getLong(BUNDLE_TIMESTAMP_KEY)
          )
        }
      }
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
