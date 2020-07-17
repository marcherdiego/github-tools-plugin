package com.nerdscorner.android.plugin.github.domain.gh

import org.kohsuke.github.GHContent
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRef
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser

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
        get() {
            val fullChangelog = fullChangelog ?: return null
            var match = changelogStartRegex.find(fullChangelog)
            val firstIndex = match?.range?.first() ?: 0
            match = match?.next()
            val lastIndex = match?.range?.first() ?: fullChangelog.length
            return fullChangelog.substring(firstIndex, lastIndex)
        }

    var alias: String? = null

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
    var versionBumpPullRequestUrl: String? = null

    val changelogFile: GHContent?
        get() = try {
            ghRepository.getFileContent("CHANGELOG.md")
        } catch (e: Exception) {
            try {
                ghRepository.getFileContent("CHANGELOG.MD")
            } catch (e: Exception) {
                try {
                    ghRepository.getFileContent("changelog.md")
                } catch (e: Exception) {
                    null
                }
            }
        }

    init {
        val repoDescription = ghRepository.description
        this.description = if (repoDescription.isNullOrEmpty()) {
            name
        } else {
            repoDescription
        }
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

    private fun getNextVersionChangelog(): String {
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

    fun createRelease(reviewersTeam: GHTeam?, externalReviewers: MutableList<GHUser>, changelogHasChanges: Boolean, changelog: String) {
        // Create rc branch
        val rcBranch = createBranchOut(DEVELOP_REF, RC_REF_PREFIX + version)

        if (changelogHasChanges) {
            // If changelog has been trimmed, then create a commit with the new changelog
            changelogFile?.update(changelog, CHANGELOG_CLEANUP_COMMIT_MESSAGE, rcBranch?.ref)
        }

        // Create Pull Request
        val rcPullRequest = ghRepository.createPullRequest(
                "rc-$version -> master",
                rcBranch?.ref,
                MASTER_REF,
                removeUnusedChangelogBlocks(lastChangelogEntry)?.third
        )
        rcPullRequestUrl = rcPullRequest.htmlUrl.toString()
        assignReviewers(rcPullRequest, reviewersTeam, externalReviewers)
    }

    fun createVersionBump(reviewersTeam: GHTeam?, externalReviewers: MutableList<GHUser>): Boolean {
        // Create version bump branch
        val versionBumpBranch = createBranchOut(DEVELOP_REF, VERSION_BUMP_REF_PREFIX + (nextVersion ?: return false))

        // Update changelog file
        changelogFile?.update(getNextVersionChangelog(), CHANGELOG_CLEANUP_COMMIT_MESSAGE, versionBumpBranch?.ref) ?: return false

        // Create Pull Request
        val versionBumpPullRequest = ghRepository.createPullRequest(
                "Version bump $nextVersion -> develop",
                versionBumpBranch?.ref,
                DEVELOP_REF,
                "Library version bump"
        )
        versionBumpPullRequestUrl = versionBumpPullRequest.htmlUrl.toString()
        assignReviewers(versionBumpPullRequest, reviewersTeam, externalReviewers)
        return true
    }

    private fun createBranchOut(from: String, to: String): GHRef? {
        val fromSha = ghRepository.getRef(from).`object`.sha
        return ghRepository.createRef(to, fromSha)
    }

    private fun assignReviewers(pullRequest: GHPullRequest, reviewersTeam: GHTeam?, externalReviewers: MutableList<GHUser>) {
        pullRequest.requestTeamReviewers(listOf(reviewersTeam))
        try {
            pullRequest.requestReviewers(externalReviewers)
        } catch (e: java.lang.Exception) {
            // Can't self assign a PR review
        }
    }

    companion object {
        private val changelogStartRegex = "# \\d+\\.\\d+\\.\\d+".toRegex()
        private val libraryVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
        private val newBlockRegex = "## New\n[^-]".toRegex()
        private val fixedBlockRegex = "## Fixed\n[^-]".toRegex()

        private const val RC_REF_PREFIX = "refs/heads/rc-"
        private const val VERSION_BUMP_REF_PREFIX = "refs/heads/version_bump-"
        private const val DEVELOP_REF = "refs/heads/develop"
        private const val MASTER_REF = "refs/heads/master"
        private const val CHANGELOG_CLEANUP_COMMIT_MESSAGE = "Changelog cleanup"
    }
}
