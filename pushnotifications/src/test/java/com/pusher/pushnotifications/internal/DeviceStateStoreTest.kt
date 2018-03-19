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

    this.testDeviceStateStore = DeviceStateStore(context)
  }

  @Test
  fun `instanceId is retrieved correctly`() {
    `when`(sharedPrefs.getString("instanceId", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testDeviceStateStore.instanceId)
    Assert.assertThat(testDeviceStateStore.instanceId, `is`(equalTo("i-123")))
  }

  @Test
  fun `instanceId is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("instanceId", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.instanceId = "i-123"
  }

  @Test
  fun `deviceId is retrieved correctly`() {
    `when`(sharedPrefs.getString("deviceId", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testDeviceStateStore.deviceId)
    Assert.assertThat(testDeviceStateStore.deviceId, `is`(equalTo("i-123")))
  }

  @Test
  fun `deviceId is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("deviceId", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.deviceId = "i-123"
  }

  @Test
  fun `FCMToken is retrieved correctly`() {
    `when`(sharedPrefs.getString("fcmToken", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testDeviceStateStore.FCMToken)
    Assert.assertThat(testDeviceStateStore.FCMToken, `is`(equalTo("i-123")))
  }

  @Test
  fun `FCMToken is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("fcmToken", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.FCMToken = "i-123"
  }

  @Test
  fun `osVersion is retrieved correctly`() {
    `when`(sharedPrefs.getString("osVersion", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testDeviceStateStore.osVersion)
    Assert.assertThat(testDeviceStateStore.osVersion, `is`(equalTo("i-123")))
  }

  @Test
  fun `osVersion is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("osVersion", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.osVersion = "i-123"
  }

  @Test
  fun `sdkVersion is retrieved correctly`() {
    `when`(sharedPrefs.getString("sdkVersion", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testDeviceStateStore.sdkVersion)
    Assert.assertThat(testDeviceStateStore.sdkVersion, `is`(equalTo("i-123")))
  }

  @Test
  fun `sdkVersion is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("sdkVersion", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.sdkVersion = "i-123"
  }

  @Test
  fun `interestsSet is retrieved correctly`() {
    `when`(sharedPrefs.getStringSet("interests", mutableSetOf<String>()))
        .thenReturn(mutableSetOf<String>())
        .thenReturn(mutableSetOf("hello"))

    Assert.assertThat(testDeviceStateStore.interestsSet, `is`(emptySet<String>()))
    Assert.assertThat(testDeviceStateStore.interestsSet, `is`(equalTo(mutableSetOf("hello"))))
  }

  @Test
  fun `interestsSet is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putStringSet("interests", mutableSetOf("hello"))).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testDeviceStateStore.interestsSet = mutableSetOf("hello")
  }
}
