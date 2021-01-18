package com.nerdscorner.android.plugin.github.managers

import com.nerdscorner.android.plugin.github.domain.ChangelogResult
import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.getFileContent
import org.kohsuke.github.GHContent
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser

object DeploymentManager {
    private const val VERSION_BUMP_REF_PREFIX = "refs/heads/version_bump-"
    private const val VERSION_BUMP_COMMIT_MESSAGE = "Version bump"

    private const val VERSIONS_FILE_PATH = "buildSrc/src/main/kotlin/Versions.kt"

    private const val RC_REF_PREFIX = "refs/heads/rc-"
    private const val DEVELOP_REF = "refs/heads/develop"
    private const val MASTER_REF = "refs/heads/master"

    private const val CHANGELOG_CLEANUP_COMMIT_MESSAGE = "Changelog cleanup"
    private const val CHANGELOG_FILE_PATH = "CHANGELOG.MD"

    private val newBlockRegex = "## New\n[^-]".toRegex()
    private val fixedBlockRegex = "## Fixed\n[^-]".toRegex()

    fun createRelease(
            repositoryWrapper: GHRepositoryWrapper,
            reviewersTeam: GHTeam?,
            externalReviewers: List<GHUser>?,
            changelogHasChanges: Boolean,
            changelog: String) {
        val ghRepository = repositoryWrapper.ghRepository
        // Create rc branch
        val fromSha = ghRepository.getRef(DEVELOP_REF).`object`.sha
        val rcBranch = ghRepository.createRef(RC_REF_PREFIX + repositoryWrapper.version, fromSha)

        if (changelogHasChanges) {
            // If changelog has been trimmed, then create a commit with the new changelog
            repositoryWrapper.changelogFile?.update(changelog, CHANGELOG_CLEANUP_COMMIT_MESSAGE, rcBranch?.ref)
        }

        // Create Pull Request
        val rcPullRequest = ghRepository.createPullRequest(
                "rc-${repositoryWrapper.version} -> master",
                rcBranch?.ref,
                MASTER_REF,
                removeUnusedChangelogBlocks(repositoryWrapper, repositoryWrapper.lastChangelogEntry)?.resultChangelog
        )
        repositoryWrapper.rcPullRequestUrl = rcPullRequest.htmlUrl.toString()
        assignReviewers(rcPullRequest, reviewersTeam, externalReviewers)
    }

    fun createVersionBump(repositoryWrapper: GHRepositoryWrapper, reviewersTeam: GHTeam?, externalReviewers: List<GHUser>?): Boolean {
        // Create version bump branch
        val nextVersion = repositoryWrapper.nextVersion ?: run {
            repositoryWrapper.bumpErrorMessage = "Unable to determine next version"
            return false
        }

        // Update changelog file
        if (repositoryWrapper.changelogFile == null) {
            repositoryWrapper.bumpErrorMessage = "Unable to find ${CHANGELOG_FILE_PATH} file"
        } else {
            val ghRepository = repositoryWrapper.ghRepository
            // Update Versions.kt file
            val versionsFile = getVersionsFile(ghRepository)
            if (versionsFile == null) {
                repositoryWrapper.bumpErrorMessage = "Unable to find ${VERSIONS_FILE_PATH} file"
            } else {
                val updatedVersionsFile = versionsFile.read()?.getFileContent()?.replace(
                        "const val libraryVersion = \"${repositoryWrapper.version}\"",
                        "const val libraryVersion = \"$nextVersion\""
                )
                val developSha = ghRepository.getRef(DEVELOP_REF).`object`.sha
                val newChangelogContent = getNextVersionChangelog(nextVersion, repositoryWrapper.fullChangelog)
                val versionBumpTree = ghRepository
                        .createTree()
                        .baseTree(developSha)
                        .add(CHANGELOG_FILE_PATH, newChangelogContent, false)
                        .add(VERSIONS_FILE_PATH, updatedVersionsFile, false)
                        .create()

                val versionBumpCommit = ghRepository
                        .createCommit()
                        .tree(versionBumpTree.sha)
                        .parent(developSha)
                        .message(VERSION_BUMP_COMMIT_MESSAGE)
                        .create()

                val versionBumpBranch = ghRepository.createRef(VERSION_BUMP_REF_PREFIX + nextVersion, versionBumpCommit.shA1)

                // Create Pull Request
                val versionBumpPullRequest = ghRepository.createPullRequest(
                        "Version bump $nextVersion -> develop",
                        versionBumpBranch?.ref,
                        DEVELOP_REF,
                        "Library version bump"
                )
                repositoryWrapper.versionBumpPullRequestUrl = versionBumpPullRequest.htmlUrl.toString()
                assignReviewers(versionBumpPullRequest, reviewersTeam, externalReviewers)
                return true
            }
        }
        return false
    }

    fun removeUnusedChangelogBlocks(
            repositoryWrapper: GHRepositoryWrapper,
            changelog: String? = repositoryWrapper.lastChangelogEntry): ChangelogResult? {
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
        return ChangelogResult(emptyChangelog, hasChanges, resultChangelog)
    }

    private fun assignReviewers(pullRequest: GHPullRequest, reviewersTeam: GHTeam?, externalReviewers: List<GHUser>?) {
        pullRequest.requestTeamReviewers(listOf(reviewersTeam))
        try {
            pullRequest.requestReviewers(externalReviewers)
        } catch (e: Exception) {
            // Can't self assign a PR review
        }
    }

    private fun getVersionsFile(ghRepository: GHRepository): GHContent? {
        return try {
            ghRepository.getFileContent(VERSIONS_FILE_PATH)
        } catch (e: Exception) {
            null
        }
    }

    private fun getNextVersionChangelog(nextVersion: String, fullChangelog: String?): String {
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
}