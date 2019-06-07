package com.pusher.pushnotifications.internal

import com.squareup.moshi.JsonAdapter
import com.squareup.tape2.ObjectQueue
import okio.Buffer
import okio.Okio
import java.io.IOException
import java.io.OutputStream

class MoshiConverter<T>(private val jsonAdapter: JsonAdapter<T>) : ObjectQueue.Converter<T> {

    @Throws(IOException::class)
    override fun from(bytes: ByteArray): T {
        return jsonAdapter.fromJson(Buffer().write(bytes))!!
    }

    @Throws(IOException::class)
    override fun toStream(`val`: T, os: OutputStream) {
        Okio.buffer(Okio.sink(os)).use { sink -> jsonAdapter.toJson(sink, `val`) }
    }
}
