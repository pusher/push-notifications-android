package com.pusher.pushnotifications.internal

import android.content.Context
import android.content.SharedPreferences
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.junit.Before
import org.mockito.Mockito.doNothing

class DeviceStateStoreTest {
  private val context: Context = Mockito.mock(Context::class.java)
  private val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
  private lateinit var testDeviceStateStore: DeviceStateStore

  @Before
  fun before() {
    `when`(context.getSharedPreferences("com.pusher.pushnotifications.PushNotificationsInstance", Context.MODE_PRIVATE))
        .thenReturn(sharedPrefs)
    `when`(context.getSharedPreferences("com.pusher.pushnotifications.PushNotificationsInstances", Context.MODE_PRIVATE))
            .thenReturn(sharedPrefs)

    this.testDeviceStateStore = DeviceStateStore(context)
  }

  @Test
  fun `instanceIds is retrieved correctly`() {
    `when`(sharedPrefs.getStringSet("instanceIds", mutableSetOf<String>()))
        .thenReturn(mutableSetOf<String>())
        .thenReturn(mutableSetOf("i-123"))

    Assert.assertThat(testDeviceStateStore.instanceIds, `is`(emptySet<String>()))
    Assert.assertThat(testDeviceStateStore.instanceIds, `is`(equalTo(mutableSetOf("i-123"))))
  }

  @Test
  fun `instanceIds is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putStringSet("instanceIds", mutableSetOf("i-123"))).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.instanceIds = mutableSetOf("i-123")
  }
}
