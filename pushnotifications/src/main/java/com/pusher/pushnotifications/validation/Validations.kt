package com.pusher.pushnotifications.validation

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import com.pusher.pushnotifications.logging.Logger

object Validations {
  private val log = Logger.get(this::class)

  internal fun validateApplicationIcon(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    try {
      val possibleBitmap = AdaptiveIconDrawable.createFromStream(
        context.resources.openRawResource(context.applicationInfo.icon),
        "applicationInfo.icon")

      if (possibleBitmap != null) {
        return
      }
    } catch (_: Exception) {
    }
    log.e("Your application icon is not compatible with Android Oreo Adaptive Icons.\n" +
      "This means that the application will likely crash the System UI when receiving a Push Notification.\n" +
      "To learn more about this issue check: https://issuetracker.google.com/issues/68716460")
    return
  }
}
