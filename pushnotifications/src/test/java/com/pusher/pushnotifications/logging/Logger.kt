package com.pusher.pushnotifications.logging

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class Logger {

    fun v(msg: String, t: Throwable? = null) {
        println("VERBOSE: $msg")
    }

    fun d(msg: String, t: Throwable? = null) {
        println("DEBUG: $msg")
    }

    fun i(msg: String, t: Throwable? = null) {
        println("INFO: $msg")
    }

    fun w(msg: String, t: Throwable? = null) {
        println("WARN: $msg")
    }

    fun e(msg: String, t: Throwable? = null) {
        println("ERROR: $msg")
    }

    companion object {
        private val loggers = ConcurrentHashMap<KClass<*>, Logger>()

        fun get(cl: KClass<*>): Logger {
            return loggers.getOrPut(cl, {
                return Logger()
            })
        }
    }
}