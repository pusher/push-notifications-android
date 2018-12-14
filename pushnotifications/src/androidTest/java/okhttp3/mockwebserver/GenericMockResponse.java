package okhttp3.mockwebserver;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.WebSocketListener;
import okhttp3.internal.http2.Settings;
import okio.Buffer;

interface GenericMockResponse extends Cloneable {
    String getStatus();

    MockResponse setResponseCode(int code);

    MockResponse setStatus(String status);

    Headers getHeaders();

    MockResponse clearHeaders();

    MockResponse addHeader(String header);

    MockResponse addHeader(String name, Object value);

    MockResponse addHeaderLenient(String name, Object value);

    MockResponse setHeader(String name, Object value);

    MockResponse setHeaders(Headers headers);

    MockResponse removeHeader(String name);

    Buffer getBody();

    MockResponse setBody(Buffer body);

    MockResponse setBody(String body);

    MockResponse setChunkedBody(Buffer body, int maxChunkSize);

    MockResponse setChunkedBody(String body, int maxChunkSize);

    SocketPolicy getSocketPolicy();

    MockResponse setSocketPolicy(SocketPolicy socketPolicy);

    int getHttp2ErrorCode();

    MockResponse setHttp2ErrorCode(int http2ErrorCode);

    MockResponse throttleBody(long bytesPerPeriod, long period, TimeUnit unit);

    long getThrottleBytesPerPeriod();

    long getThrottlePeriod(TimeUnit unit);

    MockResponse setBodyDelay(long delay, TimeUnit unit);

    long getBodyDelay(TimeUnit unit);

    MockResponse setHeadersDelay(long delay, TimeUnit unit);

    long getHeadersDelay(TimeUnit unit);

    MockResponse withPush(PushPromise promise);

    List<PushPromise> getPushPromises();

    MockResponse withSettings(Settings settings);

    Settings getSettings();

    MockResponse withWebSocketUpgrade(WebSocketListener listener);

    WebSocketListener getWebSocketListener();
}
