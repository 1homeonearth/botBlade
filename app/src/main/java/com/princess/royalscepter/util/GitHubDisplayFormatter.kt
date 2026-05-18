package com.princess.royalscepter.util

import com.princess.royalscepter.data.model.GitHubProjectConfig

object GitHubDisplayFormatter {
    const val NOT_LINKED = "not linked"

    fun repositoryName(github: GitHubProjectConfig?): String {
        val owner = github?.owner?.trim().orEmpty()
        val repo = github?.repo?.trim().orEmpty()
        return if (owner.isNotBlank() && repo.isNotBlank()) "$owner/$repo" else NOT_LINKED
    }
}
