package com.pusher.pushnotifications.logging

import java.util.concurrent.ConcurrentHashMap

import android.util.Log
import kotlin.reflect.KClass

class Logger(private val name: String) {
  fun v(msg: String, t: Throwable? = null) {
    if (logLevel <= Log.VERBOSE) {
      if (t != null) {
        Log.v(name, msg, t)
      } else {
        Log.v(name, msg)
      }
    }
  }

  fun d(msg: String, t: Throwable? = null) {
    if (logLevel <= Log.DEBUG) {
      if (t != null) {
        Log.d(name, msg, t)
      } else {
        Log.d(name, msg)
      }
    }
  }

  fun i(msg: String, t: Throwable? = null) {
    if (logLevel <= Log.INFO) {
      if (t != null) {
        Log.i(name, msg, t)
      } else {
        Log.i(name, msg)
      }
    }
  }

  fun w(msg: String, t: Throwable? = null) {
    if (logLevel <= Log.WARN) {
      if (t != null) {
        Log.w(name, msg, t)
      } else {
        Log.w(name, msg)
      }
    }
  }

  fun e(msg: String, t: Throwable? = null) {
    if (logLevel <= Log.ERROR) {
      if (t != null) {
        Log.e(name, msg, t)
      } else {
        Log.e(name, msg)
      }
    }
  }

  companion object {
    private val loggers = ConcurrentHashMap<KClass<*>, Logger>()

    var logLevel = Log.DEBUG

    fun get(cl: KClass<*>): Logger {
      return loggers.getOrPut(cl, {
        return if (cl.java.name.endsWith("\$Companion")) {
          // strip out the `$Companion` part and get the actual class simpleName
          Logger(cl.java.name.dropLast("\$Companion".length).split('.').last())
        } else {
          Logger(cl.java.simpleName)
        }
      })
    }
  }
}
