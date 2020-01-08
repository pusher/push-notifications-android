package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.api.DeviceMetadata
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshisealed.annotations.TypeLabel
import java.io.Serializable

@JsonClass(generateAdapter = false, generator = "sealed:ServerSyncJob")
sealed class ServerSyncJob: Serializable

// If you add new fields to any of these data classes, any older records on peoples devices may not
// be read and ignored as they do not match the current expected class.
// Please see the PersistentJobQueueTests to see the expected behaviour.
@TypeLabel("StartJob")
data class StartJob(val fcmToken: String, val knownPreviousClientIds: List<String>): ServerSyncJob()

@TypeLabel("RefreshTokenJob")
data class RefreshTokenJob(val newToken: String): ServerSyncJob()

@TypeLabel("SubscribeJob")
data class SubscribeJob(val interest: String): ServerSyncJob()

@TypeLabel("UnsubscribeJob")
data class UnsubscribeJob(val interest: String): ServerSyncJob()

@TypeLabel("SetSubscriptionsJob")
data class SetSubscriptionsJob(val interests: Set<String>): ServerSyncJob()

@TypeLabel("ApplicationStartJob")
data class ApplicationStartJob(val deviceMetadata: DeviceMetadata): ServerSyncJob()

@TypeLabel("SetUserIdJob")
data class SetUserIdJob(val userId: String): ServerSyncJob()

@TypeLabel("StopJob")
class StopJob: ServerSyncJob()
