package com.nerdscorner.android.plugin.ci

import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.enqueue
import com.squareup.okhttp.Request

object TravisCi : CiEnvironment() {
    private const val BASE_URL = "https://api.travis-ci.com"
    private const val RERUN_JOB_URL = "$BASE_URL/build/{id}/restart"
    private const val API_VERSION_HEADER_KEY = "Travis-API-Version"
    private const val API_VERSION_HEADER_VALUE = "3"
    private const val AUTHORIZATION_HEADER = "Authorization"

    override fun triggerRebuild(externalId: String, success: () -> Unit, fail: (String?) -> Unit): CiEnvironment {
        val propertiesComponent = PropertiesComponent.getInstance()
        val travisToken = propertiesComponent.getValue(Strings.TRAVIS_CI_TOKEN_PROPERTY, Strings.BLANK)
        if (travisToken.isEmpty()) {
            fail("Travis CI token not set")
            return this
        }
        val rerunJobRequest = Request
                .Builder()
                .url(RERUN_JOB_URL.replace("{id}", externalId))
                .post(requestBody)
                .addHeader(AUTHORIZATION_HEADER, "token $travisToken")
                .addHeader(API_VERSION_HEADER_KEY, API_VERSION_HEADER_VALUE)
                .build()
        buildRequest = client.enqueue(rerunJobRequest, success, fail)
        return this
    }
}
