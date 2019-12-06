package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.api.DeviceMetadata
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dev.zacsweers.moshisealed.annotations.TypeLabel
import java.io.Serializable

@JsonClass(generateAdapter = true, generator = "sealed:ServerSyncJob")
sealed class ServerSyncJob: Serializable { }

// If you add new fields to any of these data classes, make sure they have
// default values otherwise they can be null when reading the old stored
// JSON from disk.
@TypeLabel("StartJob")
@JsonClass(generateAdapter = true)
data class StartJob(val fcmToken: String, val knownPreviousClientIds: List<String>): ServerSyncJob()

@TypeLabel("RefreshTokenJob")
@JsonClass(generateAdapter = true)
data class RefreshTokenJob(val newToken: String): ServerSyncJob()

@TypeLabel("SubscribeJob")
@JsonClass(generateAdapter = true)
data class SubscribeJob(val interest: String): ServerSyncJob()

@TypeLabel("UnsubscribeJob")
@JsonClass(generateAdapter = true)
data class UnsubscribeJob(val interest: String): ServerSyncJob()

@TypeLabel("SetSubscriptionsJob")
@JsonClass(generateAdapter = true)
data class SetSubscriptionsJob(val interests: Set<String>): ServerSyncJob()

@TypeLabel("ApplicationStartJob")
@JsonClass(generateAdapter = true)
data class ApplicationStartJob(val deviceMetadata: DeviceMetadata): ServerSyncJob()

@TypeLabel("SetUserIdJob")
@JsonClass(generateAdapter = true)
data class SetUserIdJob(val userId: String): ServerSyncJob()

@TypeLabel("StopJob")
@JsonClass(generateAdapter = true)
class StopJob: ServerSyncJob()
