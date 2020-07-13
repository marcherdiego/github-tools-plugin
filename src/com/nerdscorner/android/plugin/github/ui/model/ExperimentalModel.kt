package com.nerdscorner.android.plugin.github.ui.model

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.Strings
import com.nerdscorner.android.plugin.utils.cancel
import org.apache.commons.io.IOUtils
import org.greenrobot.eventbus.EventBus
import org.kohsuke.github.GHContent
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import java.io.StringWriter
import java.nio.charset.Charset
import kotlin.Exception

class ExperimentalModel(private val ghOrganization: GHOrganization) {
    lateinit var bus: EventBus

    private val androidLibraries = mutableListOf<GHRepositoryWrapper>()
    private var librariesLoaderThread: Thread? = null

    fun fetchAppsChangelog() {
        androidLibraries.clear()
        librariesLoaderThread.cancel()
        var totalProgress = 0f
        val progressStep = 100f / ANDROID_LIBS.size.toFloat()
        librariesLoaderThread = Thread {
            ANDROID_LIBS.forEach {
                totalProgress += progressStep
                val repository = ghOrganization.getRepository(it.second) ?: return@forEach
                val changelogFile = getRepoChangelog(repository) ?: return@forEach
                val fileInputStream = changelogFile.read()
                val writer = StringWriter()
                IOUtils.copy(fileInputStream, writer, Charset.defaultCharset())
                val repositoryWrapper = GHRepositoryWrapper(repository)
                repositoryWrapper.changelog = getLastChangelogEntry(writer.toString())
                repositoryWrapper.alias = it.first
                androidLibraries.add(repositoryWrapper)
                bus.post(LibraryFetchedSuccessfullyEvent(it.first, totalProgress))
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

    private fun getRepoChangelog(repository: GHRepository): GHContent? {
        return try {
            repository.getFileContent("changelog.md")
        } catch (e: Exception) {
            try {
                repository.getFileContent("CHANGELOG.md")
            } catch (e: Exception) {
                try {
                    repository.getFileContent("CHANGELOG.MD")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    fun getLibrariesChangelog(): String {
        val result = StringBuilder()
        androidLibraries.forEach { library ->
            val changelog = library.changelog
            if (changelog.isNullOrEmpty()) {
                return@forEach
            }
            val versionMatch = libraryVersionRegex.find(changelog)
            val libraryVersion = versionMatch?.value
            result
                    .append("## ${library.alias} [v$libraryVersion](${library.fullUrl}/releases/tag/v$libraryVersion)")
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

    companion object {
        private val ANDROID_LIBS = listOf(
                Pair("Details View", "details-view-android"),
                Pair("Chat", "android-chat"),
                Pair("DCP", "dcp-android"),
                Pair("Notifications", "notifications-android"),
                Pair("Camera", "camera-android"),
                Pair("UI", "ui-android"),
                Pair("Upload Service", "android-upload-service"),
                Pair("Sockets", "sockets-android"),
                Pair("Networking", "networking-android"),
                Pair("Configurator", "configurators-android"),
                Pair("Testing SDK", "testing-sdk-android"),
                Pair("Commons", "commons-android"),
                Pair("Thumbor Utils", "androidThumborUtils"),
                Pair("OAuth", "tal-android-oauth")
        )

        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private val changelogStartRegex = "# \\d+\\.\\d+\\.\\d+".toRegex()
    }

    class LibraryFetchedSuccessfullyEvent(val libraryName: String, val totalProgress: Float)
    class LibrariesFetchedSuccessfullyEvent
}
