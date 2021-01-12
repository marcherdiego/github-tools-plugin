package com.nerdscorner.android.plugin.github.managers

import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.OkUrlFactory
import com.squareup.okhttp.logging.HttpLoggingInterceptor
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Level.HEADERS
import org.apache.commons.lang.StringUtils
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.OkHttpConnector

object GitHubManager {
    var github: GitHub? = null
    var ghOrganization: GHOrganization? = null
    var myselfGitHub: GHMyself? = null

    @JvmStatic
    fun setup(oauthKey: String, organization: String?) {
        // Github personal token (https://github.com/settings/tokens)
        val httpClient = OkHttpClient()
        httpClient.interceptors().add(HttpLoggingInterceptor().setLevel(HEADERS))
        github = GitHubBuilder
                .fromEnvironment()
                .withOAuthToken(oauthKey)
                .withConnector(OkHttpConnector(OkUrlFactory(httpClient)))
                .build()
        if (!StringUtils.isEmpty(organization)) {
            ghOrganization = github?.getOrganization(organization)
        }
        myselfGitHub = github?.myself
    }

    @JvmStatic
    fun clear() {
        github = null
        ghOrganization = null
        myselfGitHub = null
    }

    @JvmStatic
    fun getMyLogin() = myselfGitHub?.login

    @JvmStatic
    fun getMyName() = myselfGitHub?.name

    @JvmStatic
    fun hasOrganization() = ghOrganization != null

    @JvmStatic
    fun getOrganizationLogin() =  ghOrganization?.login
}
