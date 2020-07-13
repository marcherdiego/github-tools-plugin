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
        librariesLoaderThread = Thread {
            ANDROID_LIBS.forEach {
                val repository = ghOrganization.getRepository(it) ?: return@forEach
                val changelogFile = getRepoChangelog(repository) ?: return@forEach
                val fileInputStream = changelogFile.read()
                val writer = StringWriter()
                IOUtils.copy(fileInputStream, writer, Charset.defaultCharset())
                val repositoryWrapper = GHRepositoryWrapper(repository)
                repositoryWrapper.changelog = getLastChangelogEntry(writer.toString())
                androidLibraries.add(repositoryWrapper)
                bus.post(LibraryFetchedSuccessfullyEvent(repositoryWrapper.name))
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
                    .append("## ${library.name} [$libraryVersion](${library.fullUrl}/releases/tag/v$libraryVersion)")
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
        return result.toString()
    }

    companion object {
        private val ANDROID_LIBS = listOf(
                "details-view-android",
                "android-chat",
                "dcp-android",
                "notifications-android",
                "camera-android",
                "ui-android",
                "android-upload-service",
                "sockets-android",
                "networking-android",
                "configurators-android",
                "testing-sdk-android",
                "commons-android",
                "androidThumborUtils",
                "tal-android-oauth"
        )

        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private val changelogStartRegex = "# \\d+\\.\\d+\\.\\d+".toRegex()
    }

    class LibraryFetchedSuccessfullyEvent(val libraryName: String)
    class LibrariesFetchedSuccessfullyEvent
}
