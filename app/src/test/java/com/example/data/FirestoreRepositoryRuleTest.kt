package com.example.data

import com.example.data.firebase.CloudBookmark
import com.example.data.firebase.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FirestoreRepositoryRuleTest {

    @Test
    fun testUserProfileModelCreation() {
        val profile = UserProfile(
            userId = "user_123",
            displayName = "CloudGamer",
            email = "gamer@test.com",
            preferredRegion = "xcloud-japan",
            directBypassEnabled = true,
            streamQuality = "1080p"
        )
        assertNotNull(profile)
        assertEquals("user_123", profile.userId)
        assertEquals("CloudGamer", profile.displayName)
        assertEquals("xcloud-japan", profile.preferredRegion)
    }

    @Test
    fun testCloudBookmarkModelCreation() {
        val bookmark = CloudBookmark(
            bookmarkId = "bm_123",
            userId = "user_123",
            title = "Forza Horizon 5",
            gameUrl = "https://xbox.com/play",
            region = "xcloud-japan"
        )
        assertNotNull(bookmark)
        assertEquals("bm_123", bookmark.bookmarkId)
        assertEquals("Forza Horizon 5", bookmark.title)
    }
}
