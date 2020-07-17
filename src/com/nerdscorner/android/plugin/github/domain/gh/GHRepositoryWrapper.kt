package com.nerdscorner.android.plugin.github.domain.gh

import org.kohsuke.github.GHContent
import org.kohsuke.github.GHRepository

import java.io.Serializable
import java.net.URL

class GHRepositoryWrapper(@field:Transient val ghRepository: GHRepository) : Wrapper(), Serializable {
    val description: String
    val name: String = ghRepository.name

    val url: String
        get() = ghRepository.url.toString()

    val fullUrl: URL
        get() = ghRepository.htmlUrl

    var fullChangelog: String? = null

    val lastChangelogEntry: String?
        get() = lastChangelogEntry()

    var alias: String? = null

    val version: String?
        get() = extractCurrentVersion()

    val nextVersion: String?
        get() = extractNextVersion()

    var rcPullRequestUrl: String? = null
    var versionBumpPullRequestUrl: String? = null

    @Transient
    var changelogFile: GHContent? = null

    init {
        val repoDescription = ghRepository.description
        this.description = if (repoDescription.isNullOrEmpty()) {
            name
        } else {
            repoDescription
        }
    }

    fun getRepoChangelog(): GHContent? {
        changelogFile = try {
            ghRepository.getFileContent("changelog.md")
        } catch (e: Exception) {
            try {
                ghRepository.getFileContent("CHANGELOG.md")
            } catch (e: Exception) {
                try {
                    ghRepository.getFileContent("CHANGELOG.MD")
                } catch (e: Exception) {
                    null
                }
            }
        }
        return changelogFile
    }


    private fun lastChangelogEntry(): String? {
        val fullChangelog = fullChangelog ?: return null
        var match = changelogStartRegex.find(fullChangelog)
        val firstIndex = match?.range?.first() ?: 0
        match = match?.next()
        val lastIndex = match?.range?.first() ?: fullChangelog.length
        return fullChangelog.substring(firstIndex, lastIndex)
    }

    fun removeUnusedChangelogBlocks(changelog: String? = fullChangelog): Triple<Boolean, Boolean, String>? {
        var resultChangelog = changelog ?: return null
        val newBlockMatch = newBlockRegex.find(resultChangelog)
        if (newBlockMatch != null) {
            resultChangelog = resultChangelog.removeRange(newBlockMatch.range.first() - 1, newBlockMatch.range.last() - 1)
        }

        val fixedBlockMatch = fixedBlockRegex.find(resultChangelog)
        if (fixedBlockMatch != null) {
            resultChangelog = resultChangelog.removeRange(fixedBlockMatch.range.first() - 1, fixedBlockMatch.range.last() - 1)
        }
        val emptyChangelog = newBlockMatch != null && fixedBlockMatch != null
        val hasChanges = newBlockMatch != null || fixedBlockMatch != null
        return Triple(emptyChangelog, hasChanges, resultChangelog)
    }

    private fun extractCurrentVersion(): String? {
        return libraryVersionRegex
                .find(fullChangelog ?: return null)
                ?.value
    }

    private fun extractNextVersion(): String? {
        // Assuming version = X.Y.Z
        val tokens = version?.split(".") ?: return null
        val major = tokens[0]
        val minor = tokens[1].toInt()
        return "$major.${minor + 1}.0"
    }

    override fun toString(): String {
        return name
    }

    override fun compare(other: Wrapper): Int {
        return if (other is GHRepositoryWrapper) {
            other.name.toLowerCase()
                    .compareTo(name.toLowerCase())
        } else {
            0
        }
    }

    fun getNextVersionChangelog(): String {
        return StringBuilder()
                .append("# $nextVersion")
                .append(System.lineSeparator())
                .append("## New")
                .append(System.lineSeparator())
                .append("## Fixed")
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(fullChangelog)
                .toString()
    }

    companion object {
        private val changelogStartRegex = "# \\d+\\.\\d+\\.\\d+".toRegex()
        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private val newBlockRegex = "## New\n[^-]".toRegex()
        private val fixedBlockRegex = "## Fixed\n[^-]".toRegex()
    }
}
