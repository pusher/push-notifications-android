package com.pusher.pushnotifications.reporting

import android.os.Bundle
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.gson.annotations.SerializedName
import com.pusher.pushnotifications.logging.Logger
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.api.OperationCallbackNoArgs
import com.pusher.pushnotifications.reporting.api.*

data class PusherMetadata(
  val instanceId: String,
  val publishId: String,
  val clickAction: String?,
  @SerializedName("hasDisplayableContent") private val _hasDisplayableContent: Boolean?,
  @SerializedName("hasData") private val _hasData: Boolean?
) {
  val hasDisplayableContent: Boolean
    get() = _hasDisplayableContent ?: false

  val hasData: Boolean
    get() = _hasData ?: false
}

class ReportingJobService: JobService() {
  companion object {
    private const val BUNDLE_EVENT_TYPE_KEY = "ReportEventType"
    private const val BUNDLE_INSTANCE_ID_KEY = "InstanceId"
    private const val BUNDLE_DEVICE_ID_KEY = "DeviceId"
    private const val BUNDLE_USER_ID_KEY = "UserId"
    private const val BUNDLE_PUBLISH_ID_KEY = "PublishId"
    private const val BUNDLE_TIMESTAMP_KEY = "Timestamp"
    private const val BUNDLE_APP_IN_BACKGROUND_KEY = "AppInBackground"
    private const val BUNDLE_HAS_DISPLAYABLE_CONTENT_KEY = "HasDisplayableContent"
    private const val BUNDLE_HAS_DATA_KEY = "HasData"

    fun toBundle(reportEvent: ReportEvent): Bundle {
      val b = Bundle()
      when (reportEvent) {
        is DeliveryEvent -> {
          b.putString(BUNDLE_EVENT_TYPE_KEY, reportEvent.event.toString())
          b.putString(BUNDLE_INSTANCE_ID_KEY, reportEvent.instanceId)
          b.putString(BUNDLE_DEVICE_ID_KEY, reportEvent.deviceId)
          b.putString(BUNDLE_USER_ID_KEY, reportEvent.userId)
          b.putString(BUNDLE_PUBLISH_ID_KEY, reportEvent.publishId)
          b.putLong(BUNDLE_TIMESTAMP_KEY, reportEvent.timestampSecs)
          b.putBoolean(BUNDLE_APP_IN_BACKGROUND_KEY, reportEvent.appInBackground!!)
          b.putBoolean(BUNDLE_HAS_DISPLAYABLE_CONTENT_KEY, reportEvent.hasDisplayableContent!!)
          b.putBoolean(BUNDLE_HAS_DATA_KEY, reportEvent.hasData!!)
        }

        is OpenEvent -> {
          b.putString(BUNDLE_EVENT_TYPE_KEY, reportEvent.event.toString())
          b.putString(BUNDLE_INSTANCE_ID_KEY, reportEvent.instanceId)
          b.putString(BUNDLE_DEVICE_ID_KEY, reportEvent.deviceId)
          b.putString(BUNDLE_USER_ID_KEY, reportEvent.userId)
          b.putString(BUNDLE_PUBLISH_ID_KEY, reportEvent.publishId)
          b.putLong(BUNDLE_TIMESTAMP_KEY, reportEvent.timestampSecs)
        }
      }

      return b
    }

    fun fromBundle(bundle: Bundle): ReportEvent? {
      val eventType = bundle.getString(BUNDLE_EVENT_TYPE_KEY) ?: return null

      when (ReportEventType.valueOf(eventType)) {
        ReportEventType.Delivery -> {
          return DeliveryEvent(
            instanceId = bundle.getString(BUNDLE_INSTANCE_ID_KEY) ?: return null,
            deviceId = bundle.getString(BUNDLE_DEVICE_ID_KEY) ?: return null,
            userId = bundle.getString(BUNDLE_USER_ID_KEY),
            publishId = bundle.getString(BUNDLE_PUBLISH_ID_KEY) ?: return null,
            timestampSecs = bundle.getLong(BUNDLE_TIMESTAMP_KEY),
            appInBackground = bundle.getBoolean(BUNDLE_APP_IN_BACKGROUND_KEY),
            hasDisplayableContent = bundle.getBoolean(BUNDLE_HAS_DISPLAYABLE_CONTENT_KEY),
            hasData = bundle.getBoolean(BUNDLE_HAS_DATA_KEY)
          )
        }
        ReportEventType.Open -> {
          return OpenEvent(
            instanceId = bundle.getString(BUNDLE_INSTANCE_ID_KEY) ?: return null,
            deviceId = bundle.getString(BUNDLE_DEVICE_ID_KEY) ?: return null,
            userId = bundle.getString(BUNDLE_USER_ID_KEY),
            publishId = bundle.getString(BUNDLE_PUBLISH_ID_KEY) ?: return null,
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
        if (reportEvent != null) {
          reportEvent.deviceId

          ReportingAPI(reportEvent.instanceId).submit(
              reportEvent = reportEvent,
              operationCallback = object : OperationCallbackNoArgs {
                override fun onSuccess() {
                  log.i("Successfully submitted report.")
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
          log.w("Incorrect start of service: extras bundle is partially corrupted.")
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
