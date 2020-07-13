package com.nerdscorner.android.plugin.github.domain.gh

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

    var changelog: String? = null
    var alias: String? = null

    init {
        val repoDescription = ghRepository.description
        this.description = if (repoDescription.isNullOrEmpty()) {
            name
        } else {
            repoDescription
        }
    }

    override fun toString(): String {
        return name
    }

    override fun compare(other: Wrapper): Int {
        return if (other is GHRepositoryWrapper) {
            other.name.toLowerCase().compareTo(name.toLowerCase())
        } else {
            0
        }
    }
}
