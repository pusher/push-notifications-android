package com.example.pushnotificationsintegrationtests

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import fi.iki.elonen.NanoHTTPD
import java.lang.Exception
import java.nio.charset.Charset
import kotlin.reflect.KClass

private val gson = Gson()

abstract class NanoHTTPDRouter(val port: Int): NanoHTTPD(port) {
  val mimeTypeJSON = "application/json"

  private val routes = mutableListOf<(session: NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response?>()

  abstract fun setupRoutes()
  init {
    setupRoutes()
  }

  final override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
    routes.forEach {
      val response = it(session)
      if (response != null) {
        return response
      }
    }

    return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "")
  }

  data class Request(val session: NanoHTTPD.IHTTPSession, val params: Map<String, String>, val body: ByteArray) {
    fun <T: Any> entity(kotlinClass: KClass<T>, f: Request.(e: T) -> NanoHTTPD.Response): NanoHTTPD.Response {
      return try {
        val parsedEntity = gson.fromJson<T>(body.toString(Charset.forName("UTF-8")), kotlinClass.java)
        f(parsedEntity)
      } catch (_: JsonSyntaxException){
        complete(Response.Status.BAD_REQUEST)
      }
    }

    fun complete(status: NanoHTTPD.Response.Status): NanoHTTPD.Response {
      return NanoHTTPD.newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, "")
    }

    fun <T> complete(status: NanoHTTPD.Response.Status, value: T): NanoHTTPD.Response {
      val jsonString = gson.toJson(value)

      return NanoHTTPD.newFixedLengthResponse(status, "application/json", jsonString)
    }
  }

  fun head(pathTemplate: String, f: Request.() -> NanoHTTPD.Response) {
    routes += handle(Method.HEAD, pathTemplate, f)
  }

  fun get(pathTemplate: String, f: Request.() -> NanoHTTPD.Response) {
    routes += handle(Method.GET, pathTemplate, f)
  }

  fun post(pathTemplate: String, f: Request.() -> NanoHTTPD.Response) {
    routes += handle(Method.POST, pathTemplate, f)
  }

  fun put(pathTemplate: String, f: Request.() -> NanoHTTPD.Response) {
    routes += handle(Method.PUT, pathTemplate, f)
  }

  fun delete(pathTemplate: String, f: Request.() -> NanoHTTPD.Response) {
    routes += handle(Method.DELETE, pathTemplate, f)
  }

  fun handle(method: Method, pathTemplate: String, f: Request.() -> NanoHTTPD.Response?): (session: NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response? {
    return { session ->
      if (session.method == method) {
        extractParams(pathTemplate, session.uri)?.let { extractedParms ->
          val contentLength = try {
            Integer.parseInt(session.headers["content-length"])
          } catch (e: Throwable) {
            0
          }
          val buffer = ByteArray(contentLength)
          session.inputStream.read(buffer, 0, contentLength)

          Log.i("FakeErrol", "$method: ${session.uri}")
          f(Request(session, extractedParms, buffer))
        }
      }
      else {
      null
      }
    }
  }

  private fun extractParams(pathTemplate: String, path: String): Map<String, String>? {
    val params = mutableMapOf<String, String>()
    try {
      val pathTemplateParts = pathTemplate.split('{', '}')

      var cursor = 0
      var pathMatchingState = false
      pathTemplateParts.forEach { part ->
        pathMatchingState = !pathMatchingState
        if (pathMatchingState) {
          if (path.substring(cursor, cursor + part.length) != part) {
            return null
          }
          cursor += part.length
        } else {
          val extractedParam = path.substring(cursor).takeWhile { it != '/' }
          params.put(part, extractedParam)
          cursor += extractedParam.length
        }
      }

      // has the `pathTemplate` been fully matched
      if (cursor != path.length) {
        return null
      }

    } catch (_: Exception) {
      return null
    }

    return params
  }
}