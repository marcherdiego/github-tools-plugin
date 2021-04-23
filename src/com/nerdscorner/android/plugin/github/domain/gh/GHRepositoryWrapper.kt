package com.nerdscorner.android.plugin.github.domain.gh

import com.nerdscorner.android.plugin.utils.getFileContent
import org.kohsuke.github.GHContent
import org.kohsuke.github.GHRepository

import java.io.Serializable
import java.net.URL

class GHRepositoryWrapper(@field:Transient val ghRepository: GHRepository) : Wrapper(), Serializable {
    private val description: String
    val name: String = ghRepository.name

    val url: String
        get() = ghRepository.url.toString()

    val fullUrl: URL
        get() = ghRepository.htmlUrl

    var fullChangelog: String? = null

    val lastChangelogEntry: String?
        get() {
            val fullChangelog = fullChangelog ?: return null
            var match = changelogStartRegex.find(fullChangelog) ?: oldChangelogStartRegex.find(fullChangelog)
            val firstIndex = match?.range?.first() ?: 0
            match = match?.next()
            val lastIndex = match?.range?.first() ?: fullChangelog.length
            return fullChangelog.substring(firstIndex, lastIndex)
        }

    val version: String?
        get() {
            return libraryVersionRegex.find(fullChangelog ?: return null)?.value
        }

    val nextVersion: String?
        get() {
            // Assuming version = X.Y.Z
            val tokens = version?.split(".") ?: return null
            val major = tokens[0]
            val minor = tokens[1].toInt()
            return "$major.${minor + 1}.0"
        }

    var rcPullRequestUrl: String? = null
    var rcCreationErrorMessage: String? = null

    var versionBumpPullRequestUrl: String? = null
    var bumpErrorMessage: String? = null

    val changelogFile: GHContent?
        get() = try {
            ghRepository.getFileContent(CHANGELOG_FILE_PATH)
        } catch (e: Exception) {
            null
        }

    init {
        val repoDescription = ghRepository.description
        this.description = if (repoDescription.isNullOrEmpty()) {
            name
        } else {
            repoDescription
        }
    }

    fun ensureChangelog() {
        fullChangelog = changelogFile?.read()?.getFileContent()
    }

    override fun toString() = name

    override fun compare(other: Wrapper): Int {
        return if (other is GHRepositoryWrapper) {
            other
                    .name
                    .toLowerCase()
                    .compareTo(name.toLowerCase())
        } else {
            0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        if (name != (other as GHRepositoryWrapper).name) {
            return false
        }
        return true
    }

    override fun hashCode() = name.hashCode()

    companion object {
        private val changelogStartRegex = "# \\d+\\.\\d+\\.\\d+".toRegex()
        private val oldChangelogStartRegex = "# v\\d+\\.\\d+\\.\\d+".toRegex()
        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private const val CHANGELOG_FILE_PATH = "CHANGELOG.MD"
    }
}
