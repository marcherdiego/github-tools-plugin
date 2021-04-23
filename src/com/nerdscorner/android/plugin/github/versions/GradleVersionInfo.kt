package com.nerdscorner.android.plugin.github.versions

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.getFileContent

class GradleVersionInfo(ghRepositoryWrapper: GHRepositoryWrapper) : VersionInfo(ghRepositoryWrapper) {
    override val versionFilePath = "build.gradle"

    override fun getUpdatedFile(nextVersion: String): String? {
        return getVersionFileContent()
                ?.read()
                ?.getFileContent()
                ?.replace(
                        "ext.libraryVersion=\"${ghRepositoryWrapper.version}\"",
                        "ext.libraryVersion=\"$nextVersion\""
                )
                ?.replace(
                        "ext.libraryVersion='${ghRepositoryWrapper.version}'",
                        "ext.libraryVersion='$nextVersion'"
                )
    }
}
