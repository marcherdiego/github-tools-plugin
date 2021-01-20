package com.nerdscorner.android.plugin.github.versions

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import org.kohsuke.github.GHContent

abstract class VersionInfo(protected val ghRepositoryWrapper: GHRepositoryWrapper) {
    abstract val versionFilePath: String

    protected fun getVersionFileContent(): GHContent? = ghRepositoryWrapper.ghRepository.getFileContent(versionFilePath)

    abstract fun getUpdatedFile(nextVersion: String): String?
}
