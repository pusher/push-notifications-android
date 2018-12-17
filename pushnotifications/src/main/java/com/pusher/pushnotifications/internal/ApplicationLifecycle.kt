package com.pusher.pushnotifications.internal

import java.lang.ref.WeakReference
import java.math.BigInteger
import java.security.MessageDigest
import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.pusher.pushnotifications.BuildConfig
import com.pusher.pushnotifications.PushNotificationsInstance
import com.pusher.pushnotifications.api.DeviceMetadata
import com.pusher.pushnotifications.api.OperationCallbackNoArgs
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.logging.Logger

class AppActivityLifecycleCallbacks: Application.ActivityLifecycleCallbacks {

  companion object {
    var startedCount = 0
    var stoppedCount = 0
    fun appInBackground(): Boolean = startedCount <= stoppedCount

    internal var currentActivity: WeakReference<Activity>? = null
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    currentActivity = WeakReference(activity)
  }

  override fun onActivityStarted(activity: Activity) {
    startedCount += 1
    currentActivity = WeakReference(activity)
  }

  override fun onActivityResumed(activity: Activity) {
    currentActivity = WeakReference(activity)
  }

  override fun onActivityPaused(activity: Activity) {
    stoppedCount +=1
    currentActivity = null
  }

  override fun onActivityStopped(activity: Activity) {
    stoppedCount +=1
    if (currentActivity?.get() == activity) { // an activity may get stopped after a new one is resumed
        currentActivity = null
    }
  }

  override fun onActivityDestroyed(activity: Activity) {
    if (currentActivity?.get() == activity) { // an activity may get destroyed after a new one is resumed
      currentActivity = null
    }
  }

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {
  }
}

class PushNotificationsInitProvider: ContentProvider() {
  private val log = Logger.get(this::class)

  override fun onCreate(): Boolean {
    val deviceStateStore = DeviceStateStore(context)

    deviceStateStore.instanceId?.let { instanceId ->
      val pni = PushNotificationsInstance(context, instanceId)
      pni.onApplicationStarted()
    }

    (context.applicationContext as? Application).apply {
      when(this) {
        is Application -> registerActivityLifecycleCallbacks(AppActivityLifecycleCallbacks())
        else -> log.w("Failed to register activity lifecycle callbacks. Notification delivery events might be incorrect.")
      }
    }

    return false
  }

  override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
  override fun insert(uri: Uri?, values: ContentValues?): Uri? = null
  override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int = 0
  override fun getType(uri: Uri?): String? = null
}
