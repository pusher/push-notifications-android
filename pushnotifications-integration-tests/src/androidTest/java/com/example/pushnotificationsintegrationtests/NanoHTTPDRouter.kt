package com.example.pushnotificationsintegrationtests

import fi.iki.elonen.NanoHTTPD
import java.lang.Exception

abstract class NanoHTTPDRouter(val port: Int): NanoHTTPD(port) {
  val mimeTypeJSON = "application/json"
  val notFoundResponse = NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "")

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

    return notFoundResponse
  }

  fun head(pathTemplate: String, f: (session: NanoHTTPD.IHTTPSession, urlParams: Map<String, String>, body: ByteArray) -> NanoHTTPD.Response) {
    routes += handle(Method.HEAD, pathTemplate, f)
  }

  fun get(pathTemplate: String, f: (session: NanoHTTPD.IHTTPSession, urlParams: Map<String, String>, body: ByteArray) -> NanoHTTPD.Response) {
    routes += handle(Method.GET, pathTemplate, f)
  }

  fun post(pathTemplate: String, f: (session: NanoHTTPD.IHTTPSession, urlParams: Map<String, String>, body: ByteArray) -> NanoHTTPD.Response) {
    routes += handle(Method.POST, pathTemplate, f)
  }

  fun put(pathTemplate: String, f: (session: NanoHTTPD.IHTTPSession, urlParams: Map<String, String>, body: ByteArray) -> NanoHTTPD.Response) {
    routes += handle(Method.PUT, pathTemplate, f)
  }

  fun delete(pathTemplate: String, f: (session: NanoHTTPD.IHTTPSession, urlParams: Map<String, String>, body: ByteArray) -> NanoHTTPD.Response) {
    routes += handle(Method.DELETE, pathTemplate, f)
  }

  fun handle(method: Method, pathTemplate: String, f: (session: NanoHTTPD.IHTTPSession, urlParams: Map<String, String>, body: ByteArray) -> NanoHTTPD.Response?): (session: NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response? {
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
          f(session, extractedParms, buffer)
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