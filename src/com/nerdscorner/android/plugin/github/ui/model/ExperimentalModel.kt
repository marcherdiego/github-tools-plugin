package com.nerdscorner.android.plugin.github.ui.model

import com.google.common.util.concurrent.AtomicDouble
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.github.managers.GitHubManager
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.addIfNotPresent
import com.nerdscorner.android.plugin.utils.cancel
import com.nerdscorner.android.plugin.utils.startThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser
import java.util.concurrent.atomic.AtomicInteger

class ExperimentalModel {
    lateinit var bus: EventBus

    val allLibraries = mutableListOf<GHRepositoryWrapper>()
    private val excludedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibraries = mutableListOf<GHRepositoryWrapper>()
    private val includedLibrariesNames = mutableListOf<String>()

    private var librariesLoaderThread: Thread? = null
    private var reposLoaderThread: Thread? = null
    private var releasesCreatorThread: Thread? = null
    private var versionBumpCreatorThread: Thread? = null

    private var loadTotalItems = AtomicInteger()
    private var loadCompletedItems = AtomicInteger()
    private var loadProgress = AtomicDouble()

    init {
        // Restore Included Libraries
        val includedLibrariesJson = propertiesComponent.getValue(INCLUDED_LIBRARIES_PROPERTY, Strings.EMPTY_LIST)
        gson
                .fromJson<List<String>>(includedLibrariesJson, stringListTypeToken)
                .forEach {
                    includedLibrariesNames.add(it)
                }
    }

    private fun shouldAddRepository(repository: GHRepository): Boolean {
        val allowNonOrganizationRepos = propertiesComponent.isTrueValue(Strings.SHOW_REPOS_FROM_OUTSIDE_ORGANIZATION)
        val validSource = allowNonOrganizationRepos || GitHubManager.repoBelongsToAnyOrganization(repository)
        return validSource && repository.isFork.not()
    }

    fun loadRepositories() {
        reposLoaderThread.cancel()
        allLibraries.clear()
        includedLibraries.clear()
        reposLoaderThread = startThread {
            GitHubManager.forEachOrganizationsRepo { repository ->
                if (shouldAddRepository(repository)) {
                    val repo = GHRepositoryWrapper(repository)
                    allLibraries.addIfNotPresent(repo)
                    if (repository.name in includedLibrariesNames) {
                        includedLibraries.add(repo)
                    }
                }
            }
            GitHubManager.myselfGitHub
                    ?.listSubscriptions()
                    ?.withPageSize(BaseReposModel.LARGE_PAGE_SIZE)
                    ?.forEach { repository ->
                        if (shouldAddRepository(repository)) {
                            val repo = GHRepositoryWrapper(repository)
                            allLibraries.addIfNotPresent(repo)
                            if (repository.name in includedLibrariesNames) {
                                includedLibraries.add(repo)
                            }
                        }
                    }
            bus.post(ReposLoadedEvent())
        }
    }

    fun fetchAppsChangelog() {
        librariesLoaderThread.cancel()
        var totalProgress = 0f
        val progressStep = 100f / includedLibraries.size.toFloat()
        librariesLoaderThread = startThread {
            includedLibraries.forEach { repository ->
                totalProgress += progressStep
                repository.ensureChangelog()
                bus.post(ChangelogFetchedSuccessfullyEvent(repository.name, totalProgress))
            }
            bus.post(AllChangelogFetchedSuccessfullyEvent())
        }
    }

    fun getLibrariesChangelog(): String {
        val result = StringBuilder()
        includedLibraries.forEach { library ->
            val (emptyChangelog, _, trimmedChangelog) = library.removeUnusedChangelogBlocks() ?: return@forEach
            if (emptyChangelog) {
                return@forEach
            }
            result
                    .append("## ${library.name} [v${library.version}](${library.fullUrl}/releases/tag/v${library.version})")
                    .append(System.lineSeparator())
                    .append(addChangelogIndent(trimmedChangelog))
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

    fun createLibrariesReleases(reviewersTeamName: String?) {
        releasesCreatorThread.cancel()
        releasesCreatorThread = startThread {
            val androidReviewersTeam = GitHubManager.getReviewersTeam(reviewersTeamName)
            val externalReviewers = externalReviewersUserNames.mapNotNull {
                GitHubManager.github?.getUser(it)
            }

            loadProgress = AtomicDouble()
            loadTotalItems = AtomicInteger(includedLibraries.size)
            loadCompletedItems = AtomicInteger()
            val progressStep = 100.0 / includedLibraries.size.toDouble()
            val deferredReleases = mutableListOf<Deferred<GHRepositoryWrapper?>>()
            includedLibraries.forEach { library ->
                val deferredRelease = GlobalScope.async(Dispatchers.IO) {
                    val result = releaseLibrary(library, androidReviewersTeam, externalReviewers)
                    loadProgress.addAndGet(progressStep)
                    result
                }
                deferredReleases.add(deferredRelease)
            }
            runBlocking {
                val releasedLibraries = mutableListOf<GHRepositoryWrapper>()
                deferredReleases.forEach {
                    // Wait for tasks to be completed
                    val library = it.await()
                    if (library != null) {
                        // It was bumped
                        releasedLibraries.add(library)
                    }
                }
                bus.post(ReleasesCreatedSuccessfullyEvent(releasedLibraries))
            }
        }
    }

    private fun releaseLibrary(library: GHRepositoryWrapper,
                               reviewersTeam: GHTeam?,
                               externalReviewers: List<GHUser>): GHRepositoryWrapper? {
        var result: GHRepositoryWrapper? = null
        with(library) {
            try {
                rcCreationErrorMessage = null
                ensureChangelog()
                val changelogResult = removeUnusedChangelogBlocks(fullChangelog)
                when {
                    changelogResult == null -> {
                        bumpErrorMessage = CHANGELOG_NOT_FOUND
                    }
                    changelogResult.emptyChangelog -> {
                        rcCreationErrorMessage = EMPTY_CHANGELOG_MESSAGE
                        bus.post(ReleaseSkippedSuccessfullyEvent(name, loadProgress.get()))
                    }
                    else -> {
                        createRelease(reviewersTeam, externalReviewers, changelogResult.hasChanges, changelogResult.resultChangelog)
                        result = this
                        bus.post(ReleaseCreatedSuccessfullyEvent(name, loadProgress.get()))
                    }
                }
            } catch (e: Exception) {
                rcCreationErrorMessage = e.message
            }
        }
        return result
    }

    fun createVersionBumps(reviewersTeamName: String?) {
        versionBumpCreatorThread.cancel()
        versionBumpCreatorThread = startThread {
            val androidReviewersTeam = GitHubManager.getReviewersTeam(reviewersTeamName)
            val externalReviewers = externalReviewersUserNames.mapNotNull {
                GitHubManager.github?.getUser(it)
            }
            loadProgress = AtomicDouble()
            loadTotalItems = AtomicInteger(includedLibraries.size)
            loadCompletedItems = AtomicInteger()
            val progressStep = 100.0 / loadTotalItems.toDouble()
            val deferredBumps = mutableListOf<Deferred<GHRepositoryWrapper?>>()
            includedLibraries.forEach { library ->
                val deferredBump = GlobalScope.async(Dispatchers.IO) {
                    val result = bumpLibrary(library, androidReviewersTeam, externalReviewers)
                    loadProgress.addAndGet(progressStep)
                    result
                }
                deferredBumps.add(deferredBump)
            }
            runBlocking {
                val bumpedLibraries = mutableListOf<GHRepositoryWrapper>()
                deferredBumps.forEach {
                    // Wait for tasks to be completed
                    val library = it.await()
                    if (library != null) {
                        // It was bumped
                        bumpedLibraries.add(library)
                    }
                }
                bus.post(VersionBumpsCreatedSuccessfullyEvent(bumpedLibraries))
            }
        }
    }

    private fun bumpLibrary(library: GHRepositoryWrapper,
                            reviewersTeam: GHTeam?,
                            externalReviewers: List<GHUser>): GHRepositoryWrapper? {
        var result: GHRepositoryWrapper? = null
        with(library) {
            try {
                bumpErrorMessage = null
                ensureChangelog()
                val changelogResult = removeUnusedChangelogBlocks()
                when {
                    changelogResult == null -> {
                        bumpErrorMessage = CHANGELOG_NOT_FOUND
                    }
                    changelogResult.emptyChangelog -> {
                        bumpErrorMessage = NO_CHANGES_NEEDED
                        bus.post(VersionBumpSkippedSuccessfullyEvent(name, loadProgress.get()))
                    }
                    else -> {
                        val libraryBumped = createVersionBump(reviewersTeam, externalReviewers)
                        if (libraryBumped) {
                            result = this
                            bus.post(VersionBumpCreatedSuccessfullyEvent(name, loadProgress.get()))
                        }
                    }
                }
            } catch (e: Exception) {
                bumpErrorMessage = e.message
            }
        }
        return result
    }

    fun saveReviewerTeamName(reviewerTeam: String?) {
        propertiesComponent.setValue(REVIEWER_TEAM_NAME_PROPERTY, reviewerTeam)
    }

    fun getReviewerTeamName() = propertiesComponent.getValue(REVIEWER_TEAM_NAME_PROPERTY) ?: DEFAULT_REVIEWER_TEAM_NAME

    class ChangelogFetchedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class AllChangelogFetchedSuccessfullyEvent
    class ReposLoadedEvent

    class ReleaseCreatedSuccessfullyEvent(val libraryName: String?, val totalProgress: Double)
    class ReleaseSkippedSuccessfullyEvent(val libraryName: String?, val totalProgress: Double)
    class ReleasesCreatedSuccessfullyEvent(val releasedLibraries: MutableList<GHRepositoryWrapper>)

    class VersionBumpCreatedSuccessfullyEvent(val libraryName: String?, val totalProgress: Double)
    class VersionBumpSkippedSuccessfullyEvent(val libraryName: String?, val totalProgress: Double)
    class VersionBumpsCreatedSuccessfullyEvent(val bumpedLibraries: MutableList<GHRepositoryWrapper>)

    companion object {
        private val gson = Gson()
        private val propertiesComponent = PropertiesComponent.getInstance()
        private val stringListTypeToken = object : TypeToken<List<String>>() {}.type
        private const val INCLUDED_LIBRARIES_PROPERTY = "included_libraries"
        private const val REVIEWER_TEAM_NAME_PROPERTY = "reviewer_team_name"

        private const val EMPTY_CHANGELOG_MESSAGE = "Empty changelog"
        private const val NO_CHANGES_NEEDED = "No changes needed"
        private const val CHANGELOG_NOT_FOUND = "CHANGELOG.MD not found"

        private const val DEFAULT_REVIEWER_TEAM_NAME = "AndroidReviewers"
        private val externalReviewersUserNames = listOf("rtss00")
    }
}
