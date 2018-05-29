package com.tiagohs.hqr.service.extensions

import io.reactivex.Observable
import okhttp3.*
import okio.*
import java.io.IOException

fun Call.asObservable(): Observable<Response> {
    return Observable.unsafeCreate { subscriber ->
        // Since Call is a one-shot type, clone it for each new subscriber.
        val call = clone()

        try {
            val response = call.execute()
            if (!call.isCanceled) {
                subscriber.onNext(response)
                subscriber.onComplete()
            }
        } catch (error: Exception) {
            if (!call.isCanceled) {
                subscriber.onError(error)
            }
        }
    }
}

fun Call.asObservableSuccess(): Observable<Response> {
    return asObservable().doOnNext { response ->
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code()}")
        }
    }
}

fun OkHttpClient.newCallWithProgress(request: Request, listener: ProgressListener): Call {
    val progressClient = newBuilder()
            .cache(null)
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                        .body(ProgressResponseBody(originalResponse.body()!!, listener))
                        .build()
            }
            .build()

    return progressClient.newCall(request)
}

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}

class ProgressResponseBody(private val responseBody: ResponseBody, private val progressListener: ProgressListener) : ResponseBody() {

    private val bufferedSource: BufferedSource by lazy {
        Okio.buffer(source(responseBody.source()))
    }

    override fun contentType(): MediaType {
        return responseBody.contentType()!!
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        return bufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            internal var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1L)
                return bytesRead
            }
        }
    }
}