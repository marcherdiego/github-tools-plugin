package com.nerdscorner.android.plugin.github.events

import com.nerdscorner.android.plugin.github.domain.gh.GHRepositoryWrapper

class FavoriteRepositoryUpdatedEvent(val added: Boolean, val repository: GHRepositoryWrapper)
