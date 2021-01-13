package com.nerdscorner.android.plugin.github.managers

import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.OkUrlFactory
import com.squareup.okhttp.logging.HttpLoggingInterceptor
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Level.HEADERS
import org.apache.commons.lang.StringUtils
import org.kohsuke.github.GHMyself
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.extras.OkHttpConnector

object GitHubManager {
    var github: GitHub? = null
    var ghOrganizations = mutableListOf<GHOrganization>()
    var myselfGitHub: GHMyself? = null

    @JvmStatic
    fun setup(oauthKey: String, organizations: String?) {
        // Github personal token (https://github.com/settings/tokens)
        val httpClient = OkHttpClient()
        httpClient.interceptors().add(HttpLoggingInterceptor().setLevel(HEADERS))
        github = GitHubBuilder
                .fromEnvironment()
                .withOAuthToken(oauthKey)
                .withConnector(OkHttpConnector(OkUrlFactory(httpClient)))
                .build()
        ghOrganizations.clear()
        if (!StringUtils.isEmpty(organizations)) {
            organizations
                    ?.split(",")
                    ?.forEach {
                        ghOrganizations.add(github?.getOrganization(it.trim()) ?: return@forEach)
                    }

        }
        myselfGitHub = github?.myself
    }

    @JvmStatic
    fun clear() {
        github = null
        ghOrganizations.clear()
        myselfGitHub = null
    }

    @JvmStatic
    fun getMyLogin() = myselfGitHub?.login

    @JvmStatic
    fun getMyName() = myselfGitHub?.name

    @JvmStatic
    fun hasOrganizations() = ghOrganizations.isNotEmpty()

    fun repoBelongsToAnyOrganization(repository: GHRepository) = ghOrganizations.any { repository.fullName.startsWith(it.login) }

    fun forEachOrganizationsRepo(block: (repository: GHRepository) -> Unit) {
        ghOrganizations.forEach { ghOrganization ->
            ghOrganization
                    .listRepositories()
                    ?.withPageSize(BaseReposModel.LARGE_PAGE_SIZE)
                    ?.forEach { repository ->
                        block(repository)
                    }
        }
    }

    fun getReviewersTeam(teamName: String?): GHTeam? {
        ghOrganizations.forEach { ghOrganization ->
            ghOrganization
                    .teams
                    ?.entries
                    ?.firstOrNull {
                        it.key == teamName
                    }
                    ?.value
                    ?.let { foundTeam ->
                        return foundTeam
                    }
        }
        return null
    }
}
