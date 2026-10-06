package com.iocl.knowledgehub.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iocl.knowledgehub.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val MAGAZINES_COLLECTION = "magazines"
        const val NEWSPAPERS_COLLECTION = "newspapers"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val USERS_COLLECTION = "users"
    }

    // ========== MAGAZINES ==========
    fun getMagazines(): Flow<List<Magazine>> = callbackFlow {
        val listener = firestore.collection(MAGAZINES_COLLECTION)
            .orderBy("year", Query.Direction.DESCENDING)
            .orderBy("month", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val magazines = snapshot?.documents?.mapNotNull { doc ->
                    doc.toMagazine()
                } ?: emptyList()
                trySend(magazines)
            }
        awaitClose { listener.remove() }
    }

    fun getMagazinesByYear(year: Int): Flow<List<Magazine>> = callbackFlow {
        val listener = firestore.collection(MAGAZINES_COLLECTION)
            .whereEqualTo("year", year)
            .orderBy("month", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val magazines = snapshot?.documents?.mapNotNull { it.toMagazine() } ?: emptyList()
                trySend(magazines)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMagazineById(id: String): Magazine? {
        return try {
            val doc = firestore.collection(MAGAZINES_COLLECTION).document(id).get().await()
            doc.toMagazine()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMagazine(magazine: Magazine): String {
        val docRef = firestore.collection(MAGAZINES_COLLECTION).document()
        val data = magazine.toMap().toMutableMap()
        data["id"] = docRef.id
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun updateMagazine(magazine: Magazine) {
        firestore.collection(MAGAZINES_COLLECTION)
            .document(magazine.id)
            .update(magazine.toMap())
            .await()
    }

    suspend fun deleteMagazine(id: String) {
        firestore.collection(MAGAZINES_COLLECTION).document(id).delete().await()
    }

    suspend fun incrementMagazineViewCount(id: String) {
        val ref = firestore.collection(MAGAZINES_COLLECTION).document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentViews = snapshot.getLong("viewCount") ?: 0
            transaction.update(ref, "viewCount", currentViews + 1)
        }.await()
    }

    suspend fun incrementMagazineDownloadCount(id: String) {
        val ref = firestore.collection(MAGAZINES_COLLECTION).document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentDownloads = snapshot.getLong("downloadCount") ?: 0
            transaction.update(ref, "downloadCount", currentDownloads + 1)
        }.await()
    }

    // ========== NEWSPAPERS ==========
    fun getNewspapers(): Flow<List<Newspaper>> = callbackFlow {
        val listener = firestore.collection(NEWSPAPERS_COLLECTION)
            .orderBy("publishDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val newspapers = snapshot?.documents?.mapNotNull { it.toNewspaper() } ?: emptyList()
                trySend(newspapers)
            }
        awaitClose { listener.remove() }
    }

    fun getNewspapersByDateRange(startDate: Long, endDate: Long): Flow<List<Newspaper>> = callbackFlow {
        val listener = firestore.collection(NEWSPAPERS_COLLECTION)
            .whereGreaterThanOrEqualTo("publishDate", startDate)
            .whereLessThanOrEqualTo("publishDate", endDate)
            .orderBy("publishDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val newspapers = snapshot?.documents?.mapNotNull { it.toNewspaper() } ?: emptyList()
                trySend(newspapers)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getNewspaperById(id: String): Newspaper? {
        return try {
            val doc = firestore.collection(NEWSPAPERS_COLLECTION).document(id).get().await()
            doc.toNewspaper()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addNewspaper(newspaper: Newspaper): String {
        val docRef = firestore.collection(NEWSPAPERS_COLLECTION).document()
        val data = newspaper.toMap().toMutableMap()
        data["id"] = docRef.id
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun updateNewspaper(newspaper: Newspaper) {
        firestore.collection(NEWSPAPERS_COLLECTION)
            .document(newspaper.id)
            .update(newspaper.toMap())
            .await()
    }

    suspend fun deleteNewspaper(id: String) {
        firestore.collection(NEWSPAPERS_COLLECTION).document(id).delete().await()
    }

    suspend fun incrementNewspaperViewCount(id: String) {
        val ref = firestore.collection(NEWSPAPERS_COLLECTION).document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentViews = snapshot.getLong("viewCount") ?: 0
            transaction.update(ref, "viewCount", currentViews + 1)
        }.await()
    }

    suspend fun incrementNewspaperDownloadCount(id: String) {
        val ref = firestore.collection(NEWSPAPERS_COLLECTION).document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentDownloads = snapshot.getLong("downloadCount") ?: 0
            transaction.update(ref, "downloadCount", currentDownloads + 1)
        }.await()
    }

    // ========== NOTIFICATIONS ==========
    fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val listener = firestore.collection(NOTIFICATIONS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    Notification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = try {
                            NotificationType.valueOf(doc.getString("type") ?: "GENERAL")
                        } catch (e: Exception) {
                            NotificationType.GENERAL
                        },
                        documentId = doc.getString("documentId"),
                        documentType = doc.getString("documentType")?.let {
                            try {
                                DocumentType.valueOf(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                        isRead = doc.getBoolean("isRead") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                } ?: emptyList()
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendNotification(notification: Notification) {
        val docRef = firestore.collection(NOTIFICATIONS_COLLECTION).document()
        val data = mapOf(
            "id" to docRef.id,
            "title" to notification.title,
            "message" to notification.message,
            "type" to notification.type.name,
            "documentId" to notification.documentId,
            "documentType" to notification.documentType?.name,
            "isRead" to false,
            "createdAt" to notification.createdAt
        )
        docRef.set(data).await()
    }

    // ========== ANALYTICS ==========
    suspend fun getAnalytics(): Analytics {
        return try {
            val usersSnapshot = firestore.collection(USERS_COLLECTION).get().await()
            val magazinesSnapshot = firestore.collection(MAGAZINES_COLLECTION).get().await()
            val newspapersSnapshot = firestore.collection(NEWSPAPERS_COLLECTION).get().await()

            val totalViews = magazinesSnapshot.documents.sumOf {
                it.getLong("viewCount")?.toInt() ?: 0
            } + newspapersSnapshot.documents.sumOf {
                it.getLong("viewCount")?.toInt() ?: 0
            }
            val totalDownloads = magazinesSnapshot.documents.sumOf {
                it.getLong("downloadCount")?.toInt() ?: 0
            } + newspapersSnapshot.documents.sumOf {
                it.getLong("downloadCount")?.toInt() ?: 0
            }

            Analytics(
                totalUsers = usersSnapshot.size(),
                activeUsers = usersSnapshot.documents.count { it.getBoolean("isActive") ?: false },
                totalMagazines = magazinesSnapshot.size(),
                totalNewspapers = newspapersSnapshot.size(),
                totalDownloads = totalDownloads,
                totalViews = totalViews
            )
        } catch (e: Exception) {
            Analytics()
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION).get().await()
            snapshot.documents.mapNotNull { doc ->
                User(
                    uid = doc.id,
                    employeeId = doc.getString("employeeId") ?: "",
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    department = doc.getString("department") ?: "",
                    designation = doc.getString("designation") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    role = try {
                        UserRole.valueOf(doc.getString("role") ?: "EMPLOYEE")
                    } catch (e: Exception) {
                        UserRole.EMPLOYEE
                    },
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0,
                    lastLoginAt = doc.getLong("lastLoginAt") ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ========== EXTENSION FUNCTIONS ==========
    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toMagazine(): Magazine? {
        return try {
            Magazine(
                id = id,
                title = getString("title") ?: return null,
                description = getString("description") ?: "",
                coverImageUrl = getString("coverImageUrl") ?: "",
                pdfUrl = getString("pdfUrl") ?: "",
                month = getLong("month")?.toInt() ?: 1,
                year = getLong("year")?.toInt() ?: 2024,
                category = try {
                    MagazineCategory.valueOf(getString("category") ?: "CORPORATE")
                } catch (e: Exception) {
                    MagazineCategory.CORPORATE
                },
                tags = (get("tags") as? List<String>) ?: emptyList(),
                pageCount = getLong("pageCount")?.toInt() ?: 0,
                fileSize = getLong("fileSize") ?: 0,
                viewCount = getLong("viewCount")?.toInt() ?: 0,
                downloadCount = getLong("downloadCount")?.toInt() ?: 0,
                uploadedBy = getString("uploadedBy") ?: "",
                uploadedAt = getLong("uploadedAt") ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toNewspaper(): Newspaper? {
        return try {
            Newspaper(
                id = id,
                title = getString("title") ?: return null,
                headline = getString("headline") ?: "",
                summary = getString("summary") ?: "",
                coverImageUrl = getString("coverImageUrl") ?: "",
                pdfUrl = getString("pdfUrl") ?: "",
                publishDate = getLong("publishDate") ?: 0,
                edition = getString("edition") ?: "",
                category = try {
                    NewspaperCategory.valueOf(getString("category") ?: "DAILY")
                } catch (e: Exception) {
                    NewspaperCategory.DAILY
                },
                tags = (get("tags") as? List<String>) ?: emptyList(),
                pageCount = getLong("pageCount")?.toInt() ?: 0,
                fileSize = getLong("fileSize") ?: 0,
                viewCount = getLong("viewCount")?.toInt() ?: 0,
                downloadCount = getLong("downloadCount")?.toInt() ?: 0,
                uploadedBy = getString("uploadedBy") ?: "",
                uploadedAt = getLong("uploadedAt") ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Magazine.toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "coverImageUrl" to coverImageUrl,
        "pdfUrl" to pdfUrl,
        "month" to month,
        "year" to year,
        "category" to category.name,
        "tags" to tags,
        "pageCount" to pageCount,
        "fileSize" to fileSize,
        "viewCount" to viewCount,
        "downloadCount" to downloadCount,
        "uploadedBy" to uploadedBy,
        "uploadedAt" to uploadedAt
    )

    private fun Newspaper.toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "headline" to headline,
        "summary" to summary,
        "coverImageUrl" to coverImageUrl,
        "pdfUrl" to pdfUrl,
        "publishDate" to publishDate,
        "edition" to edition,
        "category" to category.name,
        "tags" to tags,
        "pageCount" to pageCount,
        "fileSize" to fileSize,
        "viewCount" to viewCount,
        "downloadCount" to downloadCount,
        "uploadedBy" to uploadedBy,
        "uploadedAt" to uploadedAt
    )
}
