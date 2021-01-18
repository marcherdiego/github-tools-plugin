package com.nerdscorner.android.plugin.github.versions

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper
import com.nerdscorner.android.plugin.utils.getFileContent

class KtsVersionInfo(ghRepositoryWrapper: GHRepositoryWrapper) : VersionInfo(ghRepositoryWrapper) {
    override val versionFilePath = "buildSrc/src/main/kotlin/Versions.kt"

    override fun getUpdatedFile(nextVersion: String): String? {
        return getVersionFileContent()
                ?.read()
                ?.getFileContent()
                ?.replace(
                        "const val libraryVersion = \"${ghRepositoryWrapper.version}\"",
                        "const val libraryVersion = \"$nextVersion\""
                )
    }
}
