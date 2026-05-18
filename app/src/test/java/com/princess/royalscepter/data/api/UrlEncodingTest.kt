package com.princess.royalscepter.data.api

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlEncodingTest {
    @Test
    fun pathSegmentPercentEncodesReservedAsciiCharacters() {
        assertEquals("project%20id%2Fbranch%3Fname%23frag%2Bplus", UrlEncoding.pathSegment("project id/branch?name#frag+plus"))
    }

    @Test
    fun pathSegmentPercentEncodesUtf8Bytes() {
        assertEquals("caf%C3%A9-%F0%9F%91%91", UrlEncoding.pathSegment("café-👑"))
    }

    @Test
    fun filePathPreservesSeparatorsButEncodesEachSegment() {
        assertEquals("src/commands/hello%20world%2Btoken.ts", UrlEncoding.filePath("src/commands/hello world+token.ts"))
    }
}
