package com.nerdscorner.android.plugin.ci

import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.utils.Strings
import com.squareup.okhttp.Call
import com.squareup.okhttp.Callback
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import java.io.IOException

object CircleCi : CiEnvironment {
    private var buildRequest: Call? = null
    private const val BASE_URL = "https://circleci.com/api/v2"
    private const val RERUN_WORKFLOW_URL = "$BASE_URL/workflow/{id}/rerun"
    private const val APPLICATION_JSON = "application/json"
    private const val CIRCLE_TOKEN = "Circle-Token"
    private const val WORKFLOW_ID_KEY = "workflow-id"

    private val client = OkHttpClient()

    private fun extractWorkflowId(externalId: String) = JsonParser()
            .parse(externalId)
            .asJsonObject
            .getAsJsonPrimitive(WORKFLOW_ID_KEY)
            .asString

    override fun triggerRebuild(externalId: String, success: () -> Unit, fail: (String?) -> Unit): CiEnvironment {
        val propertiesComponent = PropertiesComponent.getInstance()
        val circleToken = propertiesComponent.getValue(Strings.CIRCLE_CI_TOKEN_PROPERTY, Strings.BLANK)
        if (circleToken.isEmpty()) {
            fail("Circle CI token not set")
            return this
        }
        val rerunUrl = RERUN_WORKFLOW_URL.replace("{id}", extractWorkflowId(externalId) ?: return this)
        val rerunWorkflowRequest = Request
                .Builder()
                .url(rerunUrl)
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), Strings.BLANK))
                .header(CIRCLE_TOKEN, circleToken)
                .build()
        buildRequest = client.newCall(rerunWorkflowRequest)
        buildRequest?.enqueue(object : Callback {
            override fun onResponse(response: Response?) {
                if (response?.isSuccessful == true) {
                    success()
                } else {
                    fail(response?.body()?.string())
                }
            }

            override fun onFailure(request: Request?, exception: IOException?) {
                fail(exception?.message)
            }
        })
        return this
    }

    override fun cancelRequest() {
        buildRequest?.cancel()
    }
}