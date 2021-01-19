package com.nerdscorner.android.plugin.utils

import com.nerdscorner.android.plugin.github.managers.GitHubManager
import org.kohsuke.github.GHRepository

fun GHRepository.shouldAddRepository(): Boolean {
    val validSource = GitHubManager.canShowReposFromOutsideOrganization() || GitHubManager.repoBelongsToAnyOrganization(this)
    return validSource && isFork.not()
}
