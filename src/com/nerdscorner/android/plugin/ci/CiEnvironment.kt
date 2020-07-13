package com.nerdscorner.android.plugin.ci

interface CiEnvironment {
    fun triggerRebuild(externalId: String, success: () -> Unit = {}, fail: (String?) -> Unit = {}): CiEnvironment

    fun cancelRequest()
}
