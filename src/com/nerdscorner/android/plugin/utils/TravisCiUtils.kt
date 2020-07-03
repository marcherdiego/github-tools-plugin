package com.nerdscorner.android.plugin.utils

import com.intellij.ide.util.PropertiesComponent
import com.squareup.okhttp.Callback
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import java.io.IOException

object TravisCiUtils {
    private const val BASE_URL = "https://api.travis-ci.com"
    private const val RERUN_JOB_URL = "$BASE_URL/build/{id}/restart"
    private const val APPLICATION_JSON = "application/json"
    private const val API_VERSION_HEADER_KEY = "Travis-API-Version"
    private const val API_VERSION_HEADER_VALUE = "3"
    private const val AUTHORIZATION_HEADER = "Authorization"

    private val client = OkHttpClient()

    fun reRunJob(externalId: String, success: () -> Unit = {}, fail: (String?) -> Unit = {}) {
        val propertiesComponent = PropertiesComponent.getInstance()
        val travisToken = propertiesComponent.getValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, Strings.BLANK)
        if (travisToken.isEmpty()) {
            fail("Travis CI token not set")
        }
        val rerunUrl = RERUN_JOB_URL.replace("{id}", externalId)
        val rerunJobRequest = Request
                .Builder()
                .url(rerunUrl)
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), Strings.BLANK))
                .addHeader(AUTHORIZATION_HEADER, "token $travisToken")
                .addHeader(API_VERSION_HEADER_KEY, API_VERSION_HEADER_VALUE)
                .build()
        client
                .newCall(rerunJobRequest)
                .enqueue(object : Callback {
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
    }
}