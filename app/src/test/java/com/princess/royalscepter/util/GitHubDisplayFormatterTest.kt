package com.princess.royalscepter.util

import com.princess.royalscepter.data.model.GitHubProjectConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubDisplayFormatterTest {
    @Test
    fun repositoryNameFormatsOwnerAndRepository() {
        val github = GitHubProjectConfig(owner = " Princess ", repo = " royal-bot ")

        assertEquals("Princess/royal-bot", GitHubDisplayFormatter.repositoryName(github))
    }

    @Test
    fun repositoryNameReturnsNotLinkedWhenMissingOwnerOrRepo() {
        assertEquals("not linked", GitHubDisplayFormatter.repositoryName(null))
        assertEquals("not linked", GitHubDisplayFormatter.repositoryName(GitHubProjectConfig(owner = "Princess", repo = "")))
        assertEquals("not linked", GitHubDisplayFormatter.repositoryName(GitHubProjectConfig(owner = "", repo = "royal-bot")))
    }
}
