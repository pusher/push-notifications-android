package com.pusher.pushnotifications.internal

import com.pusher.pushnotifications.api.PushNotificationsAPI
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ServerSyncHandlerTest {
  @Before
  fun before() {
    val mockServer = MockWebServer()
    val instanceId = "00000000-53A8-48D8-9F69-3B585FA878E3"
    val api = PushNotificationsAPI(
        instanceId,
        mockServer.url("/").toString()
    )
  }
}