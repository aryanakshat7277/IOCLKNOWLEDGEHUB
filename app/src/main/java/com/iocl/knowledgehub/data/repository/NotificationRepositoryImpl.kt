package com.iocl.knowledgehub.data.repository

import com.iocl.knowledgehub.data.local.dao.NotificationDao
import com.iocl.knowledgehub.data.local.entity.NotificationEntity
import com.iocl.knowledgehub.data.remote.firebase.FirestoreSource
import com.iocl.knowledgehub.domain.model.DocumentType
import com.iocl.knowledgehub.domain.model.Notification
import com.iocl.knowledgehub.domain.model.NotificationType
import com.iocl.knowledgehub.domain.model.Resource
import com.iocl.knowledgehub.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestoreSource: FirestoreSource,
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getNotifications(): Flow<Resource<List<Notification>>> = flow {
        emit(Resource.Loading)
        firestoreSource.getNotifications().collect { notifications ->
            notificationDao.insertAll(notifications.map { it.toEntity() })
            emit(Resource.Success(notifications))
        }
    }.catch { e ->
        notificationDao.getAll().collect { entities ->
            val cached = entities.map { it.toDomain() }
            if (cached.isNotEmpty()) emit(Resource.Success(cached))
            else emit(Resource.Error(e.message ?: "Failed to load notifications"))
        }
    }

    override suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            notificationDao.markAsRead(notificationId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark as read")
        }
    }

    override suspend fun markAllAsRead(): Resource<Unit> {
        return try {
            notificationDao.markAllAsRead()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark all as read")
        }
    }

    override suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            notificationDao.deleteById(notificationId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete notification")
        }
    }

    override fun getUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    override suspend fun addNotification(notification: Notification): Resource<Unit> {
        return try {
            firestoreSource.sendNotification(notification)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add notification")
        }
    }

    private fun Notification.toEntity(): NotificationEntity = NotificationEntity(
        id = id,
        title = title,
        message = message,
        type = type.name,
        documentId = documentId,
        documentType = documentType?.name,
        isRead = isRead,
        createdAt = createdAt
    )

    private fun NotificationEntity.toDomain(): Notification = Notification(
        id = id,
        title = title,
        message = message,
        type = try {
            NotificationType.valueOf(type)
        } catch (e: Exception) {
            NotificationType.GENERAL
        },
        documentId = documentId,
        documentType = documentType?.let {
            try {
                DocumentType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        },
        isRead = isRead,
        createdAt = createdAt
    )
}
