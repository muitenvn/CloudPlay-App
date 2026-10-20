package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val preferredRegion: String = "xcloud-japan",
    val directBypassEnabled: Boolean = true,
    val streamQuality: String = "1080p",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBookmark(
    @DocumentId val id: String = "",
    val bookmarkId: String = "",
    val userId: String = "",
    val title: String = "",
    val gameUrl: String = "",
    val imageUrl: String? = null,
    val region: String = "xcloud-japan",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudGameProfile(
    @DocumentId val id: String = "",
    val profileId: String = "",
    val userId: String = "",
    val gameTitle: String = "",
    val targetFps: Long = 60L,
    val bitrateMbps: Long = 25L,
    val vibrationEnabled: Boolean = true,
    val lowLatencyAudio: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
