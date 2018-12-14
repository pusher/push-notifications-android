package okhttp3.mockwebserver

import okhttp3.Headers
import okhttp3.WebSocketListener
import okhttp3.internal.http2.Settings
import okio.Buffer
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

data class TargetedMockResponse(val mockResponse: MockResponse, val path: String) : Cloneable {
  val status: String = mockResponse.status
  val headers: Headers = mockResponse.headers
  val body: Buffer = mockResponse.body
  val socketPolicy: SocketPolicy = mockResponse.socketPolicy
  val http2ErrorCode: Int = mockResponse.http2ErrorCode
  val throttleBytesPerPeriod: Long = mockResponse.throttleBytesPerPeriod
  val pushPromises: List<PushPromise> = mockResponse.pushPromises
  val settings: Settings = mockResponse.settings
  val webSocketListener: WebSocketListener = mockResponse.webSocketListener
  fun setResponseCode(code: Int): TargetedMockResponse { mockResponse.setResponseCode(code); return this }
  fun setStatus(status: String): TargetedMockResponse { mockResponse.setStatus(status); return this }
  fun clearHeaders(): TargetedMockResponse { mockResponse.clearHeaders(); return this }
  fun addHeader(header: String): TargetedMockResponse { mockResponse.addHeader(header); return this }
  fun addHeader(name: String, value: Any): TargetedMockResponse { mockResponse.addHeader(name); return this }
  fun addHeaderLenient(name: String, value: Any): TargetedMockResponse { mockResponse.addHeaderLenient(name, value); return this }
  fun setHeader(name: String, value: Any): TargetedMockResponse { mockResponse.setHeader(name, value); return this }
  fun setHeaders(headers: Headers): TargetedMockResponse { mockResponse.setHeaders(headers); return this }
  fun removeHeader(name: String): TargetedMockResponse { mockResponse.removeHeader(name); return this }
  fun setBody(body: Buffer): TargetedMockResponse { mockResponse.setBody(body); return this }
  fun setBody(body: String): TargetedMockResponse { mockResponse.setBody(body); return this }
  fun setChunkedBody(body: Buffer, maxChunkSize: Int): TargetedMockResponse { mockResponse.setChunkedBody(body, maxChunkSize); return this }
  fun setChunkedBody(body: String, maxChunkSize: Int): TargetedMockResponse { mockResponse.setChunkedBody(body, maxChunkSize); return this }
  fun setSocketPolicy(socketPolicy: SocketPolicy): TargetedMockResponse { mockResponse.setSocketPolicy(socketPolicy); return this }
  fun setHttp2ErrorCode(http2ErrorCode: Int): TargetedMockResponse { mockResponse.setHttp2ErrorCode(http2ErrorCode); return this }
  fun throttleBody(bytesPerPeriod: Long, period: Long, unit: TimeUnit): TargetedMockResponse { mockResponse.throttleBody(bytesPerPeriod, period, unit); return this }
  fun getThrottlePeriod(unit: TimeUnit): Long = mockResponse.getThrottlePeriod(unit)
  fun setBodyDelay(delay: Long, unit: TimeUnit): TargetedMockResponse { mockResponse.setBodyDelay(delay, unit); return this }
  fun getBodyDelay(unit: TimeUnit): Long = mockResponse.getBodyDelay(unit)
  fun setHeadersDelay(delay: Long, unit: TimeUnit): TargetedMockResponse { mockResponse.setHeadersDelay(delay, unit); return this }
  fun getHeadersDelay(unit: TimeUnit): Long = mockResponse.getHeadersDelay(unit)
  fun withPush(promise: PushPromise): TargetedMockResponse { mockResponse.withPush(promise); return this }
  fun withSettings(settings: Settings): TargetedMockResponse { mockResponse.withSettings(settings); return this }
  fun withWebSocketUpgrade(listener: WebSocketListener): TargetedMockResponse { mockResponse.withWebSocketUpgrade(listener); return this }
}

fun MockWebServer.enqueue(targetedMockResponse: TargetedMockResponse) {
  this.protocols()
  this.setDispatcher(object: Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      if (request.path == targetedMockResponse.path) {
        return targetedMockResponse.mockResponse
      }



      throw RuntimeException("Unexpected request")
    }
  })
}

fun MockResponse.forPath(path: String): TargetedMockResponse {
  return TargetedMockResponse(this, path)
}
