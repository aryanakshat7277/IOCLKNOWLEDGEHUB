package com.iocl.knowledgehub.domain.repository

import com.iocl.knowledgehub.domain.model.Notification
import com.iocl.knowledgehub.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(): Flow<Resource<List<Notification>>>
    suspend fun markAsRead(notificationId: String): Resource<Unit>
    suspend fun markAllAsRead(): Resource<Unit>
    suspend fun deleteNotification(notificationId: String): Resource<Unit>
    fun getUnreadCount(): Flow<Int>
    suspend fun addNotification(notification: Notification): Resource<Unit>
}
