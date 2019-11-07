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

class InstanceDeviceStateStoreTest {
  private val context: Context = Mockito.mock(Context::class.java)
  private val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
  private lateinit var testInstanceDeviceStateStore: InstanceDeviceStateStore

  @Before
  fun before() {
    `when`(context.getSharedPreferences("com.pusher.pushnotifications.i-123.PushNotificationsInstance", Context.MODE_PRIVATE))
        .thenReturn(sharedPrefs)

    this.testInstanceDeviceStateStore = InstanceDeviceStateStore(context, "i-123")
  }

  @Test
  fun `deviceId is retrieved correctly`() {
    `when`(sharedPrefs.getString("deviceId", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testInstanceDeviceStateStore.deviceId)
    Assert.assertThat(testInstanceDeviceStateStore.deviceId, `is`(equalTo("i-123")))
  }

  @Test
  fun `deviceId is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("deviceId", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testInstanceDeviceStateStore.deviceId = "i-123"
  }

  @Test
  fun `FCMToken is retrieved correctly`() {
    `when`(sharedPrefs.getString("fcmToken", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testInstanceDeviceStateStore.FCMToken)
    Assert.assertThat(testInstanceDeviceStateStore.FCMToken, `is`(equalTo("i-123")))
  }

  @Test
  fun `FCMToken is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("fcmToken", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testInstanceDeviceStateStore.FCMToken = "i-123"
  }

  @Test
  fun `osVersion is retrieved correctly`() {
    `when`(sharedPrefs.getString("osVersion", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testInstanceDeviceStateStore.osVersion)
    Assert.assertThat(testInstanceDeviceStateStore.osVersion, `is`(equalTo("i-123")))
  }

  @Test
  fun `osVersion is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("osVersion", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testInstanceDeviceStateStore.osVersion = "i-123"
  }

  @Test
  fun `sdkVersion is retrieved correctly`() {
    `when`(sharedPrefs.getString("sdkVersion", null))
        .thenReturn(null)
        .thenReturn("i-123")

    Assert.assertNull(testInstanceDeviceStateStore.sdkVersion)
    Assert.assertThat(testInstanceDeviceStateStore.sdkVersion, `is`(equalTo("i-123")))
  }

  @Test
  fun `sdkVersion is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putString("sdkVersion", "i-123")).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testInstanceDeviceStateStore.sdkVersion = "i-123"
  }

  @Test
  fun `interestsSet is retrieved correctly`() {
    `when`(sharedPrefs.getStringSet("interests", mutableSetOf<String>()))
        .thenReturn(mutableSetOf<String>())
        .thenReturn(mutableSetOf("hello"))

    Assert.assertThat(testInstanceDeviceStateStore.interests, `is`(emptySet<String>()))
    Assert.assertThat(testInstanceDeviceStateStore.interests, `is`(equalTo(mutableSetOf("hello"))))
  }

  @Test
  fun `interestsSet is stored correctly`() {
    val mockEditor = Mockito.mock(SharedPreferences.Editor::class.java)
    `when`(sharedPrefs.edit()).thenReturn(mockEditor)
    `when`(mockEditor.putStringSet("interests", mutableSetOf("hello"))).thenReturn(mockEditor)
    doNothing().`when`(mockEditor).apply()

    testInstanceDeviceStateStore.interests = mutableSetOf("hello")
  }
}
