package com.nerdscorner.android.plugin.utils

import com.squareup.okhttp.Call
import com.squareup.okhttp.Callback
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import java.io.IOException

fun OkHttpClient.enqueue(request: Request, success: () -> Unit = {}, fail: (String?) -> Unit = {}): Call {
    val call = newCall(request)
    call?.enqueue(object : Callback {
        override fun onResponse(response: Response?) {
            if (response?.isSuccessful == true) {
                success()
            } else {
                fail(response?.body()?.toString())
            }
        }

        override fun onFailure(request: Request?, exception: IOException?) {
            fail(exception?.message)
        }
    })
    return call
}
