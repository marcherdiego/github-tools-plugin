package com.nerdscorner.android.plugin.github.ui.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.apache.commons.io.IOUtils
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub
import java.io.StringWriter
import java.lang.Exception
import java.nio.charset.Charset

class ExperimentalModel(private val ghOrganization: GHOrganization, private val github: GitHub) {
    lateinit var bus: EventBus

    val allLibraries = mutableListOf<GHRepositoryWrapper>()
    val excludedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibrariesNames = mutableListOf<String>()
    private var librariesLoaderThread: Thread? = null
    private var reposLoaderThread: Thread? = null
    private var releasesCreatorThread: Thread? = null

    init {
        // Restore Included Libraries
        val includedLibrariesJson = propertiesComponent.getValue(INCLUDED_LIBRARIES_PROPERTY, Strings.EMPTY_LIST)
        gson
                .fromJson<List<String>>(includedLibrariesJson, stringListTypeToken)
                .forEach {
                    includedLibrariesNames.add(it)
                }
    }

    fun loadRepositories() {
        reposLoaderThread.cancel()
        allLibraries.clear()
        reposLoaderThread = Thread {
            ghOrganization
                    .listRepositories()
                    .withPageSize(BaseReposModel.LARGE_PAGE_SIZE)
                    .forEach { repository ->
                        if (repository.isFork.not() && repository.fullName.startsWith(ghOrganization.login)) {
                            val repo = GHRepositoryWrapper(repository)
                            allLibraries.add(repo)
                            if (repository.name in includedLibrariesNames) {
                                includedLibraries.add(repo)
                            }
                        }
                    }
            bus.post(ReposLoadedEvent())
        }
        reposLoaderThread?.start()
    }

    fun fetchAppsChangelog() {
        librariesLoaderThread.cancel()
        var totalProgress = 0f
        val progressStep = 100f / includedLibraries.size.toFloat()
        librariesLoaderThread = Thread {
            includedLibraries.forEach { repository ->
                totalProgress += progressStep
                ensureChangelog(repository)
                bus.post(LibraryFetchedSuccessfullyEvent(repository.alias, totalProgress))
            }
            bus.post(LibrariesFetchedSuccessfullyEvent())
        }
        librariesLoaderThread?.start()
    }

    private fun ensureChangelog(repository: GHRepositoryWrapper) {
        val changelogFile = repository.getRepoChangelog()
        val fileInputStream = changelogFile?.read() ?: return
        val writer = StringWriter()
        IOUtils.copy(fileInputStream, writer, Charset.defaultCharset())
        repository.fullChangelog = writer.toString()
        val repositoryAlias = libsAlias.getOrDefault(repository.name, repository.name)
        repository.alias = repositoryAlias
    }

    fun getLibrariesChangelog(): String {
        val result = StringBuilder()
        includedLibraries.forEach { library ->
            val changelog = library.lastChangelogEntry
            if (changelog.isNullOrEmpty()) {
                return@forEach
            }
            val libraryVersion = libraryVersionRegex.find(changelog)?.value
            result
                    .append("## ${library.alias} [v$libraryVersion](${library.fullUrl}/releases/tag/v$libraryVersion)")
                    .append(System.lineSeparator())
                    .append(addChangelogIndent(changelog))
        }
        return result.toString()
    }

    private fun addChangelogIndent(changelog: String): String {
        val result = StringBuilder()
        changelog
                .split(System.lineSeparator())
                .drop(1)
                .forEach { line ->
                    if (line.startsWith(Strings.HASH_POUND)) {
                        result
                                .append(Strings.HASH_POUND)
                                .append(line)
                    } else {
                        result.append(line)

                    }
                    result.append(System.lineSeparator())
                }
        return result
                .dropLast(System.lineSeparator().length)
                .toString()
    }

    fun addLibrary(library: GHRepositoryWrapper) {
        includedLibrariesNames.add(library.name)
        includedLibraries.add(library)
        excludedLibraries.remove(library)
        propertiesComponent.setValue(INCLUDED_LIBRARIES_PROPERTY, gson.toJson(includedLibrariesNames))
    }

    fun removeLibrary(library: GHRepositoryWrapper) {
        excludedLibraries.add(library)
        includedLibraries.remove(library)
        includedLibrariesNames.remove(library.name)
        propertiesComponent.setValue(INCLUDED_LIBRARIES_PROPERTY, gson.toJson(includedLibrariesNames))
    }

    fun createLibrariesReleases() {
        releasesCreatorThread.cancel()
        releasesCreatorThread = Thread {
            try {
                val androidReviewersTeam = getReviewersTeam(ANDROID_REVIEWERS_TEAM_NAME)
                val externalReviewers = mutableListOf<GHUser>()
                EXTERNAL_REVIEWERS.forEach { userName ->
                    externalReviewers.add(github.getUser(userName))
                }

                var totalProgress = 0f
                val progressStep = 100f / includedLibraries.size.toFloat()
                includedLibraries.forEach { library ->
                    ensureChangelog(library)
                    val (emptyChangelog, changelogHasChanges, trimmedChangelog) = library.removeUnusedChangelogBlocks() ?: return@forEach
                    if (emptyChangelog) {
                        totalProgress += progressStep
                        return@forEach
                    }
                    bus.post(CreatingReleaseCandidateEvent(library.alias, totalProgress))
                    val libraryVersion = libraryVersionRegex.find(trimmedChangelog)?.value
                    val developSha = library.ghRepository.getRef(DEVELOP_REF).`object`.sha
                    val rcBranchName = RC_REF_PREFIX + libraryVersion
                    // Create rc branch
                    library
                            .ghRepository
                            .createRef(rcBranchName, developSha)
                    if (changelogHasChanges) {
                        // If changelog has been trimmed, then create a commit with the new changelog
                        library
                                .getRepoChangelog()
                                ?.update(trimmedChangelog, CHANGELOG_CLEANUP_COMMIT_MESSAGE, rcBranchName)
                    }
                    // Create Pull Request
                    val rcPullRequest = library
                            .ghRepository
                            .createPullRequest(
                                    "rc-$libraryVersion -> master",
                                    rcBranchName,
                                    MASTER_REF,
                                    library.removeUnusedChangelogBlocks(library.lastChangelogEntry)?.third
                            )

                    // Assign reviewers
                    rcPullRequest.requestTeamReviewers(listOf(androidReviewersTeam))
                    try {
                        rcPullRequest.requestReviewers(externalReviewers)
                    } catch (e: Exception) {
                        // Can't self assign a PR review
                    }
                    totalProgress += progressStep
                    bus.post(ReleaseCreatedSuccessfullyEvent(library.alias, totalProgress))
                }
                bus.post(ReleasesCreatedSuccessfullyEvent())
            } catch (e: Exception) {
                bus.post(ReleasesCreationFailedEvent(e.message))
            }
        }
        releasesCreatorThread?.start()
    }

    private fun getReviewersTeam(teamName: String): GHTeam? {
        return ghOrganization
                .teams
                .entries
                .firstOrNull {
                    it.key == teamName
                }
                ?.value
    }

    class LibraryFetchedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class LibrariesFetchedSuccessfullyEvent
    class ReposLoadedEvent
    class CreatingReleaseCandidateEvent(val libraryName: String?, val totalProgress: Float)
    class ReleaseCreatedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class ReleasesCreatedSuccessfullyEvent
    class ReleasesCreationFailedEvent(val message: String?)

    companion object {
        private val gson = Gson()
        private val propertiesComponent = PropertiesComponent.getInstance()
        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private val libsAlias = HashMap<String, String>().apply {
            put("details-view-android", "Details View")
            put("android-chat", "Chat")
            put("dcp-android", "DCP")
            put("notifications-android", "Notifications")
            put("camera-android", "Camera")
            put("ui-android", "UI")
            put("android-upload-service", "Upload Service")
            put("sockets-android", "Sockets")
            put("networking-android", "Networking")
            put("configurators-android", "Configurator")
            put("testing-sdk-android", "Testing SDK")
            put("commons-android", "Commons")
            put("androidThumborUtils", "Thumbor Utils")
            put("tal-android-oauth", "OAuth")
        }
        private val stringListTypeToken = object : TypeToken<List<String>>() {}.type
        private const val INCLUDED_LIBRARIES_PROPERTY = "included_libraries"

        private const val MASTER_REF = "refs/heads/master"
        private const val DEVELOP_REF = "refs/heads/develop"
        private const val RC_REF_PREFIX = "refs/heads/rc-"
        private const val CHANGELOG_CLEANUP_COMMIT_MESSAGE = "Changelog cleanup"
        private const val ANDROID_REVIEWERS_TEAM_NAME = "AndroidReviewers"
        private val EXTERNAL_REVIEWERS = listOf("rtss00")
    }
}
