package com.nerdscorner.android.plugin.github.ui.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub
import java.lang.Exception

class ExperimentalModel(private val ghOrganization: GHOrganization, private val github: GitHub) {
    lateinit var bus: EventBus

    val allLibraries = mutableListOf<GHRepositoryWrapper>()
    val excludedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibrariesNames = mutableListOf<String>()
    private var librariesLoaderThread: Thread? = null
    private var reposLoaderThread: Thread? = null
    private var releasesCreatorThread: Thread? = null
    private var versionBumpCreatorThread: Thread? = null

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
                repository.ensureChangelog()
                bus.post(ChangelogFetchedSuccessfullyEvent(repository.alias, totalProgress))
            }
            bus.post(ChangelogsFetchedSuccessfullyEvent())
        }
        librariesLoaderThread?.start()
    }

    fun getLibrariesChangelog(): String {
        val result = StringBuilder()
        includedLibraries.forEach { library ->
            val changelog = library.lastChangelogEntry
            if (changelog.isNullOrEmpty()) {
                return@forEach
            }
            result
                    .append("## ${library.alias} [v${library.version}](${library.fullUrl}/releases/tag/v${library.version})")
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
            val androidReviewersTeam = getReviewersTeam(ANDROID_REVIEWERS_TEAM_NAME)
            val externalReviewers = mutableListOf<GHUser>()
            EXTERNAL_REVIEWERS.forEach { userName ->
                externalReviewers.add(github.getUser(userName))
            }
            val releasedLibraries = mutableListOf<GHRepositoryWrapper>()
            var totalProgress = 0f
            val progressStep = 100f / includedLibraries.size.toFloat()
            includedLibraries.forEach { library ->
                try {
                    library.rcCreationErrorMessage = null
                    library.ensureChangelog()
                    val (emptyChangelog, changelogHasChanges, trimmedChangelog) = library.removeUnusedChangelogBlocks() ?: return@forEach
                    if (emptyChangelog) {
                        library.rcCreationErrorMessage = EMPTY_CHANGELOG_MESSAGE
                        totalProgress += progressStep
                        return@forEach
                    }
                    bus.post(CreatingReleaseCandidateEvent(library.alias, totalProgress))
                    library.createRelease(androidReviewersTeam, externalReviewers, changelogHasChanges, trimmedChangelog)
                    totalProgress += progressStep
                    releasedLibraries.add(library)
                    bus.post(ReleaseCreatedSuccessfullyEvent(library.alias, totalProgress))
                } catch (e: Exception) {
                    totalProgress += progressStep
                    library.rcCreationErrorMessage = e.message
                }
            }
            bus.post(ReleasesCreatedSuccessfullyEvent(releasedLibraries))
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

    fun createVersionBumps() {
        versionBumpCreatorThread.cancel()
        versionBumpCreatorThread = Thread {
            val androidReviewersTeam = getReviewersTeam(ANDROID_REVIEWERS_TEAM_NAME)
            val externalReviewers = mutableListOf<GHUser>()
            EXTERNAL_REVIEWERS.forEach { userName ->
                externalReviewers.add(github.getUser(userName))
            }
            var totalProgress = 0f
            val progressStep = 100f / includedLibraries.size.toFloat()
            val bumpedLibraries = mutableListOf<GHRepositoryWrapper>()
            includedLibraries.forEach { library ->
                try {
                    library.bumpErrorMessage = null
                    library.ensureChangelog()
                    bus.post(CreatingVersionBumpEvent(library.alias, totalProgress))
                    val libraryBumped = library.createVersionBump(androidReviewersTeam, externalReviewers)
                    totalProgress += progressStep
                    if (libraryBumped.not()) {
                        return@forEach
                    }
                    bumpedLibraries.add(library)
                    bus.post(VersionBumpCreatedSuccessfullyEvent(library.alias, totalProgress))
                } catch (e: Exception) {
                    totalProgress += progressStep
                    library.bumpErrorMessage = e.message
                }
            }
            bus.post(VersionBumpsCreatedSuccessfullyEvent(bumpedLibraries))
        }
        versionBumpCreatorThread?.start()
    }

    class ChangelogFetchedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class ChangelogsFetchedSuccessfullyEvent
    class ReposLoadedEvent

    class CreatingReleaseCandidateEvent(val libraryName: String?, val totalProgress: Float)
    class ReleaseCreatedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class ReleasesCreatedSuccessfullyEvent(val releasedLibraries: MutableList<GHRepositoryWrapper>)

    class CreatingVersionBumpEvent(val libraryName: String?, val totalProgress: Float)
    class VersionBumpCreatedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class VersionBumpsCreatedSuccessfullyEvent(val bumpedLibraries: MutableList<GHRepositoryWrapper>)

    companion object {
        private val gson = Gson()
        private val propertiesComponent = PropertiesComponent.getInstance()
        private val stringListTypeToken = object : TypeToken<List<String>>() {}.type
        private const val INCLUDED_LIBRARIES_PROPERTY = "included_libraries"

        private const val EMPTY_CHANGELOG_MESSAGE = "Empty changelog"

        private const val ANDROID_REVIEWERS_TEAM_NAME = "AndroidReviewers"
        private val EXTERNAL_REVIEWERS = listOf("rtss00")
    }
}
