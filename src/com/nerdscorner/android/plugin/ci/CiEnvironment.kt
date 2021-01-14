package com.nerdscorner.android.plugin.ci

import com.nerdscorner.android.plugin.utils.Strings
import com.squareup.okhttp.Call
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.RequestBody

abstract class CiEnvironment {
    protected var buildRequest: Call? = null
    protected val requestBody: RequestBody = RequestBody.create(MediaType.parse("application/json"), Strings.BLANK)
    protected val client = OkHttpClient()

    abstract fun triggerRebuild(externalId: String, success: () -> Unit = {}, fail: (String?) -> Unit = {}): CiEnvironment

    fun cancelRequest() = buildRequest?.cancel()
}

