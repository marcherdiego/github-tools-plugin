package com.nerdscorner.android.plugin.github.ui.model

import com.google.common.util.concurrent.AtomicDouble
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.ide.util.PropertiesComponent
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub
import java.util.concurrent.atomic.AtomicInteger

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
            val (emptyChangelog, _, trimmedChangelog) = library.removeUnusedChangelogBlocks() ?: return@forEach
            if (emptyChangelog) {
                return@forEach
            }
            result
                    .append("## ${library.alias} [v${library.version}](${library.fullUrl}/releases/tag/v${library.version})")
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

    fun createLibrariesReleases() {
        releasesCreatorThread.cancel()
        releasesCreatorThread = Thread {
            val androidReviewersTeam = getReviewersTeam(ANDROID_REVIEWERS_TEAM_NAME)
            val externalReviewers = externalReviewersUserNames.map {
                github.getUser(it)
            }

            loadProgress = AtomicDouble()
            loadTotalItems = AtomicInteger(includedLibraries.size)
            loadCompletedItems = AtomicInteger()
            val progressStep = 100.0 / includedLibraries.size.toDouble()
            val deferredReleases = mutableListOf<Deferred<GHRepositoryWrapper?>>()
            includedLibraries.forEach { library ->
                val deferredRelease = GlobalScope.async(Dispatchers.IO) {
                    loadProgress.addAndGet(progressStep)
                    releaseLibrary(library, androidReviewersTeam, externalReviewers)
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
        releasesCreatorThread?.start()
    }

    private fun releaseLibrary(library: GHRepositoryWrapper,
                               reviewersTeam: GHTeam?,
                               externalReviewers: List<GHUser>): GHRepositoryWrapper? {
        var result: GHRepositoryWrapper? = null
        with(library) {
            try {
                rcCreationErrorMessage = null
                ensureChangelog()
                val changelogResult = removeUnusedChangelogBlocks()
                when {
                    changelogResult == null -> {
                        bumpErrorMessage = CHANGELOG_NOT_FOUND
                    }
                    changelogResult.first -> {
                        rcCreationErrorMessage = EMPTY_CHANGELOG_MESSAGE
                        bus.post(ReleaseSkippedSuccessfullyEvent(alias, loadProgress.get()))
                    }
                    else -> {
                        createRelease(reviewersTeam, externalReviewers, changelogResult.second, changelogResult.third)
                        result = this
                        bus.post(ReleaseCreatedSuccessfullyEvent(alias, loadProgress.get()))
                    }
                }
            } catch (e: Exception) {
                rcCreationErrorMessage = e.message
            }
        }
        return result
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
            val externalReviewers = externalReviewersUserNames.map {
                github.getUser(it)
            }
            loadProgress = AtomicDouble()
            loadTotalItems = AtomicInteger(includedLibraries.size)
            loadCompletedItems = AtomicInteger()
            val progressStep = 100.0 / loadTotalItems.toDouble()
            val deferredBumps = mutableListOf<Deferred<GHRepositoryWrapper?>>()
            includedLibraries.forEach { library ->
                val deferredBump = GlobalScope.async(Dispatchers.IO) {
                    loadProgress.addAndGet(progressStep)
                    bumpLibrary(library, androidReviewersTeam, externalReviewers)
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
        versionBumpCreatorThread?.start()
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
                    changelogResult.first -> {
                        bumpErrorMessage = NO_CHANGES_NEEDED
                        bus.post(VersionBumpSkippedSuccessfullyEvent(alias, loadProgress.get()))
                    }
                    else -> {
                        val libraryBumped = createVersionBump(reviewersTeam, externalReviewers)
                        if (libraryBumped) {
                            result = this
                            bus.post(VersionBumpCreatedSuccessfullyEvent(alias, loadProgress.get()))
                        }
                    }
                }
            } catch (e: Exception) {
                bumpErrorMessage = e.message
            }
        }
        return result
    }

    class ChangelogFetchedSuccessfullyEvent(val libraryName: String?, val totalProgress: Float)
    class ChangelogsFetchedSuccessfullyEvent
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

        private const val EMPTY_CHANGELOG_MESSAGE = "Empty changelog"
        private const val NO_CHANGES_NEEDED = "No changes needed"
        private const val CHANGELOG_NOT_FOUND = "CHANGELOG.MD not found"

        private const val ANDROID_REVIEWERS_TEAM_NAME = "AndroidReviewers"
        private val externalReviewersUserNames = listOf("rtss00")
    }
}
