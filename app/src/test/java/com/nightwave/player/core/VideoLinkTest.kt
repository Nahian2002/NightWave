package com.nightwave.player.core

import org.junit.Assert.*
import org.junit.Test

class VideoLinkTest {
    private val expected = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    @Test fun standardUrls() {
        val inputs = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234&t=30",
            "https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=5",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "youtu.be/dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/live/dQw4w9WgXcQ?feature=share",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "Hey check this out https://youtu.be/dQw4w9WgXcQ from my feed"
        )
        inputs.forEach { assertEquals(it, expected, VideoLink.normalize(it)) }
    }

    @Test fun rejectPhishingOrBadIds() {
        val invalid = listOf(
            "", "hello music", "https://youtube.com.evil.tld/watch?v=dQw4w9WgXcQ",
            "https://evil.youtube.com/watch?v=dQw4w9WgXcQ",
            "http://youtu.be/short", "https://youtube.com/watch?v=dQw4w9WgXcQextra",
            "file:///etc/passwd", "javascript:alert('bad')", "https://youtu.be/%2Fetc%2Fpasswd",
            "https://youtube.com/playlist?list=PL123", "https://youtube.com/watch?list=PL123",
            "https://youtu.be/dQw4w9WgXcQ.evil.example"
        )
        invalid.forEach { assertNull(it, VideoLink.normalize(it)) }
    }

    @Test fun linkDetection() {
        assertTrue(VideoLink.resemblesLink("https://www.youtube.com/playlist?list=foo"))
        assertTrue(VideoLink.resemblesLink("youtu.be/invalid"))
        assertFalse(VideoLink.resemblesLink("Lo-fi hip hop to study to"))
    }
}
