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
import java.io.StringWriter
import java.nio.charset.Charset

class ExperimentalModel(private val ghOrganization: GHOrganization) {
    lateinit var bus: EventBus

    val allLibraries = mutableListOf<GHRepositoryWrapper>()
    val excludedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibraries = mutableListOf<GHRepositoryWrapper>()
    val includedLibrariesNames = mutableListOf<String>()
    private var librariesLoaderThread: Thread? = null
    private var reposLoaderThread: Thread? = null

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
                val changelogFile = repository.getRepoChangelog() ?: return@forEach
                val fileInputStream = changelogFile.read()
                val writer = StringWriter()
                IOUtils.copy(fileInputStream, writer, Charset.defaultCharset())
                repository.changelog = getLastChangelogEntry(writer.toString())
                bus.post(LibraryFetchedSuccessfullyEvent(repository.name, totalProgress))
            }
            bus.post(LibrariesFetchedSuccessfullyEvent())
        }
        librariesLoaderThread?.start()
    }

    private fun getLastChangelogEntry(fullChangelog: String): String {
        var match = changelogStartRegex.find(fullChangelog)
        val firstIndex = match?.range?.first() ?: 0
        match = match?.next()
        val lastIndex = match?.range?.first() ?: fullChangelog.length
        return fullChangelog.substring(firstIndex, lastIndex)
    }

    fun getLibrariesChangelog(): String {
        val result = StringBuilder()
        includedLibraries.forEach { library ->
            val changelog = library.changelog
            if (changelog.isNullOrEmpty()) {
                return@forEach
            }
            val versionMatch = libraryVersionRegex.find(changelog)
            val libraryVersion = versionMatch?.value
            result
                    .append("## ${library.name} [v$libraryVersion](${library.fullUrl}/releases/tag/v$libraryVersion)")
                    .append(System.lineSeparator())
                    .append(addChangelogIndent(changelog))
        }
        return result.toString()
    }

    private fun addChangelogIndent(trimmedChangelog: String): String {
        val result = StringBuilder()
        trimmedChangelog
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

    fun addLibrary(libraryName: String) {
        val library = findLibrary(libraryName) ?: return
        includedLibrariesNames.add(libraryName)
        includedLibraries.add(library)
        excludedLibraries.remove(library)
        propertiesComponent.setValue(INCLUDED_LIBRARIES_PROPERTY, gson.toJson(includedLibrariesNames))
    }

    fun removeLibrary(libraryName: String) {
        val library = findLibrary(libraryName) ?: return
        excludedLibraries.add(library)
        includedLibraries.remove(library)
        includedLibrariesNames.remove(libraryName)
        propertiesComponent.setValue(INCLUDED_LIBRARIES_PROPERTY, gson.toJson(includedLibrariesNames))
    }

    private fun findLibrary(library: String?) = allLibraries.find {
        it.name == library
    }

    class LibraryFetchedSuccessfullyEvent(val libraryName: String, val totalProgress: Float)
    class LibrariesFetchedSuccessfullyEvent
    class ReposLoadedEvent

    companion object {
        private val gson = Gson()
        private val propertiesComponent = PropertiesComponent.getInstance()
        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private val changelogStartRegex = "# \\d+\\.\\d+\\.\\d+".toRegex()

        private val stringListTypeToken = object : TypeToken<List<String>>() {}.type
        private const val INCLUDED_LIBRARIES_PROPERTY = "included_libraries"
    }
}
