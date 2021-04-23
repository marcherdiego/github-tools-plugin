package com.nerdscorner.android.plugin.github.managers

import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.ui.model.BaseReposModel
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.shouldAddRepository
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

    private val propertiesComponent = PropertiesComponent.getInstance()

    var github: GitHub? = null
    private var ghOrganizations = mutableListOf<GHOrganization>()
    var myselfGitHub: GHMyself? = null

    @JvmStatic
    fun setup(oauthKey: String, organizations: String?) {
        initGithub(oauthKey)
        initOrganizations(organizations)
    }

    fun initGithub(oauthKey: String?) {
        // Github personal token (https://github.com/settings/tokens)
        val httpClient = OkHttpClient().apply {
            interceptors().add(HttpLoggingInterceptor().setLevel(HEADERS))
        }
        github = GitHubBuilder
                .fromEnvironment()
                .withOAuthToken(oauthKey ?: return)
                .withConnector(OkHttpConnector(OkUrlFactory(httpClient)))
                .build()
        myselfGitHub = github?.myself
    }

    fun initOrganizations(organizations: String?) {
        ghOrganizations.clear()
        if (!StringUtils.isEmpty(organizations)) {
            organizations
                    ?.split(",")
                    ?.forEach {
                        ghOrganizations.add(github?.getOrganization(it.trim()) ?: return@forEach)
                    }

        }
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

    fun canShowReposFromOutsideOrganization(): Boolean {
        // Either che checkbox is unchecked or the user didn't set any organization
        return propertiesComponent.isTrueValue(Strings.SHOW_REPOS_FROM_ORGANIZATION_ONLY).not() || ghOrganizations.isEmpty()
    }

    fun repoBelongsToAnyOrganization(repository: GHRepository) = ghOrganizations.any { repository.fullName.startsWith(it.login) }

    fun forEachOrganizationsRepo(block: (repository: GHRepository) -> Unit) {
        ghOrganizations.forEach { ghOrganization ->
            ghOrganization
                    .listRepositories()
                    .withPageSize(BaseReposModel.LARGE_PAGE_SIZE)
                    .filter {
                        it.shouldAddRepository()
                    }
                    .forEach {
                        block(it)
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
