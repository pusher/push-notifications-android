package com.pusher.pushnotifications.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.pusher.pushnotifications.PushNotifications
import com.pusher.sample.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val push = PushNotifications()
//        Log.i("MainActivity", "x = " + PushNotifications().x)
    }
}
