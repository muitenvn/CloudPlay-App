package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudPlayRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    suspend fun getUserProfile(userId: String): UserProfile? {
        val snapshot = db.collection("users").document(userId).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        db.collection("users").document(profile.userId).set(profile).await()
    }

    fun observeBookmarks(userId: String): Flow<List<CloudBookmark>> {
        return db.collection("users").document(userId).collection("bookmarks")
            .snapshots()
            .map { it.toObjects<CloudBookmark>() }
    }

    suspend fun addBookmark(bookmark: CloudBookmark) {
        db.collection("users").document(bookmark.userId)
            .collection("bookmarks").document(bookmark.bookmarkId)
            .set(bookmark).await()
    }

    suspend fun deleteBookmark(userId: String, bookmarkId: String) {
        db.collection("users").document(userId)
            .collection("bookmarks").document(bookmarkId)
            .delete().await()
    }

    fun observeProfiles(userId: String): Flow<List<CloudGameProfile>> {
        return db.collection("users").document(userId).collection("profiles")
            .snapshots()
            .map { it.toObjects<CloudGameProfile>() }
    }

    suspend fun saveGameProfile(profile: CloudGameProfile) {
        db.collection("users").document(profile.userId)
            .collection("profiles").document(profile.profileId)
            .set(profile).await()
    }
}
