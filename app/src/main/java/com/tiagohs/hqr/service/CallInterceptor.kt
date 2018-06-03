package com.tiagohs.hqr.service

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class CallInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain?): Response {
        val request = chain!!.request()

        // try the request
        var response = chain.proceed(request)

        var tryCount = 0
        val maxLimit = 3 //Set your max limit here

        while (!response.isSuccessful && tryCount < maxLimit) {

            Log.d("intercept", "Request failed - $tryCount")

            tryCount++

            // retry the request
            response = chain.proceed(request)
        }

        // otherwise just pass the original response on
        return response
    }
}