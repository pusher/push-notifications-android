package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.api.DeviceMetadata
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import java.io.Serializable

sealed class ServerSyncJob: Serializable

data class StartJob(val fcmToken: String, val knownPreviousClientIds: List<String>): ServerSyncJob()
data class RefreshTokenJob(val newToken: String): ServerSyncJob()
data class SubscribeJob(val interest: String): ServerSyncJob()
data class UnsubscribeJob(val interest: String): ServerSyncJob()
data class SetSubscriptionsJob(val interests: Set<String>): ServerSyncJob()
data class ApplicationStartJob(val deviceMetadata: DeviceMetadata): ServerSyncJob()
data class SetUserIdJob(val userId: String): ServerSyncJob()
class StopJob: ServerSyncJob()

// the following is required for the Persistent Job Queue to read and write data correctly.
// if you add another ServerSyncJob above, you must also add it below here too!
class ServerSyncJsonAdapters {

    companion object {
        val polymorphicJsonAdapterFactory: PolymorphicJsonAdapterFactory<ServerSyncJob> = PolymorphicJsonAdapterFactory.of(ServerSyncJob::class.java, "ServerSyncJob")
                .withSubtype(StartJob::class.java, "StartJob")
                .withSubtype(RefreshTokenJob::class.java, "RefreshTokenJob")
                .withSubtype(SubscribeJob::class.java, "SubscribeJob")
                .withSubtype(UnsubscribeJob::class.java, "UnsubscribeJob")
                .withSubtype(SetSubscriptionsJob::class.java, "SetSubscriptionsJob")
                .withSubtype(ApplicationStartJob::class.java, "ApplicationStartJob")
                .withSubtype(SetUserIdJob::class.java, "SetUserIdJob")
                .withSubtype(StopJob::class.java, "StopJob")
    }

}
