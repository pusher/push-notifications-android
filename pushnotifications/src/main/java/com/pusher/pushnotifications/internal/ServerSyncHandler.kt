package com.pusher.pushnotifications.internal

import android.os.*
import com.pusher.pushnotifications.*
import com.pusher.pushnotifications.api.*
import com.pusher.pushnotifications.auth.TokenProvider
import com.pusher.pushnotifications.logging.Logger
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshisealed.reflect.MoshiSealedJsonAdapterFactory
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.*

class ServerSyncHandler private constructor(
    private val api: PushNotificationsAPI,
    private val deviceStateStore: InstanceDeviceStateStore,
    private val jobQueue: PersistentJobQueue<ServerSyncJob>,
    private val handleServerSyncEvent: (ServerSyncEvent) -> Unit,
    private val getTokenProvider: () -> TokenProvider?,
    looper: Looper
): Handler(looper) {
  private val serverSyncProcessHandler = {
    val handlerThread = HandlerThread(looper.thread.name + "-inner-worker")
    handlerThread.start()

    ServerSyncProcessHandler(
        api = api,
        deviceStateStore = deviceStateStore,
        jobQueue = jobQueue,
        handleServerSyncEvent = handleServerSyncEvent,
        getTokenProvider = getTokenProvider,
        looper = handlerThread.looper
    )
  }()

  init {
    // when the app first launches, we should queue up all of the outstanding
    // jobs in the queue so we can pick up where we have left off
    jobQueue.asIterable().forEach { job ->
      when (job) {
        is SetUserIdJob -> {
          // skipping it. If the user is still supposed to logged in, then
          // there should be another setUserIdJob being enqueued upon launch
        }
        else -> {
          serverSyncProcessHandler.sendMessage(Message().also { it.obj = job })
        }
      }
    }
  }

  override fun handleMessage(msg: Message) {
    super.handleMessage(msg)
    val job = msg.obj as ServerSyncJob
    jobQueue.push(job)

    serverSyncProcessHandler.sendMessage(Message.obtain(msg))
  }

  companion object {
    private val serverSyncHandlers = mutableMapOf<String, ServerSyncHandler>()

    internal fun obtain(
        instanceId: String,
        api: PushNotificationsAPI,
        deviceStateStore: InstanceDeviceStateStore,
        secureFileDir: File,
        handleServerSyncEvent: (ServerSyncEvent) -> Unit,
        getTokenProvider: () -> TokenProvider?
        ): ServerSyncHandler {
      return synchronized(serverSyncHandlers) {
        serverSyncHandlers.getOrPut(instanceId) {
          val handlerThread = HandlerThread("ServerSyncHandler-$instanceId")
          handlerThread.start()

          val moshi = Moshi.Builder()
                  .add(MoshiSealedJsonAdapterFactory())
                  .add(KotlinJsonAdapterFactory())
                  .build()
          val converter = MoshiConverter(moshi.adapter(ServerSyncJob::class.java))

          File(secureFileDir, "beams").mkdirs()
          val file = File(secureFileDir, "beams/$instanceId.jobqueue")
          val jobQueue = TapeJobQueue<ServerSyncJob>(file, converter)
          ServerSyncHandler(
              api = api,
              deviceStateStore = deviceStateStore,
              jobQueue = jobQueue,
              handleServerSyncEvent = handleServerSyncEvent,
              getTokenProvider = getTokenProvider,
              looper = handlerThread.looper
          )
        }
      }
    }

    fun refreshToken(fcmToken: String): Message =
        Message.obtain().apply { obj = RefreshTokenJob(fcmToken) }

    fun start(fcmToken: String, knownPreviousClientIds: List<String>): Message =
        Message.obtain().apply { obj = StartJob(fcmToken, knownPreviousClientIds) }

    fun subscribe(interest: String): Message =
        Message.obtain().apply { obj = SubscribeJob(interest) }

    fun unsubscribe(interest: String): Message =
        Message.obtain().apply { obj = UnsubscribeJob(interest) }

    fun setSubscriptions(interests: Set<String>): Message =
        Message.obtain().apply { obj = SetSubscriptionsJob(interests) }

    fun applicationStart(deviceMetadata: DeviceMetadata): Message =
        Message.obtain().apply { obj = ApplicationStartJob(deviceMetadata) }

    fun setUserId(userId: String): Message =
        Message.obtain().apply { obj = SetUserIdJob(userId) }

    fun stop(): Message =
        Message.obtain().apply { obj = StopJob() }
  }
}

class ServerSyncProcessHandler internal constructor(
    private val api: PushNotificationsAPI,
    private val deviceStateStore: InstanceDeviceStateStore,
    private val jobQueue: PersistentJobQueue<ServerSyncJob>,
    private val handleServerSyncEvent: (ServerSyncEvent) -> Unit,
    private val getTokenProvider: () -> TokenProvider?,
    looper: Looper
): Handler(looper) {
  var tokenProviderTimeoutSecs: Long = 60

  private val log = Logger.get(this::class)
  private val started: Boolean
  get() = deviceStateStore.deviceId != null

  private fun recreateDevice(fcmToken: String) {
    // Register device with Errol
    val registrationResponse =
        api.registerFCM(
            token = fcmToken,
            knownPreviousClientIds = emptyList(),
            retryStrategy = RetryStrategy.WithInfiniteExpBackOff())

    var localInterests: Set<String> = emptySet()
    synchronized(deviceStateStore) {
      localInterests = deviceStateStore.interests

      deviceStateStore.FCMToken = fcmToken
      deviceStateStore.serverConfirmedInterestsHash = null
      deviceStateStore.deviceId = registrationResponse.deviceId
    }

    if (localInterests.count() > 0) {
      api.setSubscriptions( // TODO: We don't really handle if we get a 400 or 404
          deviceId = registrationResponse.deviceId,
          interests = localInterests,
          retryStrategy = RetryStrategy.WithInfiniteExpBackOff())
    }

    val storedUserId = deviceStateStore.userId
    if (storedUserId != null) {
      val tp = getTokenProvider()
      if (tp == null) {
        // Any failures during this process are equivalent to de-authing the user e.g. setUserId(null)
        // If the user session is indeed over, there should be a Stop in the backlog eventually
        // If the user session is still valid, there should be a setUserId in the backlog

        log.w("Failed to set the user id due token provider not being present")
        deviceStateStore.userId = null
        return
      }

      try {
        val executor = Executors.newSingleThreadExecutor()
        val future: Future<String> = executor.submit<String> {
          tp.fetchToken(storedUserId)
        }
        val jwt = future.get(tokenProviderTimeoutSecs, TimeUnit.SECONDS)

        api.setUserId(
            deviceStateStore.deviceId!!,
            jwt,
            RetryStrategy.WithInfiniteExpBackOff())
      } catch (e: Exception) {
        // Any failures during this process are equivalent to de-authing the user e.g. setUserId(null)
        // If the user session is indeed over, there should be a Stop in the backlog eventually
        // If the user session is still valid, there should be a setUserId in the backlog

        log.w("Failed to set the user id due to an unexpected error", e)
        deviceStateStore.userId = null
        return
      }
    }
  }

  private fun processStartJob(startJob: StartJob) {
    // Register device with Errol
    val registrationResponse =
        api.registerFCM(
            token = startJob.fcmToken,
            knownPreviousClientIds = startJob.knownPreviousClientIds,
            retryStrategy = RetryStrategy.WithInfiniteExpBackOff())

    val outstandingJobs = mutableListOf<ServerSyncJob>()
    synchronized(deviceStateStore) {
      // Replay sub/unsub/setsub operations in job queue over initial interest set
      val interests = registrationResponse.initialInterests.toMutableSet()
      for (j in jobQueue.asIterable()) {
        if (j is StartJob) {
          break
        }
        when (j) {
          is SubscribeJob -> {
            interests += j.interest
          }
          is UnsubscribeJob -> {
            interests -= j.interest
          }
          is SetSubscriptionsJob -> {
            interests.clear()
            interests.addAll(j.interests)
          }
          is StopJob -> {
            outstandingJobs.clear()
            // Any subscriptions changes done at this point are just discarded,
            // and we need to assume the initial interest set as the starting point again
            interests.clear()
            interests.addAll(registrationResponse.initialInterests)
          }
          is SetUserIdJob -> {
            outstandingJobs.add(j)
          }
          is ApplicationStartJob -> {
            // ignoring it as we are already going to sync the state anyway
          }
          is RefreshTokenJob -> {
            outstandingJobs.add(j)
          }
          else -> {
            throw IllegalStateException("Job $j unexpected during SDK start")
          }
        }
      }

      val localInterestWillChange = deviceStateStore.interests != interests

      // Replace interests with the result
      if (localInterestWillChange) {
        deviceStateStore.interests = interests
        handleServerSyncEvent(InterestsChangedEvent(interests))
      }
    }

    deviceStateStore.deviceId = registrationResponse.deviceId
    deviceStateStore.FCMToken = startJob.fcmToken

    val remoteInterestsWillChange = deviceStateStore.interests != registrationResponse.initialInterests
    if (remoteInterestsWillChange) {
      api.setSubscriptions( // TODO: We don't really handle if we get a 400 or 404
          deviceId = registrationResponse.deviceId,
          interests = deviceStateStore.interests,
          retryStrategy = RetryStrategy.WithInfiniteExpBackOff())
    }

    log.d("Number of outstanding jobs: ${outstandingJobs.size}")
    outstandingJobs.forEach { j ->
      processJob(j)
    }
  }

  private fun processStopJob() {
    api.delete(
        deviceStateStore.deviceId!!,
        RetryStrategy.WithInfiniteExpBackOff())

    deviceStateStore.deviceId = null
    deviceStateStore.userId = null
    deviceStateStore.FCMToken = null
    deviceStateStore.osVersion = null
    deviceStateStore.sdkVersion = null
    deviceStateStore.serverConfirmedInterestsHash = null
  }

  private fun processApplicationStartJob(job: ApplicationStartJob) {
    try {
      val hasMetadataChanged =
          deviceStateStore.sdkVersion != job.deviceMetadata.sdkVersion
              || deviceStateStore.osVersion != job.deviceMetadata.androidVersion

      if (hasMetadataChanged) {
        api.setMetadata(
            deviceStateStore.deviceId!!,
            job.deviceMetadata,
            RetryStrategy.JustDont())

        deviceStateStore.sdkVersion = job.deviceMetadata.sdkVersion
        deviceStateStore.osVersion = job.deviceMetadata.androidVersion
      }

      val interests = deviceStateStore.interests
      val interestsSorted = interests.sorted().joinToString()
      val md5Digest = MessageDigest.getInstance("MD5")
      md5Digest.update(interestsSorted.toByteArray())
      val interestsHash = BigInteger(1, md5Digest.digest()).toString(16)

      if (interestsHash != deviceStateStore.serverConfirmedInterestsHash) {
        api.setSubscriptions(
            deviceStateStore.deviceId!!,
            interests,
            RetryStrategy.JustDont())

          deviceStateStore.serverConfirmedInterestsHash = interestsHash
      }
    } catch (e: PushNotificationsAPIException) {
      // all these operations are best-effort
      log.w("Fail to apply some operations on the application start job, skipping it", e)
    }
  }

  private fun processSetUserIdJob(job: SetUserIdJob) {
    synchronized(deviceStateStore) {
      val storedUserId = deviceStateStore.userId
      if (storedUserId != null) {
        if (storedUserId == job.userId) {
          // cool.

          handleServerSyncEvent(
              UserIdSet(userId = job.userId, pusherCallbackError = null))
          return
        } else {
          throw IllegalStateException("This device has already been registered to another user id.")
        }
      }
    }

    val tp = getTokenProvider()
    if (tp == null) {
      handleServerSyncEvent(
          UserIdSet(
              userId = job.userId,
              pusherCallbackError = PusherCallbackError(
                  message = "Could not set user id: TokenProvider is missing. Have you called start?",
                  cause = null
              ))
      )
      return
    }

    val jwt = try {
      val executor = Executors.newSingleThreadExecutor()
      val future: Future<String> = executor.submit<String> {
        tp.fetchToken(job.userId)
      }
      future.get(tokenProviderTimeoutSecs, TimeUnit.SECONDS)
    } catch (e: ExecutionException) {
      handleServerSyncEvent(
          UserIdSet(
              userId = job.userId,
              pusherCallbackError = PusherCallbackError(
                  message = "Could not set user id: An error has occurred when using the TokenProvider: ${e.cause?.message ?: "Unknown reason"}",
                  cause = e.cause
              ))
      )
      return
    } catch (e: TimeoutException) {
      handleServerSyncEvent(
          UserIdSet(
              userId = job.userId,
              pusherCallbackError = PusherCallbackError(
                  message = "Could not set user id: TokenProvider timed out when reaching customer service (> $tokenProviderTimeoutSecs seconds)",
                  cause = null
              ))
      )
      return
    } catch (e: Exception) {
      handleServerSyncEvent(
          UserIdSet(
              userId = job.userId,
              pusherCallbackError = PusherCallbackError(
                  message = "Could not set user id: An unexpected error occurred when using the TokenProvider. Please contact support@pusher.com",
                  cause = e
              ))
      )
      return
    }

    try {
      api.setUserId(
          deviceStateStore.deviceId!!,
          jwt,
          RetryStrategy.WithInfiniteExpBackOff())

      deviceStateStore.userId = job.userId

      handleServerSyncEvent(
          UserIdSet(userId = job.userId, pusherCallbackError = null))
    } catch (e: PushNotificationsAPIBadJWT) {
      handleServerSyncEvent(
          UserIdSet(
              userId = job.userId,
              pusherCallbackError = PusherCallbackError(
                message = "Could not set user id, jwt rejected: ${e.reason}",
                cause = null // Not forwarding `e` because it is internal
            ))
          )
    } catch (e: PushNotificationsAPIUnprocessableEntity) {
      handleServerSyncEvent(
          UserIdSet(
              userId = job.userId,
              pusherCallbackError = PusherCallbackError(
                message = "Could not set user id: ${e.reason}",
                cause = e
            ))
          )
    } catch (e: PushNotificationsAPIBadRequest) {
      handleServerSyncEvent(
          UserIdSet(
            userId = job.userId,
            pusherCallbackError = PusherCallbackError(
                message = "Something went wrong: ${e.reason}. Please contact support@pusher.com.",
                cause = e
            ))
      )
      throw e
    }
  }

  private fun processJob(job: ServerSyncJob) {
    try {
      when(job) {
        is SubscribeJob -> {
          api.subscribe(
              deviceStateStore.deviceId!!,
              job.interest,
              RetryStrategy.WithInfiniteExpBackOff())
        }
        is UnsubscribeJob -> {
          api.unsubscribe(
              deviceStateStore.deviceId!!,
              job.interest,
              RetryStrategy.WithInfiniteExpBackOff())
        }
        is SetSubscriptionsJob -> {
          api.setSubscriptions(
              deviceStateStore.deviceId!!,
              job.interests,
              RetryStrategy.WithInfiniteExpBackOff())
        }
        is RefreshTokenJob -> {
          if (deviceStateStore.FCMToken != job.newToken) {
            api.refreshToken(
                deviceStateStore.deviceId!!,
                job.newToken,
                RetryStrategy.WithInfiniteExpBackOff())

            deviceStateStore.FCMToken = job.newToken
          }
        }
        is SetUserIdJob -> {
          processSetUserIdJob(job)
        }
        is ApplicationStartJob -> {
          processApplicationStartJob(job)
        }
      }
    } catch (e: PushNotificationsAPIBadRequest) {
      // not really recoverable, so log it here and also monitor 400s closely on our backend
      // (this really shouldn't happen)
      log.e("Fail to make a valid request to the server for job ($job), skipping it", e)
    } catch (e: PushNotificationsAPIDeviceNotFound) {
      // server has forgotten about this device, it needs to be recreated
      recreateDevice(deviceStateStore.FCMToken!!)
      processJob(job)
    }
  }

  override fun handleMessage(msg: Message) {
    val job = msg.obj as ServerSyncJob

    // If the SDK hasn't started yet we can't do anything, so skip
    val shouldSkip = !started && job !is StartJob
    if (shouldSkip) {
      return
    }

    when (job) {
      is StartJob -> {
        processStartJob(job)

        // Clear queue up to the start job
        while (jobQueue.peek() !is StartJob) {
          jobQueue.pop()
        }
        jobQueue.pop() // Also remove start job
      }
      is StopJob -> {
        processStopJob()
        jobQueue.pop()
      }
      else -> {
        processJob(job)
        jobQueue.pop()
      }
    }
  }
}
