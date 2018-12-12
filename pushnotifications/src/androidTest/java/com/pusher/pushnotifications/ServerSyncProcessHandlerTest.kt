package com.pusher.pushnotifications

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.pusher.pushnotifications.api.PushNotificationsAPI
import com.pusher.pushnotifications.internal.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ServerSyncProcessHandlerTest {
  /**
   * Non-start jobs should be skipped if the SDK has not been started yet.
   */
  @Test
  fun skipJobsBeforeStart() {
    val instanceId = "000000-c1de-09b9-a8f6-2a22dbdd062a"
    val mockServer = MockWebServer()
    mockServer.start()

    val api = PushNotificationsAPI(instanceId, mockServer.url("/").toString())
    val deviceStateStore = DeviceStateStore(InstrumentationRegistry.getTargetContext())

    val tempFile = File.createTempFile("persistentJobQueue-", ".queue")
    tempFile.delete() // QueueFile expects a handle to a non-existent file on first run.
    val jobQueue = TapeJobQueue<ServerSyncJob>(tempFile)

    val handler = ServerSyncProcessHandler(api, deviceStateStore, jobQueue)

    (0..2).forEach { index ->
      handler.handleMessage(
          ServerSyncHandler.subscribe("interest-$index")
      )
    }


  }

}