package com.nerdscorner.android.plugin.ci

import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.enqueue
import com.squareup.okhttp.Request

object CircleCi : CiEnvironment() {
    private const val BASE_URL = "https://circleci.com/api/v2"
    private const val RERUN_WORKFLOW_URL = "$BASE_URL/workflow/{id}/rerun"
    private const val CIRCLE_TOKEN = "Circle-Token"
    private const val WORKFLOW_ID_KEY = "workflow-id"

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
        val rerunWorkflowRequest = Request
                .Builder()
                .url(RERUN_WORKFLOW_URL.replace("{id}", extractWorkflowId(externalId) ?: return this))
                .post(requestBody)
                .header(CIRCLE_TOKEN, circleToken)
                .build()
        buildRequest = client.enqueue(rerunWorkflowRequest, success, fail)
        return this
    }
}
